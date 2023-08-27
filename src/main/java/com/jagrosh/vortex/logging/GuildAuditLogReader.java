package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Vortex;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.restaction.pagination.PaginationAction;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

// TODO: Check to make sure audit log entries are always in order
// TODO: Vastly simplify this code

/**
 * Determines if a certain {@link AuditLogEntry} should be parsed (ie, making sure that the bot has not previously parsed this log on startup).
 * All requests to bulk request audit logs for a certain guild, or to forward an entry from a {@link net.dv8tion.jda.api.events.guild.GuildAuditLogEntryCreateEvent}
 * should be redirected to {@link AuditLogReader}, which will then call the appropriate {@link GuildAuditLogReader} instance.
 */
@Slf4j
class GuildAuditLogReader {
    private final Vortex vortex;
    private final Consumer<AuditLogEntry> handler;
    private final BlockingQueue<AuditLogEntry> logsToSyncQueue;
    private final Lock logQueueLock;
    private final Lock stateReadLock;
    private final Lock stateWriteLock;
    private volatile boolean queueIsOpen;
    private volatile boolean willBulkRetrieve;
    private volatile long lastParsedId;
    private LinkedList<AuditLogEntry> bulkParseFallbackQueue;


    public GuildAuditLogReader(Vortex vortex, BlockingQueue<AuditLogEntry> logsToSyncQueue, long guildId, Consumer<AuditLogEntry> handler) {
        this.vortex = vortex;
        this.logsToSyncQueue = logsToSyncQueue;
        this.handler = handler;
        this.bulkParseFallbackQueue = new LinkedList<>();

        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
        stateReadLock = readWriteLock.readLock();
        stateWriteLock = readWriteLock.writeLock();
        logQueueLock = new ReentrantLock();

        long localLastParsedId;
        if (Vortex.BULK_PARSE_ON_START) {
            try {
                localLastParsedId = vortex.getDatabase().auditcache.getLastParsed(guildId); // TODO: Handle first time joining guild
                willBulkRetrieve = localLastParsedId != 0L;
            } catch (Exception e) {
                log.warn("Could not get the id of the last audit log parsed for guild " + guildId, e);
                localLastParsedId = 0L;
                willBulkRetrieve = false;
            }
        } else {
            localLastParsedId = 0L;
            willBulkRetrieve = false;
        }

        this.lastParsedId = localLastParsedId;
    }

    /**
     * Marks a lone audit log entry that the bot might want to pass for parsing
     * @param entry The audit log entry
     */
    // TODO: Optimise
    public void parseEntry(AuditLogEntry entry) {
        if (stateReadLock.tryLock()) {
            try {
                if (willBulkRetrieve) {
                    return; // No need to parse since it will be bulk retrieved momentarily
                } else {
                    handleWhenReadlock(entry);
                }
            } finally {
                stateReadLock.unlock();
            }
        } else {
            // Executed when a thread has the state write lock/is bulk proccessing audit logs
            // There is a chance that bulk requesting may miss a newer log, so it should be queued just in case
            logQueueLock.lock();
            try {
                if (queueIsOpen) { // Queue will be open right before the write lock is acquired, and close right after aquiring LOG_QUEUE_MONITOR
                    bulkParseFallbackQueue.add(entry);
                    return; // Added for proccessing
                } else if (lastParsedId >= entry.getIdLong()) {
                    return; // Already handled
                } else {
                    // If we're still here, wait until we can proccess it normally post bulk request style
                    // Note: It's really important this lock is fair for this to work properly as its currently written
                    stateReadLock.lock();
                    try {
                        handleWhenReadlock(entry);
                    } finally {
                        stateReadLock.unlock();
                    }
                }
            } finally {
                logQueueLock.unlock();
            }
        }
    }

    private void handleWhenReadlock(AuditLogEntry entry) {
        // TODO: Determine if lock should be released before handling/if comparing values is unneccessary (aka is it garuenteed this method is never concurrently called)
        if (lastParsedId < entry.getIdLong()) {
            setLastParsedEntryAndSync(entry);
        } else {
            // Still proccess since there is no 100% garuntee that we have not proccessed it yet, maybe
            // TODO: Figure out if the above comment is true or not
        }

        handler.accept(entry);
    }

    /**
     * Indicates that the bot should bulk request logs for a specific guild using an {@link net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction AuditLogPaginationAction}.
     * This should only be called once after the initial {@link net.dv8tion.jda.api.events.session.ReadyEvent}
     * @param g The guild
     */
    // TODO: Make sure no one left the guild while the bot was down to evade persists
    public void bulkRead(Guild g) {
        if (!willBulkRetrieve) {
            return;
        }

        queueIsOpen = true;
        stateWriteLock.lock();
        try {
            // Reads from the modlogs
            if (!g.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS)) {
                return;
            }

            long localLastParsed = lastParsedId;

            // TODO: Optimise by sending logs in bulk instead of one by one
            List<AuditLogEntry> retrievedLogs = g.retrieveAuditLogs()
                    .cache(false)
                    .limit(50) // TODO: Clamp for safety
                    .order(PaginationAction.PaginationOrder.FORWARD) // (8/16/23): There's this weird bug where doing it backwards does not give you the most recent log
                    .skipTo(localLastParsed)
                    .takeWhileAsync(entry -> true)
                    .get();

            if (!retrievedLogs.isEmpty()) {
                AuditLogEntry lastLog = retrievedLogs.get(retrievedLogs.size() - 1);
                setLastParsedEntryAndSync(lastLog);
                retrievedLogs.forEach(handler);
                readFromQueue(lastLog);
            }
        } catch (InsufficientPermissionException t) {
            readFromQueue(null);
        } catch (Throwable t) {
            readFromQueue(null);
            log.error("Could not bulk retrieve audit logs from guild " + g.getId(), t);
        } finally {
            willBulkRetrieve = false;
            stateWriteLock.unlock();
        }
    }

    private void readFromQueue(AuditLogEntry lastBulkParsedEntry) {
        logQueueLock.lock();
        try {
            queueIsOpen = false;
            bulkParseFallbackQueue.removeIf(queueEntry -> queueEntry.getIdLong() <= lastBulkParsedEntry.getIdLong()); // These were already processed
            bulkParseFallbackQueue.forEach(handler);
            setLastParsedEntryAndSync(bulkParseFallbackQueue.isEmpty() ? null : bulkParseFallbackQueue.getLast());
            bulkParseFallbackQueue = null;
        } finally {
            logQueueLock.unlock();
        }
    }

    public void setLastParsedEntryAndSync(AuditLogEntry entry) {
        if (entry != null) {
            lastParsedId = entry.getIdLong();
            try {
                logsToSyncQueue.put(entry);
            } catch (InterruptedException e) {
                log.error("Could not sync last parsed audit log id " + entry.getId() + " for guild " + entry.getGuild().getId(), e);
            }
        }
    }
}

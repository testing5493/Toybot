package com.jagrosh.vortex.utils;

import net.dv8tion.jda.api.entities.Guild;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Warning: DO NOT USE FOR VERIFIED BOTS
 */
@DoNotUseForVerifiedBots
public class GuildResourceProvider<T> {
    private final HashMap<Long, T> resourceMap;
    private final Lock readLock;
    private final Lock writeLock;
    private final Function<Long, T> resourceCreator;

    /**
     * Warning: DO NOT USE FOR VERIFIED BOTS
     * @param resourceCreator The supplier used for creating the guild specific resource
     */
    @DoNotUseForVerifiedBots
    public GuildResourceProvider(Supplier<T> resourceCreator) {
        this(id -> resourceCreator.get());
    }

    /**
     * Warning: DO NOT USE FOR VERIFIED BOTS
     * @param resourceCreator The function used for creating the guild specific resource, with the id of the guild being
     * passed in
     */
    @DoNotUseForVerifiedBots
    public GuildResourceProvider(Function<Long, T> resourceCreator) {
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        resourceMap = new HashMap<>(64);
        this.resourceCreator = resourceCreator;
    }

    @DoNotUseForVerifiedBots
    public GuildResourceProvider(Map<Long, T> map, Function<Long, T> resourceCreator) {
        ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        resourceMap = new HashMap<>();
        resourceMap.putAll(map);
        this.resourceCreator = resourceCreator;
    }

    /**
     * Gets a resource associated with a specific guild
     * Warning: DO NOT USE FOR VERIFIED BOTS
     *
     * @param g The guild
     * @return The resource
     */
    @DoNotUseForVerifiedBots
    public T get(Guild g) {
        return get(g.getIdLong());
    }

    /**
     * Gets a resource associated with a specific guild
     * Warning: DO NOT USE FOR VERIFIED BOTS
     *
     * @param id The id of the guild
     * @return The resource
     */
    @DoNotUseForVerifiedBots
    public T get(Long id) {
        /*
           Specifically why you should not use this for verified bots is that this code is efficient, but not if
           you have a bot that can be added to a guild by anyone, as multiple guild joins at once can potentially cause
           bottlenecks (needs testing).
           Additionally, the guild mapping resources in its current state may not work well with the amount of guilds
           verified bots are usually in.
         */
        T resource;
        readLock.lock();
        try {
            resource = resourceMap.get(id);
        } finally {
            readLock.unlock();
        }

        if (resource != null) {
            return resource;
        } else {
            if (!writeLock.tryLock()) {
                return get(id);
            } else {
                try {
                    resource = resourceMap.get(id);
                    if (resource == null) {
                        resourceMap.put(id, resource = resourceCreator.apply(id));
                    }

                    return resource;
                } finally {
                    writeLock.unlock();
                }
            }
        }
    }
}

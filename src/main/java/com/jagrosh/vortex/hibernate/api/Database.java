package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.automod.Filter;
import com.jagrosh.vortex.hibernate.entities.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.*;
import org.hibernate.cfg.Configuration;

import java.util.function.Consumer;

/**
 * An API for connecting to the H2 SQL Database using the <a href="https://www.hibernate.org">Hibernate ORM</a>
 *
 * For organisation, methods that interact with a certain aspect of the database are in their respective classes.
 * For example, all methods that interact with modlogs are located in {@link Database#modlogs},
 * (ie. {@link Database#modlogs#getCase(long, int)}, {@link Database#modlogs#deleteCase(long, long, int), etc.},
 * while all methods that interact with tags would be in the {@link Database#tags}.
 */
@Slf4j
public final class Database {
    private final SessionFactory SESSION_FACTORY;
    public final TagManager tags = new TagManager(this);
    public final ModlogManager modlogs = new ModlogManager(this);

    public Database() {
        try {
            SESSION_FACTORY = new Configuration()
                    .configure()
                    .addClass(BanLog.class)
                    .addClass(GravelLog.class)
                    .addClass(KickLog.class)
                    .addClass(MuteLog.class)
                    .addClass(WarnLog.class)
                    .addClass(Tag.class)
                    .buildSessionFactory();
        } catch (Throwable e) {
            log.error("Could not initialise the Database", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    /**
     * Does a transaction with Hibernate
     *
     * @param consumer The logic of the transaction
     * @return true if the transaction was successful, false if an error or exception occured
     */
     boolean doTransaction(Consumer<Session> consumer) {
        Transaction transaction = null;
        try (Session session = SESSION_FACTORY.openSession()) {
            transaction = session.beginTransaction();
            consumer.accept(session);
            transaction.commit();
            return true;
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }

            log.error("An exception occurred while executing a database query", e);
            return false;
        }
    }
}
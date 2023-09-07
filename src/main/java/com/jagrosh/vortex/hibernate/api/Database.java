package com.jagrosh.vortex.hibernate.api;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.hibernate.entities.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An API for connecting to the H2 SQL Database using the <a href="https://www.hibernate.org">Hibernate ORM</a>
 * <p>
 * For organisation, methods that interact with a certain aspect of the database are in their respective classes.
 * For example, all methods that interact with modlogs are located in {@link Database#modlogs},
 * (ie. {@link Database#modlogs#getCase(long, int)}, {@link Database#modlogs#deleteCase(long, long, int), etc.},
 * while all methods that interact with tags would be in the {@link Database#tags}.
 */
// TODO: Hide so things are hidden jda api/implementation style
@Slf4j(topic = "Database")
public final class Database {
    public final @Getter Vortex vortex;
    public final TagManager tags = new TagManager(this);
    public final ModlogManager modlogs = new ModlogManager(this);
    public final GuildDataManager guild_data = new GuildDataManager(this);


    /* INTERNALS */
    private final SessionFactory SESSION_FACTORY;

    public Database(Vortex vortex) {
        this.vortex = vortex;
        this.SESSION_FACTORY = jpaBootstrap();
        modlogs.init();
    }

    private SessionFactory nativeBootsrap() {
        Metadata metadata = new MetadataSources()
                .addAnnotatedClass(Tag.class)
                .addAnnotatedClass(BanLog.class)
                .addAnnotatedClass(KickLog.class)
                .addAnnotatedClass(GravelLog.class)
                .addAnnotatedClass(MuteLog.class)
                .addAnnotatedClass(WarnLog.class)
                .addAnnotatedClass(SoftbanLog.class)
                .addAnnotatedClass(GuildData.class)
                .buildMetadata();

        try (SessionFactory sessionFactory = metadata.buildSessionFactory()) {
            return sessionFactory;
        } catch (Throwable e) {
            log.error("Could not initialise the Database", e);
            throw new ExceptionInInitializerError(e);
        }
    }

    private SessionFactory jpaBootstrap() {
        try {
            // FIXME
            getClass().getClassLoader().loadClass("com.github.benmanes.caffeine.jcache.spi.CaffeineCachingProvider");
        } catch (Exception e) {
            log.warn("Failed to load second level cache provider", e);
        }

        EntityManagerFactory emf = Persistence.createEntityManagerFactory("experimental-unit");
        return emf.unwrap(SessionFactory.class);
    }

    /**
     * Engages with a transaction with Hibernate
     *
     * @param function The logic of the transaction
     * @return The value returned by the transaction
     *
     * @throws PersistenceException If something goes wrong while interacting with the database
     */
    <T> T doTransaction(Function<Session, T> function) throws PersistenceException {
        Transaction transaction = null;
        try (Session session = SESSION_FACTORY.openSession()) {
            transaction = session.beginTransaction();
            T t = function.apply(session);
            transaction.commit();
            return t;
        } catch (Exception e) {
            log.error("An exception occurred while executing a database query", e);
            throw new PersistenceException(e);
        } finally {
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }

    /**
     * Engages with a transaction with Hibernate
     *
     * @param consumer The logic of the transaction
     *
     * @throws PersistenceException If something goes wrong while interacting with the database
     */
    void doTransaction(Consumer<Session> consumer) throws PersistenceException {
        Transaction transaction = null;
        try (Session session = SESSION_FACTORY.openSession()) {
            transaction = session.beginTransaction();
            consumer.accept(session);
            transaction.commit();
        } catch (Exception e) {
            log.error("An exception occurred while executing a database query", e);
            throw new PersistenceException(e);
        } finally {
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }
}
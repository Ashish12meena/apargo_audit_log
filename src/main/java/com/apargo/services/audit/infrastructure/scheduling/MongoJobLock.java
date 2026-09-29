package com.apargo.services.audit.infrastructure.scheduling;

import java.lang.management.ManagementFactory;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import org.bson.Document;

import com.apargo.services.audit.common.constant.MongoCollections;
import com.apargo.services.audit.common.constant.MongoFields;
import com.apargo.services.audit.common.constant.MongoFields.JobLock;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;

/**
 * Lease lock in {@code job_locks}, so only one instance runs a scheduled job at a time. A lock
 * whose lease expired (holder crashed) is taken over automatically.
 */
public class MongoJobLock {

    private final MongoCollection<Document> locks;
    private final Clock clock;
    private final String instanceId;

    public MongoJobLock(MongoDatabase database, Clock clock) {
        this.locks = database.getCollection(MongoCollections.JOB_LOCKS);
        this.clock = clock;
        this.instanceId = ManagementFactory.getRuntimeMXBean().getName() + "/" + UUID.randomUUID();
    }

    /** @return true if this instance now holds the lock for {@code leaseFor} */
    public boolean tryAcquire(String jobName, Duration leaseFor) {
        Instant now = clock.instant();
        try {
            locks.updateOne(
                    Filters.and(Filters.eq(MongoFields.ID, jobName), Filters.lte(JobLock.LOCKED_UNTIL, Date.from(now))),
                    Updates.combine(
                            Updates.set(JobLock.LOCKED_UNTIL, Date.from(now.plus(leaseFor))),
                            Updates.set(JobLock.LOCKED_AT, Date.from(now)),
                            Updates.set(JobLock.LOCKED_BY, instanceId)),
                    new UpdateOptions().upsert(true));
            return true;
        } catch (MongoWriteException e) {
            // The lock document exists and is still leased: the upsert collides on _id.
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY) {
                return false;
            }
            throw e;
        }
    }

    public void release(String jobName) {
        locks.updateOne(
                Filters.and(Filters.eq(MongoFields.ID, jobName), Filters.eq(JobLock.LOCKED_BY, instanceId)),
                Updates.set(JobLock.LOCKED_UNTIL, Date.from(clock.instant())));
    }
}

package com.allwage.clockin.repository.audit;

import com.allwage.clockin.model.AuditEvent;
import com.allwage.clockin.model.AuditEventType;
import com.allwage.clockin.model.AuditReasonCode;
import com.allwage.clockin.model.AuditSource;
import com.allwage.clockin.model.ClockAuditPayload;
import com.allwage.clockin.repository.store.DocumentStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditEventDocumentStoreRepositoryTest {

    @Test
    void save_rejectsDuplicateAuditEventIdAndPreservesOriginalEvent() {
        AuditEventDocumentStoreRepository repository = new AuditEventDocumentStoreRepository(new DocumentStore());
        AuditEvent original = auditEvent("audit-123", "clock-123", "emp-123");
        AuditEvent replacement = auditEvent("audit-123", "clock-456", "emp-456");

        repository.save(original);

        assertThatThrownBy(() -> repository.save(replacement))
            .isInstanceOf(IllegalStateException.class);

        assertThat(repository.findByType(AuditEventType.CLOCK_ACCEPTED))
            .containsExactly(original);
    }

    @Test
    void save_allowsOnlyOneConcurrentAppendForAnAuditEventId() throws InterruptedException {
        AuditEventDocumentStoreRepository repository = new AuditEventDocumentStoreRepository(new DocumentStore());
        AuditEvent first = auditEvent("audit-123", "clock-123", "emp-123");
        AuditEvent second = auditEvent("audit-123", "clock-456", "emp-456");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Void> firstAppend = executor.submit(() -> appendWhenStarted(repository, first, ready, start));
            Future<Void> secondAppend = executor.submit(() -> appendWhenStarted(repository, second, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(successfulAppends(firstAppend, secondAppend)).isEqualTo(1);
            assertThat(repository.findByType(AuditEventType.CLOCK_ACCEPTED))
                .hasSize(1)
                .containsAnyOf(first, second);
        } finally {
            executor.shutdownNow();
        }
    }

    private Void appendWhenStarted(
        AuditEventDocumentStoreRepository repository,
        AuditEvent auditEvent,
        CountDownLatch ready,
        CountDownLatch start
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        repository.save(auditEvent);
        return null;
    }

    private int successfulAppends(Future<Void> firstAppend, Future<Void> secondAppend) throws InterruptedException {
        return successfulAppend(firstAppend) + successfulAppend(secondAppend);
    }

    private int successfulAppend(Future<Void> append) throws InterruptedException {
        try {
            append.get(5, TimeUnit.SECONDS);
            return 1;
        } catch (ExecutionException exception) {
            assertThat(exception.getCause()).isInstanceOf(IllegalStateException.class);
            return 0;
        } catch (java.util.concurrent.TimeoutException exception) {
            throw new AssertionError("Concurrent audit append timed out", exception);
        }
    }

    private AuditEvent auditEvent(String id, String clockEventId, String employeeId) {
        return new AuditEvent(
            id,
            Instant.parse("2026-08-16T10:00:00Z"),
            "correlation-123",
            clockEventId,
            employeeId,
            AuditEventType.CLOCK_ACCEPTED,
            1,
            new ClockAuditPayload(AuditReasonCode.CLOCK_ACCEPTED, AuditSource.MOBILE_API, 200)
        );
    }
}

package com.allwage.clockin.service.audit;

import com.allwage.clockin.model.AuditEventType;
import com.allwage.clockin.model.AuditReasonCode;
import com.allwage.clockin.model.AuditSource;
import com.allwage.clockin.model.ClockAuditPayload;
import com.allwage.clockin.model.ClockEvent;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Maps clock-processing outcomes to versioned clock audit payloads.
 */
@Component
public class ClockProcessingAuditMapper implements AuditMapper {

    private static final int OK = 200;
    private static final int INTERNAL_SERVER_ERROR = 500;

    @Override
    public AuditDraft onSuccess(AuditInvocation invocation) {
        ClockEvent clockEvent = clockEvent(invocation.result());
        return new AuditDraft(
            clockEvent.id(),
            clockEvent.employeeId(),
            AuditEventType.CLOCK_ACCEPTED,
            new ClockAuditPayload(AuditReasonCode.CLOCK_ACCEPTED, AuditSource.MOBILE_API, OK)
        );
    }

    @Override
    public Optional<AuditDraft> onFailure(AuditInvocation invocation, Throwable failure) {
        if (invocation.arguments().length == 0 || !(invocation.arguments()[0] instanceof ClockEvent clockEvent)) {
            return Optional.empty();
        }
        return Optional.of(new AuditDraft(
            clockEvent.id(),
            clockEvent.employeeId(),
            AuditEventType.CLOCK_FAILED,
            new ClockAuditPayload(AuditReasonCode.CLOCK_PROCESSING_FAILED, AuditSource.MOBILE_API,
                INTERNAL_SERVER_ERROR)
        ));
    }

    private ClockEvent clockEvent(Object result) {
        if (result instanceof ClockEvent clockEvent) {
            return clockEvent;
        }
        throw new IllegalArgumentException("Clock processing must return a clock event");
    }
}

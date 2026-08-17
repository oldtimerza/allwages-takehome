package com.allwage.clockin.service.audit;

import com.allwage.clockin.model.audit.AuditEventType;
import com.allwage.clockin.model.audit.AuditReasonCode;
import com.allwage.clockin.model.audit.AuditSource;
import com.allwage.clockin.model.audit.ClockAuditPayload;
import com.allwage.clockin.model.clock.ClockEvent;
import com.allwage.clockin.model.clock.ClockValidationResult;
import com.allwage.clockin.model.Site.ValidatedClockEvent;
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
        return auditFor(validatedClockEvent(invocation.result()));
    }

    /**
     * Maps a persisted clock decision to an immutable audit draft.
     *
     * @param clockEvent processed clock event
     * @return audit draft for the clock outcome
     */
    public AuditDraft auditFor(ValidatedClockEvent clockEvent) {
        ClockValidationResult result = clockEvent.validationResult();
        return new AuditDraft(
            clockEvent.clockEvent().id(),
            clockEvent.clockEvent().employeeId(),
            result.decision() == ClockValidationResult.Decision.ACCEPTED
                ? AuditEventType.CLOCK_ACCEPTED
                : AuditEventType.CLOCK_REJECTED,
            new ClockAuditPayload(reasonCodeFor(result.reason()), AuditSource.MOBILE_API, OK)
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

    private ValidatedClockEvent validatedClockEvent(Object result) {
        if (result instanceof ValidatedClockEvent validatedClockEvent) {
            return validatedClockEvent;
        }
        throw new IllegalArgumentException("Clock processing must return a validated clock event");
    }

    private AuditReasonCode reasonCodeFor(ClockValidationResult.Reason reason) {
        return switch (reason) {
            case ACCEPTED -> AuditReasonCode.CLOCK_ACCEPTED;
            case FUTURE_TIMESTAMP -> AuditReasonCode.FUTURE_TIMESTAMP;
            case EMPLOYEE_NOT_FOUND -> AuditReasonCode.EMPLOYEE_NOT_FOUND;
            case NO_SITE_ASSIGNMENT -> AuditReasonCode.NO_SITE_ASSIGNMENT;
            case OUTSIDE_GEOFENCE -> AuditReasonCode.GEOFENCE_REJECTED;
            case AMBIGUOUS_SITE -> AuditReasonCode.AMBIGUOUS_SITE;
            case APPROVAL_REQUIRED -> AuditReasonCode.APPROVAL_REQUIRED;
        };
    }
}

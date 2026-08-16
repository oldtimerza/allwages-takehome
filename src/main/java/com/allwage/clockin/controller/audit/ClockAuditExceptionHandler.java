package com.allwage.clockin.controller.audit;

import com.allwage.clockin.controller.clock.ClockController;
import com.allwage.clockin.model.AuditEventType;
import com.allwage.clockin.model.AuditReasonCode;
import com.allwage.clockin.model.AuditSource;
import com.allwage.clockin.model.ClockAuditPayload;
import com.allwage.clockin.service.audit.AuditDraft;
import com.allwage.clockin.service.audit.AuditWriter;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Audits clock requests rejected before they reach the clock service.
 */
@RestControllerAdvice(assignableTypes = ClockController.class)
public class ClockAuditExceptionHandler {

    private static final int BAD_REQUEST = 400;

    private final AuditWriter auditWriter;

    public ClockAuditExceptionHandler(@NonNull AuditWriter auditWriter) {
        this.auditWriter = auditWriter;
    }

    /**
     * Records validation and deserialization rejections before returning a client error.
     *
     * @param exception request failure
     * @return bad request response
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public @NonNull ResponseEntity<Void> handleInvalidClockRequest(Exception exception) {
        AuditReasonCode reasonCode = exception instanceof MethodArgumentNotValidException
            ? AuditReasonCode.REQUEST_VALIDATION_FAILED
            : AuditReasonCode.REQUEST_MALFORMED;
        auditWriter.append(new AuditDraft(
            null,
            null,
            AuditEventType.CLOCK_REJECTED,
            new ClockAuditPayload(reasonCode, AuditSource.MOBILE_API, BAD_REQUEST)
        ));
        return ResponseEntity.badRequest().build();
    }
}

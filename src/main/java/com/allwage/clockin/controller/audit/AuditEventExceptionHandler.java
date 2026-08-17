package com.allwage.clockin.controller.audit;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts invalid audit retrieval parameters into client errors.
 */
@RestControllerAdvice(assignableTypes = AuditEventController.class)
public class AuditEventExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuditEventExceptionHandler.class);

    /**
     * Returns a bad request when audit paging constraints are violated.
     *
     * @param exception request binding or constraint failure
     * @return bad request response
     */
    @ExceptionHandler({ConstraintViolationException.class, MethodArgumentNotValidException.class})
    public @NonNull ResponseEntity<Void> handleInvalidAuditPageRequest(Exception exception) {
        log.warn("Rejected invalid audit retrieval request: {}", exception.getMessage());
        return ResponseEntity.badRequest().build();
    }
}

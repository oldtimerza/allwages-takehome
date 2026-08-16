package com.allwage.clockin.controller.site;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Returns client errors for malformed or invalid site validation-rule updates.
 */
@RestControllerAdvice(assignableTypes = SiteController.class)
public class SiteValidationExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(SiteValidationExceptionHandler.class);

    /**
     * Logs and returns a client error for requests rejected before reaching the site service.
     *
     * @param request HTTP request that was rejected
     * @param exception request failure
     * @return bad request response
     */
    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class,
        HttpMessageNotReadableException.class
    })
    public @NonNull ResponseEntity<Void> handleInvalidValidationRulesRequest(
        HttpServletRequest request,
        Exception exception
    ) {
        log.warn(
            "Rejected validation-rule update request: method={} uri={} reason={}",
            request.getMethod(), request.getRequestURI(), exception.getClass().getSimpleName()
        );
        return ResponseEntity.badRequest().build();
    }
}

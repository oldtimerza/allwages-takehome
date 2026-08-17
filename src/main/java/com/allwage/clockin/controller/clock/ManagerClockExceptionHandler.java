package com.allwage.clockin.controller.clock;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts invalid manager clock retrieval parameters into client errors.
 */
@RestControllerAdvice(assignableTypes = ManagerClockController.class)
public class ManagerClockExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ManagerClockExceptionHandler.class);

    /**
     * Returns a bad request when manager clock paging constraints are violated.
     *
     * @param exception request binding or constraint failure
     * @return bad request response
     */
    @ExceptionHandler({
        ConstraintViolationException.class,
        MethodArgumentNotValidException.class,
        MethodArgumentTypeMismatchException.class
    })
    public @NonNull ResponseEntity<Void> handleInvalidClockPageRequest(Exception exception) {
        log.warn("Rejected invalid manager clock retrieval request: {}", exception.getMessage());
        return ResponseEntity.badRequest().build();
    }
}

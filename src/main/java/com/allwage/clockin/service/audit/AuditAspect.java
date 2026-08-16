package com.allwage.clockin.service.audit;

import com.allwage.clockin.repository.store.DocumentStore;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

/**
 * Applies {@link Audited} declarations and keeps successful work and audit writes atomic.
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final ApplicationContext applicationContext;
    private final AuditContextProvider auditContextProvider;
    private final AuditWriter auditWriter;
    private final DocumentStore store;

    public AuditAspect(
        @NonNull ApplicationContext applicationContext,
        @NonNull AuditContextProvider auditContextProvider,
        @NonNull AuditWriter auditWriter,
        @NonNull DocumentStore store
    ) {
        this.applicationContext = applicationContext;
        this.auditContextProvider = auditContextProvider;
        this.auditWriter = auditWriter;
        this.store = store;
    }

    /**
     * Executes one audited use case and persists its success audit event atomically.
     *
     * @param joinPoint invoked service method
     * @param audited audit declaration
     * @return original method result
     * @throws Throwable original method failure or audit persistence failure
     */
    @Around("@annotation(audited)")
    public Object recordAudit(ProceedingJoinPoint joinPoint, Audited audited) throws Throwable {
        AuditMapper mapper = applicationContext.getBean(audited.mapper());
        AuditContext context = auditContextProvider.current();
        try {
            return store.executeAtomically(() -> recordSuccessfulInvocation(joinPoint, mapper, context));
        } catch (AuditedMethodFailure failure) {
            recordFailure(mapper, context, joinPoint.getArgs(), failure.getCause());
            throw failure.getCause();
        }
    }

    private Object recordSuccessfulInvocation(
        ProceedingJoinPoint joinPoint,
        AuditMapper mapper,
        AuditContext context
    ) {
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable failure) {
            throw new AuditedMethodFailure(failure);
        }
        auditWriter.append(mapper.onSuccess(new AuditInvocation(context, joinPoint.getArgs(), result)));
        return result;
    }

    private void recordFailure(AuditMapper mapper, AuditContext context, Object[] arguments, Throwable failure) {
        mapper.onFailure(new AuditInvocation(context, arguments, null), failure).ifPresent(auditWriter::append);
        log.warn("Audited operation failed with correlation {}", context.correlationId(), failure);
    }

    private static final class AuditedMethodFailure extends RuntimeException {

        AuditedMethodFailure(Throwable cause) {
            super(cause);
        }
    }
}

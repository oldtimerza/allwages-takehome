package com.allwage.clockin.service.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * Supplies the current request correlation context or a context for non-HTTP work.
 */
@Component
public class AuditContextProvider {

    public static final String CORRELATION_ID_ATTRIBUTE = AuditContextProvider.class.getName() + ".correlationId";

    /**
     * Gets the current audit context.
     *
     * @return current audit context
     */
    public AuditContext current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return new AuditContext(correlationId(attributes.getRequest()));
        }
        return new AuditContext(UUID.randomUUID().toString());
    }

    private String correlationId(HttpServletRequest request) {
        Object value = request.getAttribute(CORRELATION_ID_ATTRIBUTE);
        if (value instanceof String correlationId && !correlationId.isBlank()) {
            return correlationId;
        }
        String correlationId = UUID.randomUUID().toString();
        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        return correlationId;
    }
}

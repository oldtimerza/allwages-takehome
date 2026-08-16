package com.allwage.clockin.service.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a service use case produces an audit event through a mapper.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {

    /**
     * Mapper that converts this method's input and result into audit data.
     *
     * @return audit mapper type
     */
    Class<? extends AuditMapper> mapper();

    /**
     * Whether the aspect should write a success audit event after the method returns.
     * Services that write their success audit explicitly within a transaction can disable this.
     *
     * @return true when the aspect owns successful-operation audit writing
     */
    boolean auditSuccess() default true;
}

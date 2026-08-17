package com.allwage.clockin.service.audit;

import com.allwage.clockin.model.audit.AuditEventType;
import com.allwage.clockin.model.audit.AuditReasonCode;
import com.allwage.clockin.model.audit.AuditSource;
import com.allwage.clockin.model.audit.ClockAuditPayload;
import com.allwage.clockin.repository.transaction.TransactionRepository;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doReturn;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private AuditContextProvider auditContextProvider;

    @Mock
    private AuditWriter auditWriter;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private TestAuditMapper auditMapper;

    @Mock
    private Audited audited;

    @Test
    void givenSuccessfulAuditedOperation_whenRecordingAudit_thenWritesAuditInTransaction() throws Throwable {
        // Given
        AuditAspect auditAspect = new AuditAspect(
            applicationContext,
            auditContextProvider,
            auditWriter,
            transactionRepository
        );
        AuditContext context = new AuditContext("correlation-1");
        AuditDraft draft = new AuditDraft(
            "clock-1",
            "employee-1",
            AuditEventType.CLOCK_ACCEPTED,
            new ClockAuditPayload(AuditReasonCode.CLOCK_ACCEPTED, AuditSource.MOBILE_API, 200)
        );
        doReturn(TestAuditMapper.class).when(audited).mapper();
        given(audited.auditSuccess()).willReturn(true);
        given(applicationContext.getBean(TestAuditMapper.class)).willReturn(auditMapper);
        given(auditContextProvider.current()).willReturn(context);
        given(joinPoint.getArgs()).willReturn(new Object[] { "clock-1" });
        given(joinPoint.proceed()).willReturn("result");
        given(auditMapper.onSuccess(any())).willReturn(draft);
        given(transactionRepository.executeAtomically(any())).willAnswer(invocation -> {
            Supplier<?> operation = invocation.getArgument(0);
            return operation.get();
        });

        // When
        Object result = auditAspect.recordAudit(joinPoint, audited);

        // Then
        assertThat(result).isEqualTo("result");
        then(transactionRepository).should().executeAtomically(any());
        then(auditWriter).should().append(draft);
    }

    private static class TestAuditMapper implements AuditMapper {

        @Override
        public AuditDraft onSuccess(AuditInvocation invocation) {
            return null;
        }
    }
}

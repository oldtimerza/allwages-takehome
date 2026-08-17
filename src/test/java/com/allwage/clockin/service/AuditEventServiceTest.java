package com.allwage.clockin.service;

import com.allwage.clockin.model.audit.AuditEventPage;
import com.allwage.clockin.model.audit.AuditEventPageQuery;
import com.allwage.clockin.model.audit.AuditEventType;
import com.allwage.clockin.repository.audit.AuditEventRepository;
import com.allwage.clockin.service.audit.AuditEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditEventService auditEventService;

    @Test
    void findPage_delegatesTheDomainQueryWithoutMutation() {
        // Given
        AuditEventPageQuery query = new AuditEventPageQuery(1, 25, AuditEventType.CLOCK_REJECTED);
        AuditEventPage expectedPage = new AuditEventPage(java.util.List.of(), 1, 25, 0, 0);
        given(auditEventRepository.findPage(query)).willReturn(expectedPage);

        // When
        AuditEventPage actualPage = auditEventService.findPage(query);

        // Then
        assertThat(actualPage).isSameAs(expectedPage);
        then(auditEventRepository).should().findPage(query);
        then(auditEventRepository).shouldHaveNoMoreInteractions();
    }
}

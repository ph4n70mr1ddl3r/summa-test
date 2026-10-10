package com.summa.service;

import com.summa.repository.AuditEventRepository;
import com.summa.model.AuditEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuditService auditService;

    @Test
    void log_createsEventWithDefaults() {
        when(auditEventRepository.save(any())).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId("evt-1");
            return event;
        });

        AuditEvent result = auditService.log("h:admin", "CREATE", "human", "h:admin-1", "Created human");

        assertNotNull(result);
        assertEquals("h:admin", result.getActor());
        assertEquals("CREATE", result.getAction());
        assertEquals("human", result.getObjectType());
        assertEquals("h:admin-1", result.getObjectId());
        verify(auditEventRepository).save(any());
    }

    @Test
    void log_fallsBackToSystemActorWhenNull() {
        when(auditEventRepository.save(any())).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId("evt-1");
            return event;
        });

        AuditEvent result = auditService.log(null, "TEST", "test", "id-1", null);

        assertEquals("system", result.getActor());
        assertEquals("{}", result.getDetail());
    }

    @Test
    void log_fallsBackToUnknownWhenActionNull() {
        when(auditEventRepository.save(any())).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId("evt-1");
            return event;
        });

        AuditEvent result = auditService.log("h:admin", null, null, null, null);

        assertEquals("unknown", result.getAction());
        assertEquals("unknown", result.getObjectType());
        assertEquals("unknown", result.getObjectId());
    }

    @Test
    void logSystem_usesSystemActor() {
        when(auditEventRepository.save(any())).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId("evt-1");
            return event;
        });

        AuditEvent result = auditService.logSystem("REFUSAL", "auth_login", "email@test.com", "Rate limited");

        assertEquals("system", result.getActor());
        assertEquals("REFUSAL", result.getAction());
        verify(auditEventRepository).save(any());
    }

    @Test
    void logWithNode_includesNodeId() {
        when(auditEventRepository.save(any())).thenAnswer(invocation -> {
            AuditEvent event = invocation.getArgument(0);
            event.setId("evt-1");
            return event;
        });

        AuditEvent result = auditService.logWithNode("a:agent-1", "HEARTBEAT", "node", "node-1", "node-1", "tick");

        assertEquals("a:agent-1", result.getActor());
        assertEquals("node-1", result.getNodeId());
        verify(auditEventRepository).save(any());
    }
}

package com.summa.service;

import com.summa.repository.AuditEventRepository;
import com.summa.model.AuditEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @InjectMocks
    private AuditService auditService;

    @Test
    void log_createsEventWithDefaults() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuditEvent result = auditService.log("user-1", "CREATE", "workspace", "ws-1", "{\"name\":\"test\"}");

        assertNotNull(result);
        assertEquals("user-1", result.getActor());
        assertEquals("CREATE", result.getAction());
        assertEquals("workspace", result.getObjectType());
        assertEquals("ws-1", result.getObjectId());
        assertEquals("live", result.getOrigin());
        assertNotNull(result.getDetail());
    }

    @Test
    void log_systemActorWhenNull() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        auditService.log(null, null, null, null, "detail");

        verify(auditEventRepository).save(argThat(e ->
            "system".equals(e.getActor()) &&
            "unknown".equals(e.getAction()) &&
            "unknown".equals(e.getObjectType()) &&
            "unknown".equals(e.getObjectId())
        ));
    }

    @Test
    void log_sanitizeJsonPlainString() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuditEvent result = auditService.log("u1", "TEST", "obj", "oid", "not json at all");

        assertNotNull(result.getDetail());
    }

    @Test
    void log_sanitizeJsonValidObject() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuditEvent result = auditService.log("u1", "TEST", "obj", "oid", "{\"key\":\"value\"}");

        assertNotNull(result.getDetail());
    }

    @Test
    void log_sanitizeSensitiveRedactsPassword() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuditEvent result = auditService.log("u1", "LOGIN", "human", "h1",
            "{\"password\":\"secret123\",\"name\":\"alice\"}");

        assertNotNull(result.getDetail());
        assertNotEquals("{\"password\":\"secret123\",\"name\":\"alice\"}", result.getDetail());
    }

    @Test
    void log_sanitizeSensitiveRedactsApiKeys() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuditEvent result = auditService.log("u1", "CREATE", "agent", "a1",
            "{\"api_key\":\"sk-abc123\",\"token\":\"bearer_xyz\"}");

        assertNotNull(result.getDetail());
        assertNotEquals("{\"api_key\":\"sk-abc123\",\"token\":\"bearer_xyz\"}", result.getDetail());
    }

    @Test
    void logSystem_usesSystemActor() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        auditService.logSystem("SCHEDULED_JOB", "trigger", "t1", null);

        verify(auditEventRepository).save(argThat(e ->
            "system".equals(e.getActor())
        ));
    }

    @Test
    void logWithNode_includesNodeId() {
        when(auditEventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        auditService.logWithNode("user-1", "RUN", "run", "r1", "node-1", null);

        verify(auditEventRepository).save(argThat(e ->
            "node-1".equals(e.getNodeId())
        ));
    }
}

package com.summa.security;

import com.summa.controller.ControllerResponses;
import com.summa.model.AuditEvent;
import com.summa.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WriteGateTest {

    private AuditService auditService;
    private WriteGate writeGate;

    @BeforeEach
    void setUp() {
        auditService = mock(AuditService.class);
        writeGate = new WriteGate(auditService);
    }

    @Test
    void enforce_writeAllowed_returnsNull() {
        // Set ThreadLocal so isWriteAllowed returns true
        RbacAuthorizationFilter_TEST_ONLY.setWriteAllowed(true);

        ResponseEntity<Map<String, Object>> result = writeGate.enforce("actor-1");

        assertNull(result);
        verifyNoInteractions(auditService);
    }

    @Test
    void enforce_writeDenied_returnsGateResponse() {
        RbacAuthorizationFilter_TEST_ONLY.setWriteAllowed(false);
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setId("audit-1");
        when(auditService.log(eq("actor-1"), eq("REFUSAL"), eq("gate"), isNull(), eq("Viewer does not have write permission")))
                .thenReturn(auditEvent);

        ResponseEntity<Map<String, Object>> result = writeGate.enforce("actor-1");

        assertNotNull(result);
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
        Map<String, Object> body = result.getBody();
        assertNotNull(body);
        assertEquals("gate", body.get("code"));
        assertEquals("Viewer does not have write permission", body.get("message"));
        assertEquals("audit-1", body.get("audit_event_id"));
    }

    @Test
    void enforce_nullActor_writeDenied() {
        RbacAuthorizationFilter_TEST_ONLY.setWriteAllowed(false);
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setId("audit-2");
        when(auditService.log(eq(null), eq("REFUSAL"), eq("gate"), isNull(), eq("Viewer does not have write permission")))
                .thenReturn(auditEvent);

        ResponseEntity<Map<String, Object>> result = writeGate.enforce(null);

        assertNotNull(result);
        assertEquals(HttpStatus.FORBIDDEN, result.getStatusCode());
    }
}

/**
 * Test-only helper to control the RbacAuthorizationFilter ThreadLocal state.
 */
class RbacAuthorizationFilter_TEST_ONLY {
    static void setWriteAllowed(boolean allowed) {
        RbacAuthorizationFilter TEST_HELPER = new RbacAuthorizationFilter(null, null) {};
        // Use reflection to set the WRITES_ALLOWED ThreadLocal
        try {
            java.lang.reflect.Field field = RbacAuthorizationFilter.class.getDeclaredField("WRITES_ALLOWED");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.lang.ThreadLocal<Boolean> tl = (java.lang.ThreadLocal<Boolean>) field.get(null);
            tl.set(allowed);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}

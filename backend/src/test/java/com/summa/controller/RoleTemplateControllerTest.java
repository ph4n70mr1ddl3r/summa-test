package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.model.RoleTemplate;
import com.summa.security.TestAuthContext;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.service.RoleTemplateService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * The catalog's class vocabulary (DAT-010, DNC-061, TPL-002).
 *
 * <p>{@code role_templates.class} admits exactly two values —
 * {@code persistent} and {@code ephemeral-subagent}. The door used to validate
 * against the three spawn classes, so a request naming {@code ephemeral} passed
 * the door and then aborted the insert on the column's CHECK: a 409 integrity
 * failure where the API owes a 422 validation refusal (API-061).
 */
class RoleTemplateControllerTest {

    private RoleTemplateService templateService;
    private AuditService auditService;
    private MemberService memberService;
    private RoleTemplateController controller;

    @BeforeEach
    void setUp() {
        templateService = mock(RoleTemplateService.class);
        auditService = mock(AuditService.class);
        memberService = mock(MemberService.class);
        controller = new RoleTemplateController(templateService, auditService, new WriteGate(auditService), memberService);
        when(auditService.log(any(), any(), any(), any(), any())).thenReturn(newAuditEvent());
        when(auditService.logSystem(anyString(), anyString(), any(), any())).thenReturn(newAuditEvent());
    }

    @AfterEach
    void tearDown() {
        TestAuthContext.clear();
    }

    private AuditEvent newAuditEvent() {
        AuditEvent event = new AuditEvent();
        event.setId("audit-1");
        return event;
    }

    private String bodyMessage(org.springframework.http.ResponseEntity<?> res) {
        Object body = res.getBody();
        if (body instanceof Map<?, ?> map) {
            Object message = map.get("message");
            return message == null ? null : message.toString();
        }
        return null;
    }

    @Test
    void createTemplate_withTheBareEphemeralClass_refused() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);

        var res = controller.createTemplate(Map.of(
                "name", "Worker",
                "class", "ephemeral"));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("persistent, ephemeral-subagent"));
        verify(templateService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void createTemplate_withAnUnknownClass_refused() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);

        var res = controller.createTemplate(Map.of(
                "name", "Worker",
                "class", "wizard"));

        assertEquals(422, res.getStatusCodeValue());
        verify(templateService, never()).create(any(), any(), any(), any(), any());
    }

    @Test
    void createTemplate_withAnEphemeralSubagentClass_allowed() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(templateService.create(eq("Worker"), eq("ephemeral-subagent"), any(), any(), eq("admin-1")))
                .thenReturn(new RoleTemplate());

        var res = controller.createTemplate(Map.of(
                "name", "Worker",
                "class", "ephemeral-subagent"));

        assertEquals(200, res.getStatusCodeValue());
        verify(templateService).create(eq("Worker"), eq("ephemeral-subagent"), any(), any(), eq("admin-1"));
    }
}

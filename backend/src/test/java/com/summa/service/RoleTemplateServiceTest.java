package com.summa.service;

import com.summa.repository.RoleTemplateRepository;
import com.summa.repository.AgentRepository;
import com.summa.repository.SpawnRequestRepository;
import com.summa.model.RoleTemplate;
import com.summa.model.Agent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.summa.model.SpawnRequest;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class RoleTemplateServiceTest {

    @Mock
    private RoleTemplateRepository templateRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private SpawnRequestRepository spawnRequestRepository;

    @Mock
    private AskService askService;

    @InjectMocks
    private RoleTemplateService service;

    @Test
    void create_draftTemplate() {
        when(templateRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        RoleTemplate result = service.create("tpl-1", "persistent", "{}", "{}");

        assertNotNull(result);
        assertEquals("draft", result.getStatus());
        assertEquals("persistent", result.getAgentClass());
        verify(auditService).log(eq("system"), eq("CREATE"), eq("role_template"), anyString(), anyString());
    }

    @Test
    void findById_found() {
        RoleTemplate tpl = new RoleTemplate();
        tpl.setId("tpl-1");
        when(templateRepository.findById("tpl-1")).thenReturn(Optional.of(tpl));

        Optional<RoleTemplate> found = service.findById("tpl-1");
        assertTrue(found.isPresent());
        assertEquals("tpl-1", found.get().getId());
    }

    @Test
    void findById_notFound() {
        when(templateRepository.findById("nonexistent")).thenReturn(Optional.empty());

        Optional<RoleTemplate> found = service.findById("nonexistent");
        assertTrue(found.isEmpty());
    }

    @Test
    void publish_bumpsVersion() {
        RoleTemplate tpl = new RoleTemplate();
        tpl.setId("tpl-1");
        tpl.setName("Worker");
        tpl.setAgentClass("persistent");
        tpl.setVersion(1);
        tpl.setStatus("draft");
        when(templateRepository.findById("tpl-1")).thenReturn(Optional.of(tpl));
        when(templateRepository.findByName("Worker")).thenReturn(Optional.empty());
        when(agentRepository.findActiveByTemplateId("tpl-1")).thenReturn(List.of());
        when(templateRepository.save(any())).thenReturn(tpl);

        RoleTemplate result = service.publish("tpl-1", "admin");

        assertNotNull(result);
        assertEquals(2, result.getVersion());
        assertEquals("active", result.getStatus());
        verify(templateRepository, atLeastOnce()).save(any());
    }

    @Test
    void publish_classFlipRefused() {
        RoleTemplate current = new RoleTemplate();
        current.setId("tpl-1");
        current.setName("Worker");
        current.setAgentClass("persistent");
        current.setVersion(1);
        current.setStatus("draft");

        RoleTemplate existing = new RoleTemplate();
        existing.setId("tpl-2");
        existing.setName("Worker");
        existing.setAgentClass("ephemeral");
        existing.setVersion(3);
        existing.setStatus("active");

        when(templateRepository.findById("tpl-1")).thenReturn(Optional.of(current));
        when(templateRepository.findByName("Worker")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> service.publish("tpl-1", "admin"));
    }

    @Test
    void retire_withLivePins_refused() {
        RoleTemplate tpl = new RoleTemplate();
        tpl.setId("tpl-1");
        tpl.setName("Worker");
        tpl.setStatus("active");

        when(templateRepository.findById("tpl-1")).thenReturn(Optional.of(tpl));
        when(agentRepository.countByTemplateIdAndStatus("tpl-1", "active")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.retire("tpl-1", "admin"));
    }

    @Test
    void retire_withPendingSpawnPins_refused() {
        RoleTemplate tpl = new RoleTemplate();
        tpl.setId("tpl-1");
        tpl.setName("Worker");
        tpl.setStatus("active");

        when(templateRepository.findById("tpl-1")).thenReturn(Optional.of(tpl));
        when(agentRepository.countByTemplateIdAndStatus("tpl-1", "active")).thenReturn(0L);
        when(spawnRequestRepository.countByTemplateIdAndStatus("tpl-1", "requested")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> service.retire("tpl-1", "admin"));
    }

    @Test
    void retire_successfullyRetires() {
        RoleTemplate tpl = new RoleTemplate();
        tpl.setId("tpl-1");
        tpl.setName("Worker");
        tpl.setStatus("active");

        when(templateRepository.findById("tpl-1")).thenReturn(Optional.of(tpl));
        when(agentRepository.countByTemplateIdAndStatus("tpl-1", "active")).thenReturn(0L);
        when(spawnRequestRepository.countByTemplateIdAndStatus("tpl-1", "requested")).thenReturn(0L);
        when(templateRepository.save(any())).thenReturn(tpl);

        RoleTemplate result = service.retire("tpl-1", "admin");

        assertNotNull(result);
        assertEquals("retired", result.getStatus());
    }
}

package com.summa.service;

import com.summa.repository.SpawnRequestRepository;
import com.summa.repository.RoleTemplateRepository;
import com.summa.repository.AgentRepository;
import com.summa.model.SpawnRequest;
import com.summa.model.Agent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpawnServiceTest {

    @Mock
    private SpawnRequestRepository spawnRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private GovernanceService governanceService;

    @Mock
    private RoleTemplateRepository templateRepository;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private SpawnService spawnService;

    @Test
    void create_requestWithDefaults() {
        when(governanceService.isSpendHaltTripped()).thenReturn(false);

        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setStatus("active");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        SpawnRequest request = new SpawnRequest();
        request.setId("spawn-1");
        request.setSpawnClass("ephemeral");
        when(spawnRepository.save(any())).thenReturn(request);

        SpawnRequest result = spawnService.create("agent-1", null, null, "ephemeral",
            "Do task", "[]", "{}", null, 24, "human-1", "actor");

        assertNotNull(result);
        assertEquals("ephemeral", result.getSpawnClass());
    }

    @Test
    void approve_updatesStatus() {
        SpawnRequest request = new SpawnRequest();
        request.setId("spawn-1");
        request.setStatus("requested");
        when(spawnRepository.findById("spawn-1")).thenReturn(Optional.of(request));

        Agent agent = new Agent();
        agent.setId("agent-new");
        when(agentRepository.save(any())).thenReturn(agent);
        when(spawnRepository.save(any())).thenReturn(request);

        SpawnRequest result = spawnService.approve("spawn-1", "admin", "actor");

        assertEquals("approved", result.getStatus());
        assertNotNull(result.getApprovedAt());
        assertEquals("agent-new", result.getAgentId());
    }

    @Test
    void approve_throwsWhenNotRequested() {
        SpawnRequest request = new SpawnRequest();
        request.setId("spawn-1");
        request.setStatus("archived");
        when(spawnRepository.findById("spawn-1")).thenReturn(Optional.of(request));

        assertThrows(IllegalStateException.class, () -> {
            spawnService.approve("spawn-1", "admin", "actor");
        });
    }

    @Test
    void deny_setsDeniedStatus() {
        SpawnRequest request = new SpawnRequest();
        request.setId("spawn-1");
        request.setStatus("requested");
        when(spawnRepository.findById("spawn-1")).thenReturn(Optional.of(request));
        when(spawnRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SpawnRequest result = spawnService.deny("spawn-1", "admin");

        assertEquals("denied", result.getStatus());
    }
}

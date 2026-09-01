package com.summa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.summa.repository.SpawnRequestRepository;
import com.summa.repository.RoleTemplateRepository;
import com.summa.repository.AgentRepository;
import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.model.SpawnRequest;
import com.summa.model.Agent;
import com.summa.model.Human;
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

    @Mock
    private MemberService memberService;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private DnaDomainRepository domainRepository;

    @Mock
    private AskService askService;

    @Mock
    private SpendLedgerService spendLedgerService;

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private ObjectMapper objectMapper;

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
        request.setRequesterId("agent-1");
        request.setRequestedByHumanId("human-1");
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

    @Test
    void create_refusesScopeCeilingExceedingParent() {
        when(governanceService.isSpendHaltTripped()).thenReturn(false);

        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setStatus("active");
        agent.setTemplateId("template-1");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        com.summa.model.RoleTemplate template = new com.summa.model.RoleTemplate();
        template.setId("template-1");
        template.setDefaultScopes("{\"fs\":\"read\",\"shell\":\"none\"}");
        when(templateRepository.findById("template-1")).thenReturn(Optional.of(template));

        SpawnRequest request = new SpawnRequest();
        request.setId("spawn-1");
        request.setSpawnClass("ephemeral");
        lenient().when(spawnRepository.save(any())).thenReturn(request);

        // Stub ObjectMapper to return proper JsonNodes for scope validation
        com.fasterxml.jackson.databind.ObjectMapper realMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        JsonNode parentScopes = null;
        JsonNode childScopes = null;
        try {
            parentScopes = realMapper.readTree("{\"fs\":\"read\",\"shell\":\"none\"}");
            childScopes = realMapper.readTree("{\"fs\":\"write\"}");
        } catch (Exception ignored) {}
        try {
            org.mockito.Mockito.doReturn(parentScopes).when(objectMapper).readTree("{\"fs\":\"read\",\"shell\":\"none\"}");
            org.mockito.Mockito.doReturn(childScopes).when(objectMapper).readTree("{\"fs\":\"write\"}");
        } catch (Exception ignored) {}

        // scope ceiling requests fs:write which parent doesn't have — should fail
        assertThrows(IllegalStateException.class, () -> {
            spawnService.create("agent-1", null, null, "ephemeral",
                "Do task", "[]", "{\"fs\":\"write\"}", null, 24, "human-1", "actor");
        });
    }

    @Test
    void create_allowsScopeCeilingWithinParent() {
        when(governanceService.isSpendHaltTripped()).thenReturn(false);

        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setStatus("active");
        agent.setTemplateId("template-1");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        com.summa.model.RoleTemplate template = new com.summa.model.RoleTemplate();
        template.setId("template-1");
        template.setDefaultScopes("{\"fs\":\"readwrite\",\"shell\":\"exec\"}");
        when(templateRepository.findById("template-1")).thenReturn(Optional.of(template));

        SpawnRequest request = new SpawnRequest();
        request.setId("spawn-1");
        request.setSpawnClass("ephemeral");
        when(spawnRepository.save(any())).thenReturn(request);

        SpawnRequest result = spawnService.create("agent-1", null, null, "ephemeral",
            "Do task", "[]", "{\"fs\":\"readwrite\"}", null, 24, "human-1", "actor");

        assertNotNull(result);
        assertEquals("ephemeral", result.getSpawnClass());
    }
}

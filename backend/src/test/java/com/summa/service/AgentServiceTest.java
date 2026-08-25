package com.summa.service;

import com.summa.repository.AgentRepository;
import com.summa.repository.AskRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.SpawnRequestRepository;
import com.summa.repository.TriggerRepository;
import com.summa.model.Agent;
import com.summa.model.Ask;
import com.summa.model.Human;
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

@ExtendWith(MockitoExtension.class)
class AgentServiceTest {

    @Mock
    private AgentRepository agentRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MemberService memberService;

    @Mock
    private AskRepository askRepository;

    @Mock
    private BoardTaskRepository boardTaskRepository;

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private TriggerRepository triggerRepository;

    @Mock
    private SpawnRequestRepository spawnRequestRepository;

    private AgentService agentService;

    @BeforeEach
    void setUp() {
        agentService = new AgentService(agentRepository, auditService, memberService, askRepository,
            boardTaskRepository, initiativeRepository, triggerRepository, spawnRequestRepository, 2);
    }

    @Test
    void create_agentWithDefaults() {
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setName("Test Agent");
        agent.setStatus("requested");
        agent.setLineageDepth(0);
        when(agentRepository.save(any())).thenReturn(agent);

        Agent result = agentService.create(
            "agent-1", "Test Agent", "human-1", "persistent",
            null, null, null, null, null, null
        );

        assertNotNull(result);
        assertEquals(0, result.getLineageDepth());
    }

    @Test
    void suspend_activeAgent() {
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setStatus("active");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Agent result = agentService.suspend("agent-1", "admin");

        assertEquals("suspended", result.getStatus());
        assertNotNull(result.getSuspendedAt());
    }

    @Test
    void suspend_throwsWhenNotActive() {
        Agent agent = new Agent();
        agent.setStatus("retiring");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        assertThrows(IllegalStateException.class, () -> {
            agentService.suspend("agent-1", "admin");
        });
    }

    @Test
    void resume_suspendedAgent() {
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setStatus("suspended");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Agent result = agentService.resume("agent-1", "admin");

        assertEquals("active", result.getStatus());
        assertNull(result.getSuspendedAt());
    }

    @Test
    void retire_resolvesPendingAsks() {
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setStatus("active");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        when(askRepository.findByFromAndStatusPending("agent-1")).thenReturn(List.of(ask));

        Agent result = agentService.retire("agent-1", "admin");

        assertEquals("retiring", result.getStatus());
        verify(askRepository).save(ask);
        assertEquals("withdrawn", ask.getStatus());
    }

    @Test
    void archive_agent() {
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setStatus("active");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));
        when(agentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Agent result = agentService.archive("agent-1", "admin");

        assertEquals("archived", result.getStatus());
        assertNotNull(result.getArchivedAt());
    }

    @Test
    void findByOwner() {
        Agent agent = new Agent();
        agent.setId("agent-1");
        when(agentRepository.findActiveByOwner("human-1")).thenReturn(List.of(agent));

        List<Agent> result = agentService.findByOwner("human-1");

        assertEquals(1, result.size());
        assertEquals("agent-1", result.get(0).getId());
    }

    @Test
    void findChildren() {
        Agent child = new Agent();
        child.setId("child-1");
        when(agentRepository.findBySpawnedBy("parent-1")).thenReturn(List.of(child));

        List<Agent> result = agentService.findChildren("parent-1");

        assertEquals(1, result.size());
    }

    @Test
    void findFirstHumanUpChain_returnsHuman() {
        Agent agent = new Agent();
        agent.setOwnerHumanId("human-1");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        Human human = new Human();
        human.setId("human-1");
        when(memberService.findHuman("human-1")).thenReturn(Optional.of(human));

        Optional<Human> result = agentService.findFirstHumanUpChain("agent-1");

        assertTrue(result.isPresent());
        assertEquals("human-1", result.get().getId());
    }

    @Test
    void findFirstHumanUpChain_fallsThroughToSpawner() {
        Agent agent = new Agent();
        agent.setOwnerHumanId("human-1");
        agent.setSpawnedBy("agent-2");
        when(agentRepository.findById("agent-1")).thenReturn(Optional.of(agent));

        Agent agent2 = new Agent();
        agent2.setSpawnedBy(null);
        when(agentRepository.findById("agent-2")).thenReturn(Optional.of(agent2));

        Optional<Human> result = agentService.findFirstHumanUpChain("agent-1");

        assertFalse(result.isPresent());
    }
}

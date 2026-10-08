package com.summa.service;

import com.summa.repository.WorkspaceRepository;
import com.summa.repository.DnaDomainRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.repository.TriggerRepository;
import com.summa.repository.SpawnRequestRepository;
import com.summa.repository.NodeRepository;
import com.summa.repository.RunRepository;
import com.summa.model.Workspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private DnaDomainRepository domainRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private InitiativeRepository initiativeRepository;

    @Mock
    private TriggerRepository triggerRepository;

    @Mock
    private SpawnRequestRepository spawnRequestRepository;

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private RunRepository runRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WorkspaceService workspaceService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        workspaceService = new WorkspaceService(workspaceRepository, domainRepository, auditService, objectMapper,
            initiativeRepository, triggerRepository, spawnRequestRepository, nodeRepository, runRepository);
    }

    @Test
    void create_withDefaults() {
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workspace result = workspaceService.create(
            "ws-1", "Project Alpha", "project", null, null, null, null, "actor"
        );

        assertNotNull(result);
        assertEquals("project", result.getKind());
        assertEquals("[]", result.getDomainIds());
        assertEquals("[]", result.getInitiativeIds());
    }

    @Test
    void create_withExplicitValues() {
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(new com.summa.model.DnaDomain()));

        Workspace result = workspaceService.create(
            "ws-2", "Project Beta", "project",
            "[\"domain-1\"]", "[\"init-1\"]", "node-1", "[\"user-1\"]", "actor"
        );

        assertEquals("[\"domain-1\"]", result.getDomainIds());
        assertEquals("[\"init-1\"]", result.getInitiativeIds());
        assertEquals("node-1", result.getNodeId());
    }

    @Test
    void rebind_workspace() {
        Workspace ws = new Workspace();
        ws.setId("ws-1");
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(ws));
        lenient().when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(nodeRepository.findById("node-2")).thenReturn(Optional.empty());

        try {
            workspaceService.rebind("ws-1", "node-2", "admin");
            fail("Should throw for missing target node");
        } catch (EntityNotFoundException e) {
            // expected
        }
    }

    @Test
    void rebind_workspace_withValidNode() {
        Workspace ws = new Workspace();
        ws.setId("ws-1");
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(ws));
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        com.summa.model.Node node = new com.summa.model.Node();
        node.setId("node-2");
        node.setStatus("trusted");
        node.setCapabilities("{\"backup\":true}");
        when(nodeRepository.findById("node-2")).thenReturn(Optional.of(node));

        Workspace result = workspaceService.rebind("ws-1", "node-2", "admin");

        assertEquals("node-2", result.getNodeId());
    }

    @Test
    void rebind_throwsWhenNotFound() {
        when(workspaceRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            workspaceService.rebind("missing", "node-1", "admin");
        });
    }

    @Test
    void archive_workspace() {
        Workspace ws = new Workspace();
        ws.setId("ws-1");
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(ws));
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workspace result = workspaceService.archive("ws-1", "admin");

        assertNotNull(result.getArchivedAt());
    }

    @Test
    void findByNode() {
        Workspace ws = new Workspace();
        ws.setId("ws-1");
        when(workspaceRepository.findByNodeId("node-1")).thenReturn(List.of(ws));

        List<Workspace> result = workspaceService.findByNode("node-1");

        assertEquals(1, result.size());
    }

    @Test
    void findAllActive() {
        Workspace ws = new Workspace();
        ws.setId("ws-1");
        when(workspaceRepository.findByArchivedAtIsNull()).thenReturn(List.of(ws));

        List<Workspace> result = workspaceService.findAllActive();

        assertEquals(1, result.size());
    }

    @Test
    void archive_noFalsePositivePlaybookMatch() throws Exception {
        // Playbook scanning was removed as dead code — archive still works without it.
        Workspace ws = new Workspace();
        ws.setId("ws-1");
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(ws));
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workspace result = workspaceService.archive("ws-1", "admin");
        assertNotNull(result.getArchivedAt());
    }

    @Test
    void archive_withActiveRun() throws Exception {
        Workspace ws = new Workspace();
        ws.setId("ws-1");
        when(workspaceRepository.findById("ws-1")).thenReturn(Optional.of(ws));
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        com.summa.model.Run run = new com.summa.model.Run();
        run.setId("run-1");
        run.setStatus("queued");
        when(runRepository.findByWorkspaceIdAndStatusIn("ws-1", List.of("queued", "running"))).thenReturn(List.of(run));
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workspace result = workspaceService.archive("ws-1", "admin");

        assertNotNull(result.getArchivedAt());
        verify(runRepository).save(run);
        assertEquals("cancelled", run.getStatus());
    }
}

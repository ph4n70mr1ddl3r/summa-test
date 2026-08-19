package com.summa.service;

import com.summa.repository.WorkspaceRepository;
import com.summa.model.Workspace;
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
class WorkspaceServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private WorkspaceService workspaceService;

    @Test
    void create_withDefaults() {
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workspace result = workspaceService.create(
            "ws-1", "Project Alpha", "project", null, null, null, null
        );

        assertNotNull(result);
        assertEquals("project", result.getKind());
        assertEquals("[]", result.getDomainIds());
        assertEquals("[]", result.getInitiativeIds());
    }

    @Test
    void create_withExplicitValues() {
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workspace result = workspaceService.create(
            "ws-2", "Project Beta", "shared",
            "[\"domain-1\"]", "[\"init-1\"]", "node-1", "[\"user-1\"]"
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
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Workspace result = workspaceService.rebind("ws-1", "node-2", "admin");

        assertEquals("node-2", result.getNodeId());
    }

    @Test
    void rebind_throwsWhenNotFound() {
        when(workspaceRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
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
}

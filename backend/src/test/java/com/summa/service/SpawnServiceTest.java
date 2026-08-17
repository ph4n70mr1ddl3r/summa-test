package com.summa.service;

import com.summa.repository.SpawnRequestRepository;
import com.summa.model.SpawnRequest;
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

    @InjectMocks
    private SpawnService spawnService;

    @Test
    void create_requestWithDefaults() {
        when(governanceService.isSpendHaltTripped()).thenReturn(false);
        
        SpawnRequest request = new SpawnRequest();
        request.setId("spawn-1");
        request.setSpawnClass("ephemeral");
        when(spawnRepository.save(any())).thenReturn(request);

        SpawnRequest result = spawnService.create("agent-1", "template-1", null, "ephemeral",
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
        when(spawnRepository.save(any())).thenReturn(request);

        SpawnRequest result = spawnService.approve("spawn-1", "admin", "actor");

        assertEquals("approved", result.getStatus());
        assertNotNull(result.getApprovedAt());
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
}

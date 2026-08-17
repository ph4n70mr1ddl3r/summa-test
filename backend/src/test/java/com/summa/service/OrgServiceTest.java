package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.model.Human;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrgServiceTest {

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private OffboardingWalkService offboardingWalkService;

    @InjectMocks
    private OrgService orgService;

    @Test
    void bootstrap_createsFirstAdmin() {
        when(humanRepository.count()).thenReturn(0L);

        Human human = new Human();
        human.setId("test-id");
        human.setName("Test Admin");
        human.setEmail("admin@test.com");
        human.setRbac("admin");
        when(humanRepository.save(any())).thenReturn(human);

        Human result = orgService.bootstrap("Test Admin", "admin@test.com", "admin");

        assertNotNull(result);
        assertEquals("admin", result.getRbac());
    }

    @Test
    void bootstrap_refusesAfterFirstHuman() {
        when(humanRepository.count()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> {
            orgService.bootstrap("Another Admin", "other@test.com", "admin");
        });
    }

    @Test
    void offboard_refusesLastAdmin() {
        when(humanRepository.findById("human-1")).thenReturn(Optional.of(createHuman("human-1", "admin")));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> {
            orgService.offboard("human-1", "admin-2");
        });
    }

    @Test
    void offboard_succeedsWhenNotLastAdmin() {
        Human human = createHuman("human-1", "member");
        when(humanRepository.findById("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(2L);
        when(offboardingWalkService.walkOffboard(eq("human-1"), eq(null), eq("admin-2")))
            .thenReturn(Map.of("humanId", "human-1"));

        Human result = orgService.offboard("human-1", "admin-2");

        assertNotNull(result);
    }

    private Human createHuman(String id, String rbac) {
        Human h = new Human();
        h.setId(id);
        h.setRbac(rbac);
        h.setDeactivatedAt(null);
        return h;
    }
}

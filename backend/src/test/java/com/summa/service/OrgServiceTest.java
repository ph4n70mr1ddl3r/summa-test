package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.model.Human;
import com.summa.security.PasswordUtil;
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

    @Mock
    private PasswordUtil passwordUtil;

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

        Human result = orgService.bootstrap("Test Admin", "admin@test.com", "admin", "testpass");

        assertNotNull(result);
        assertEquals("admin", result.getRbac());
    }

    @Test
    void bootstrap_refusesAfterFirstHuman() {
        when(humanRepository.count()).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> {
            orgService.bootstrap("Another Admin", "other@test.com", "admin", null);
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

    @Test
    void setDeputy_detectsSelfDeputy() {
        Human human = createHuman("human-1", "member");
        when(humanRepository.findById("human-1")).thenReturn(Optional.of(human));

        assertThrows(IllegalArgumentException.class, () -> {
            orgService.setDeputy("human-1", "human-1", "admin");
        });
    }

    @Test
    void setDeputy_refusesViewerAsDeputy() {
        Human human = createHuman("human-1", "member");
        Human viewer = createHuman("human-2", "viewer");
        when(humanRepository.findById("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.findById("human-2")).thenReturn(Optional.of(viewer));

        assertThrows(IllegalArgumentException.class, () -> {
            orgService.setDeputy("human-1", "human-2", "admin");
        });
    }

    @Test
    void setDeputy_detectsCycle() {
        Human human1 = createHuman("human-1", "member");
        human1.setDeputyMemberId("human-2");
        Human human2 = createHuman("human-2", "member");
        human2.setDeputyMemberId("human-1");
        when(humanRepository.findById("human-1")).thenReturn(Optional.of(human1));
        when(humanRepository.findById("human-2")).thenReturn(Optional.of(human2));

        assertThrows(IllegalStateException.class, () -> {
            orgService.setDeputy("human-1", "human-2", "admin");
        });
    }

    @Test
    void setDeputy_succeedsWhenNoCycle() {
        Human human1 = createHuman("human-1", "member");
        Human human2 = createHuman("human-2", "member");
        when(humanRepository.findById("human-1")).thenReturn(Optional.of(human1));
        when(humanRepository.findById("human-2")).thenReturn(Optional.of(human2));
        when(humanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Human result = orgService.setDeputy("human-1", "human-2", "admin");

        assertNotNull(result);
        assertEquals("human-2", result.getDeputyMemberId());
    }
}

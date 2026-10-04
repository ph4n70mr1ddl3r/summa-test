package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AgentRepository;
import com.summa.model.Human;
import com.summa.model.Agent;
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
class MemberServiceTest {

    @Mock
    private HumanRepository humanRepository;

    @Mock
    private AgentRepository agentRepository;

    @InjectMocks
    private MemberService memberService;

    @Test
    void findHuman_returnsPresent() {
        Human h = new Human();
        h.setId("h1");
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h));

        Optional<Human> result = memberService.findHuman("h1");

        assertTrue(result.isPresent());
        assertEquals("h1", result.get().getId());
    }

    @Test
    void findHuman_returnsEmpty() {
        when(humanRepository.findById("missing")).thenReturn(Optional.empty());

        Optional<Human> result = memberService.findHuman("missing");

        assertTrue(result.isEmpty());
    }

    @Test
    void findAgent_returnsPresent() {
        Agent a = new Agent();
        a.setId("a1");
        when(agentRepository.findById("a1")).thenReturn(Optional.of(a));

        Optional<Agent> result = memberService.findAgent("a1");

        assertTrue(result.isPresent());
        assertEquals("a1", result.get().getId());
    }

    @Test
    void isViewer_trueForViewerRole() {
        Human h = new Human();
        h.setRbac("viewer");
        assertTrue(memberService.isViewer(h));
    }

    @Test
    void isViewer_falseForAdmin() {
        Human h = new Human();
        h.setRbac("admin");
        assertFalse(memberService.isViewer(h));
    }

    @Test
    void isViewer_returnsFalseForNull() {
        assertFalse(memberService.isViewer(null));
    }

    @Test
    void hasWriteSurface_trueForActiveAdmin() {
        Human h = new Human();
        h.setRbac("admin");
        assertTrue(memberService.hasWriteSurface(h));
    }

    @Test
    void hasWriteSurface_falseForViewer() {
        Human h = new Human();
        h.setRbac("viewer");
        assertFalse(memberService.hasWriteSurface(h));
    }

    @Test
    void hasWriteSurface_falseForInactive() {
        Human h = new Human();
        h.setRbac("admin");
        h.setDeactivatedAt(java.time.Instant.now());
        assertFalse(memberService.hasWriteSurface(h));
    }

    @Test
    void hasWriteSurfaceAgent_trueForActiveNonEphemeral() {
        Agent a = new Agent();
        a.setStatus("active");
        a.setAgentClass("persistent");
        assertTrue(memberService.hasWriteSurfaceAgent(a));
    }

    @Test
    void hasWriteSurfaceAgent_falseForEphemeral() {
        Agent a = new Agent();
        a.setStatus("active");
        a.setAgentClass("ephemeral");
        assertFalse(memberService.hasWriteSurfaceAgent(a));
    }

    @Test
    void hasWriteSurfaceAgent_falseForNull() {
        assertFalse(memberService.hasWriteSurfaceAgent(null));
    }

    @Test
    void isAdmin_returnsTrueForAdmin() {
        Human h = new Human();
        h.setRbac("admin");
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h));

        assertTrue(memberService.isAdmin("h1"));
    }

    @Test
    void isAdmin_returnsFalseForNonAdmin() {
        Human h = new Human();
        h.setRbac("member");
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h));

        assertFalse(memberService.isAdmin("h1"));
    }

    @Test
    void isAdmin_returnsFalseForNullId() {
        assertFalse(memberService.isAdmin(null));
    }

    @Test
    void isAdmin_returnsFalseForSystemActor() {
        assertFalse(memberService.isAdmin("system"));
    }

    @Test
    void saveHuman_delegatesToRepository() {
        Human h = new Human();
        h.setId("h1");
        when(humanRepository.save(any())).thenReturn(h);

        Human result = memberService.saveHuman(h);

        assertNotNull(result);
        verify(humanRepository).save(h);
    }

    @Test
    void findAllActiveHumans_delegatesToRepository() {
        Human active = new Human();
        active.setId("h1");
        Human deactivated = new Human();
        deactivated.setId("h2");
        deactivated.setDeactivatedAt(java.time.Instant.now());
        when(humanRepository.findAllActive()).thenReturn(List.of(active, deactivated));

        List<Human> result = memberService.findAllActiveHumans();

        assertEquals(2, result.size());
    }

    @Test
    void findAllActiveAgents_returnsAllActive() {
        Agent active1 = new Agent();
        active1.setId("a1");
        active1.setStatus("active");
        Agent active2 = new Agent();
        active2.setId("a2");
        active2.setStatus("active");
        when(agentRepository.findAllActive()).thenReturn(List.of(active1, active2));

        List<Agent> result = memberService.findAllActiveAgents();

        assertEquals(2, result.size());
    }

    @Test
    void findAdmins_returnsAdminHumans() {
        Human admin = new Human();
        admin.setId("h1");
        admin.setRbac("admin");
        when(humanRepository.findActiveByRole("admin")).thenReturn(List.of(admin));

        List<Human> result = memberService.findAdmins();

        assertEquals(1, result.size());
        assertEquals("h1", result.get(0).getId());
    }

    @Test
    void findAdmins_returnsEmptyWhenNone() {
        when(humanRepository.findActiveByRole("admin")).thenReturn(List.of());

        List<Human> result = memberService.findAdmins();

        assertTrue(result.isEmpty());
    }

    @Test
    void findOwnerHumans_returnsOwnerHumans() {
        Human owner = new Human();
        owner.setId("h1");
        owner.setRbac("owner");
        when(humanRepository.findActiveByRole("owner")).thenReturn(List.of(owner));

        List<Human> result = memberService.findOwnerHumans();

        assertEquals(1, result.size());
        assertEquals("h1", result.get(0).getId());
    }

    @Test
    void countActiveAdmins_returnsCount() {
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(3L);

        long count = memberService.countActiveAdmins();

        assertEquals(3, count);
    }

    @Test
    void canWrite_returnsTrueForAdmin() {
        assertTrue(memberService.canWrite("admin"));
    }

    @Test
    void canWrite_returnsTrueForOwner() {
        assertTrue(memberService.canWrite("owner"));
    }

    @Test
    void canWrite_returnsFalseForModerator() {
        assertFalse(memberService.canWrite("moderator"));
    }

    @Test
    void canWrite_returnsFalseForViewer() {
        assertFalse(memberService.canWrite("viewer"));
    }

    @Test
    void canWrite_returnsFalseForNull() {
        assertFalse(memberService.canWrite(null));
    }

    @Test
    void canWrite_returnsFalseForUnknown() {
        assertFalse(memberService.canWrite("unknown"));
    }

    @Test
    void isAdmin_returnsFalseWhenHumanNotFound() {
        when(humanRepository.findById("missing")).thenReturn(Optional.empty());

        assertFalse(memberService.isAdmin("missing"));
    }
}

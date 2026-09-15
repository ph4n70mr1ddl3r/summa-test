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
}

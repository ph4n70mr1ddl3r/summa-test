package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AgentRepository;
import com.summa.model.Human;
import com.summa.model.Agent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
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
    void requireEligibleAskTarget_allowsAnActiveNonViewerHuman() {
        // ORG-020: an active member who is not a viewer is ask-eligible.
        Human h = new Human();
        h.setId("h1");
        h.setRbac("member");
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h));

        assertDoesNotThrow(() -> memberService.requireEligibleAskTarget("h:h1"));
    }

    @Test
    void requireEligibleAskTarget_refusesAViewer() {
        // ORG-020: a viewer is never an ask target.
        Human h = new Human();
        h.setId("v1");
        h.setRbac("viewer");
        when(humanRepository.findById("v1")).thenReturn(Optional.of(h));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> memberService.requireEligibleAskTarget("v1"));
        assertTrue(ex.getMessage().contains("Viewers cannot be ask targets"));
    }

    @Test
    void requireEligibleAskTarget_refusesANonActiveMember() {
        // ORG-020: a non-active member is refused every answerable post at write.
        Human h = new Human();
        h.setId("h1");
        h.setRbac("member");
        h.setDeactivatedAt(java.time.Instant.now());
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h));

        assertThrows(IllegalStateException.class, () -> memberService.requireEligibleAskTarget("h1"));
    }

    @Test
    void requireEligibleAskTarget_refusesAnUnknownMember() {
        when(humanRepository.findById("ghost")).thenReturn(Optional.empty());
        when(agentRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> memberService.requireEligibleAskTarget("ghost"));
    }

    @Test
    void requireEligibleAskTarget_allowsTheReservedBroadcast() {
        // The `admins` broadcast is not member-addressed — it stays exempt.
        assertDoesNotThrow(() -> memberService.requireEligibleAskTarget("admins"));
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
    void isAdmin_returnsFalseForDeactivatedAdmin() {
        // OFB-015: deactivation is credential-death — a departed admin's token
        // must not keep satisfying every admin gate.
        Human h = new Human();
        h.setRbac("admin");
        h.setDeactivatedAt(Instant.now());
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h));

        assertFalse(memberService.isAdmin("h1"));
    }

    @Test
    void toKeyedMemberRef_resolvesHumanKind() {
        Human h = new Human();
        h.setId("h1");
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h));

        assertEquals("h:h1", memberService.toKeyedMemberRef("h1"));
        assertEquals("h:h1", memberService.toKeyedMemberRef("h:h1"));
    }

    @Test
    void toKeyedMemberRef_resolvesAgentKind() {
        // The kind is resolved by live lookup — an agent reference must never be
        // relabelled as a human one.
        when(humanRepository.findById("a1")).thenReturn(Optional.empty());
        Agent agent = new Agent();
        agent.setId("a1");
        when(agentRepository.findById("a1")).thenReturn(Optional.of(agent));

        assertEquals("a:a1", memberService.toKeyedMemberRef("a1"));
        assertEquals("a:a1", memberService.toKeyedMemberRef("a:a1"));
    }

    @Test
    void toKeyedMemberRef_passesReservedSpellingsThrough() {
        assertEquals("system", memberService.toKeyedMemberRef("system"));
        assertEquals("admins", memberService.toKeyedMemberRef("admins"));
    }

    @Test
    void toKeyedMemberRef_returnsNullForUnknownMember() {
        when(humanRepository.findById("ghost")).thenReturn(Optional.empty());
        when(agentRepository.findById("ghost")).thenReturn(Optional.empty());

        assertNull(memberService.toKeyedMemberRef("ghost"));
        assertNull(memberService.toKeyedMemberRef(null));
        assertNull(memberService.toKeyedMemberRef("  "));
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

    @Test
    void resolveDeputyTarget_returnsEmptyWhenHumanNotFound() {
        when(humanRepository.findById("missing")).thenReturn(Optional.empty());

        assertTrue(memberService.resolveDeputyTarget("missing").isEmpty());
    }

    @Test
    void resolveDeputyTarget_returnsEmptyWhenNoDeputy() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        human.setDeactivatedAt(null);
        when(humanRepository.findById("h1")).thenReturn(Optional.of(human));

        assertTrue(memberService.resolveDeputyTarget("h1").isEmpty());
    }

    @Test
    void resolveDeputyTarget_returnsDeputyIdWhenEligible() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        human.setDeputyMemberId("h2");
        human.setDeactivatedAt(null);
        Human deputy = new Human();
        deputy.setId("h2");
        deputy.setRbac("admin");
        deputy.setDeactivatedAt(null);
        when(humanRepository.findById("h1")).thenReturn(Optional.of(human));
        when(humanRepository.findById("h2")).thenReturn(Optional.of(deputy));

        assertEquals("h:h2", memberService.resolveDeputyTarget("h1").orElse(null));
    }

    @Test
    void resolveDeputyTarget_skipsInactiveDeputy() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        human.setDeputyMemberId("h2");
        human.setDeactivatedAt(null);
        Human deputy = new Human();
        deputy.setId("h2");
        deputy.setRbac("admin");
        deputy.setDeactivatedAt(Instant.now());
        when(humanRepository.findById("h1")).thenReturn(Optional.of(human));
        when(humanRepository.findById("h2")).thenReturn(Optional.of(deputy));

        assertTrue(memberService.resolveDeputyTarget("h1").isEmpty());
    }

    @Test
    void resolveDeputyTarget_skipsViewerDeputy() {
        Human human = new Human();
        human.setId("h1");
        human.setRbac("member");
        human.setDeputyMemberId("h2");
        human.setDeactivatedAt(null);
        Human deputy = new Human();
        deputy.setId("h2");
        deputy.setRbac("viewer");
        deputy.setDeactivatedAt(null);
        when(humanRepository.findById("h1")).thenReturn(Optional.of(human));
        when(humanRepository.findById("h2")).thenReturn(Optional.of(deputy));

        assertTrue(memberService.resolveDeputyTarget("h1").isEmpty());
    }

    @Test
    void walkLineage_followsSpawnerLinksToTheRootOwner() {
        // ORG-050: lineage is the spawner line — a3 (worker) spawned by a2
        // (spawner) owned by h1 (root).
        Agent worker = new Agent();
        worker.setId("a3");
        worker.setSpawnedBy("a2");
        Agent spawner = new Agent();
        spawner.setId("a2");
        spawner.setSpawnedBy("h1");
        when(agentRepository.findById("a3")).thenReturn(Optional.of(worker));
        when(agentRepository.findById("a2")).thenReturn(Optional.of(spawner));
        when(humanRepository.findById("a3")).thenReturn(Optional.empty());
        when(humanRepository.findById("a2")).thenReturn(Optional.empty());
        when(agentRepository.findById("h1")).thenReturn(Optional.empty());

        assertEquals(List.of("a3", "a2", "h1"), memberService.walkLineage("a3", 10));
    }

    @Test
    void walkLineage_ignoresTheDeputyLinkOfTheRoot() {
        // ORG-060: the root human's deputy is a successor, never a lineage
        // hop — the walk stops at the root even when a live deputy exists.
        Agent spawner = new Agent();
        spawner.setId("a2");
        spawner.setSpawnedBy("h1");
        when(agentRepository.findById("a2")).thenReturn(Optional.of(spawner));
        when(humanRepository.findById("a2")).thenReturn(Optional.empty());
        when(agentRepository.findById("h1")).thenReturn(Optional.empty());

        assertEquals(List.of("a2", "h1"), memberService.walkLineage("a2", 10));
    }

    @Test
    void walkLineage_fromAHumanStopsAtTheChainRoot() {
        // ORG-050/ORG-061: a human is always a chain root — its deputy is a
        // successor, not an ancestor, so the walk returns the human and stops.
        Human h1 = newHuman("h1");
        h1.setDeputyMemberId("h2");
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h1));

        assertEquals(List.of("h1"), memberService.walkLineage("h1", 10));
        assertEquals(List.of("h1"), memberService.walkLineage("h:h1", 10));
    }

    @Test
    void walkLineage_normalizesKeyedSpawnerPointers() {
        // A keyed-union spawner pointer ("h1") must resolve against the
        // humans table, never against the wrong namespace.
        Agent spawner = new Agent();
        spawner.setId("a2");
        spawner.setSpawnedBy("h:h1");
        when(agentRepository.findById("a2")).thenReturn(Optional.of(spawner));
        when(humanRepository.findById("a2")).thenReturn(Optional.empty());
        when(agentRepository.findById("h1")).thenReturn(Optional.empty());

        assertEquals(List.of("a2", "h1"), memberService.walkLineage("a:a2", 10));
    }

    @Test
    void walkLineage_honorsTheDepthCap() {
        // The depth cap budgets hops: a3 -> a2 -> a1 would be three, so
        // with a cap of two the walk records [a3, a2] and stops.
        Agent a3 = new Agent();
        a3.setId("a3");
        a3.setSpawnedBy("a2");
        Agent a2 = new Agent();
        a2.setId("a2");
        a2.setSpawnedBy("a1");
        when(agentRepository.findById("a3")).thenReturn(Optional.of(a3));
        when(agentRepository.findById("a2")).thenReturn(Optional.of(a2));
        when(humanRepository.findById("a3")).thenReturn(Optional.empty());
        when(humanRepository.findById("a2")).thenReturn(Optional.empty());

        assertEquals(List.of("a3", "a2"), memberService.walkLineage("a3", 2));
    }

    @Test
    void walkLineage_walksToARootOwnerHumanEvenBeyondTheDepthCap() {
        // ORG-050: the chain must terminate at a human; a depth cap that
        // lands mid-chain stops at the cap — the budget, not the lineage,
        // governs how many hops are walked.
        Agent a3 = new Agent();
        a3.setId("a3");
        a3.setSpawnedBy("a2");
        Agent a2 = new Agent();
        a2.setId("a2");
        a2.setSpawnedBy("h1");
        when(agentRepository.findById("a3")).thenReturn(Optional.of(a3));
        when(agentRepository.findById("a2")).thenReturn(Optional.of(a2));
        when(humanRepository.findById("a3")).thenReturn(Optional.empty());
        when(humanRepository.findById("a2")).thenReturn(Optional.empty());

        assertEquals(List.of("a3", "a2"), memberService.walkLineage("a3", 2));
        assertEquals(List.of("a3", "a2", "h1"), memberService.walkLineage("a3", 3));
    }

    @Test
    void walkLineage_stopsOnASpawnerCycle() {
        // A mis-configured spawned_by cycle must end at the hop, not loop.
        Agent a1 = new Agent();
        a1.setId("a1");
        a1.setSpawnedBy("a2");
        Agent a2 = new Agent();
        a2.setId("a2");
        a2.setSpawnedBy("a1");
        when(agentRepository.findById("a1")).thenReturn(Optional.of(a1));
        when(agentRepository.findById("a2")).thenReturn(Optional.of(a2));
        when(humanRepository.findById("a1")).thenReturn(Optional.empty());
        when(humanRepository.findById("a2")).thenReturn(Optional.empty());

        assertEquals(List.of("a1", "a2"), memberService.walkLineage("a1", 10));
    }

    @Test
    void walkLineage_doesNotFollowDeputyLinks() {
        // ORG-060: the deputy is a successor, not an ancestor — from a human
        // start the walk returns the human and stops.
        Human h1 = newHuman("h1");
        h1.setDeputyMemberId("h2");
        when(humanRepository.findById("h1")).thenReturn(Optional.of(h1));

        assertEquals(List.of("h1"), memberService.walkLineage("h1", 10));
        assertEquals(List.of("h1"), memberService.walkLineage("h:h1", 10));
    }

    private Human newHuman(String id) {
        Human h = new Human();
        h.setId(id);
        h.setRbac("member");
        h.setDeactivatedAt(null);
        return h;
    }
}

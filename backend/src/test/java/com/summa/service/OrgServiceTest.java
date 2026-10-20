package com.summa.service;

import com.summa.repository.HumanRepository;
import com.summa.repository.AuditEventRepository;
import com.summa.repository.AskRepository;
import com.summa.repository.BoardTaskRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.repository.MemoryItemRepository;
import com.summa.repository.PatRepository;
import com.summa.repository.DataHoldRepository;
import com.summa.model.Human;
import com.summa.model.Ask;
import com.summa.model.BoardTask;
import com.summa.model.DnaProposal;
import com.summa.model.MemoryItem;
import com.summa.model.DataHold;
import com.summa.security.PasswordUtil;
import com.summa.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
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

    @Mock
    private AuditEventRepository auditEventRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private AskRepository askRepository;

    @Mock
    private BoardTaskRepository boardTaskRepository;

    @Mock
    private DnaProposalRepository proposalRepository;

    @Mock
    private MemoryItemRepository memoryItemRepository;

    @Mock
    private PatRepository patRepository;

    @Mock
    private DataHoldRepository dataHoldRepository;

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

        Human result = orgService.bootstrap("Test Admin", "admin@test.com", "admin", "Testpass12!!");

        assertNotNull(result);
        assertEquals("admin", result.getRbac());
    }

    @Test
    void bootstrap_refusesAfterFirstHuman() {
        when(humanRepository.count()).thenReturn(1L);

        assertThrows(com.summa.exception.ConflictException.class, () -> {
            orgService.bootstrap("Another Admin", "other@test.com", "admin", null);
        });
    }

    @Test
    void offboard_refusesLastAdmin() {
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(1L);
        Human human = createHuman("human-1", "admin");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));

        assertThrows(IllegalStateException.class, () -> {
            orgService.offboard("human-1", "admin-2");
        });
    }

    @Test
    void offboard_succeedsWhenNotLastAdmin() {
        Human human = createHuman("human-1", "member");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(2L);
        when(offboardingWalkService.walkOffboard(eq("human-1"), eq(null), eq("admin-2")))
            .thenReturn(Map.of("humanId", "human-1"));

        Human result = orgService.offboard("human-1", "admin-2");

        assertNotNull(result);
    }

    @Test
    void offboard_succeedsForNonAdminInASingleAdminOrg() {
        // OFB-020: the guard is about the *last admin* — offboarding a member who
        // is not an admin never empties the admin set, so a single-admin org must
        // still be able to offboard its other members.
        Human human = createHuman("human-1", "member");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(1L);
        when(offboardingWalkService.walkOffboard(eq("human-1"), eq(null), eq("admin-2")))
            .thenReturn(Map.of("humanId", "human-1"));

        Human result = orgService.offboard("human-1", "admin-2");

        assertNotNull(result);
        verify(offboardingWalkService).walkOffboard("human-1", null, "admin-2");
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
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.findById("human-1")).thenReturn(Optional.of(human));

        assertThrows(IllegalArgumentException.class, () -> {
            orgService.setDeputy("human-1", "human-1", "admin");
        });
    }

    @Test
    void setDeputy_refusesViewerAsDeputy() {
        Human human = createHuman("human-1", "member");
        Human viewer = createHuman("human-2", "viewer");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));
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
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human1));
        when(humanRepository.findById("human-2")).thenReturn(Optional.of(human2));

        assertThrows(IllegalStateException.class, () -> {
            orgService.setDeputy("human-1", "human-2", "admin");
        });
    }

    @Test
    void erasure_pseudonymizesAndSweeps() {
        Human human = createHuman("human-1", "member");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dataHoldRepository.findByKindAndSubjectIdAndReleasedAtIsNull("member", "human-1"))
            .thenReturn(List.of());

        Human admin = createHuman("admin-1", "admin");
        when(memberService.findAdmins()).thenReturn(List.of(admin));

        Ask fromAsk = new Ask();
        fromAsk.setFrom("human-1");
        Ask toAsk = new Ask();
        toAsk.setTo("human-1");
        when(askRepository.findByFrom("human-1")).thenReturn(List.of(fromAsk));
        when(askRepository.findByFrom("h:human-1")).thenReturn(List.of());
        when(askRepository.findByTo("human-1")).thenReturn(List.of(toAsk));
        when(askRepository.findByTo("h:human-1")).thenReturn(List.of());
        when(askRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DnaProposal prop = new DnaProposal();
        prop.setProposedBy("human-1");
        when(proposalRepository.findByProposedBy("human-1")).thenReturn(List.of(prop));
        when(proposalRepository.findByProposedBy("h:human-1")).thenReturn(List.of());
        when(proposalRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BoardTask task = new BoardTask();
        task.setAssigneeMemberId("human-1");
        when(boardTaskRepository.findByAssigneeMemberId("human-1")).thenReturn(List.of(task));
        when(boardTaskRepository.findByAssigneeMemberId("h:human-1")).thenReturn(List.of());
        when(boardTaskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MemoryItem mem = new MemoryItem();
        mem.setMemberId("human-1");
        when(memoryItemRepository.findByMemberId("human-1")).thenReturn(List.of(mem));
        when(memoryItemRepository.findByMemberId("h:human-1")).thenReturn(List.of());
        when(memoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        when(patRepository.findByMemberId("human-1")).thenReturn(List.of());
        when(patRepository.findByMemberId("h:human-1")).thenReturn(List.of());

        orgService.erasure("human-1", "admin-1");

        assertEquals("system", fromAsk.getFrom());
        assertEquals("h:admin-1", toAsk.getTo());
        assertEquals("h:admin-1", prop.getProposedBy());
        assertNull(task.getAssigneeMemberId());
        assertNull(mem.getMemberId());
    }

    @Test
    void erasure_refusesWhenNoActiveAdmin() {
        Human human = createHuman("human-1", "member");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dataHoldRepository.findByKindAndSubjectIdAndReleasedAtIsNull("member", "human-1"))
            .thenReturn(List.of());
        when(memberService.findAdmins()).thenReturn(List.of());

        assertThrows(ConflictException.class, () -> {
            orgService.erasure("human-1", "admin-1");
        });
    }

    @Test
    void erasure_refusesUnderLegalHold() {
        Human human = createHuman("human-1", "member");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human));
        when(humanRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dataHoldRepository.findByKindAndSubjectIdAndReleasedAtIsNull("member", "human-1"))
            .thenReturn(List.of(new DataHold()));

        assertThrows(ConflictException.class, () -> {
            orgService.erasure("human-1", "admin-1");
        });
    }

    @Test
    void setDeputy_succeedsWhenNoCycle() {
        Human human1 = createHuman("human-1", "member");
        Human human2 = createHuman("human-2", "member");
        when(humanRepository.findByIdForUpdate("human-1")).thenReturn(Optional.of(human1));
        when(humanRepository.findById("human-2")).thenReturn(Optional.of(human2));
        when(humanRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Human result = orgService.setDeputy("human-1", "human-2", "admin");

        assertNotNull(result);
        assertEquals("human-2", result.getDeputyMemberId());
    }

    @Test
    void updateRbac_refusesAnUnknownRole() {
        // API-061: an unknown role is a validation failure (422), not an
        // integrity abort (409) — and the demotion walk must not run against a
        // role the row can never carry.
        assertThrows(IllegalArgumentException.class, () ->
            orgService.updateRbac("human-1", "superuser", "admin-1"));
    }

    // --- erasure: pseudonymization, uniqueness, and the last-admin guard ---

    @Test
    void erasure_writesAUniqueOneWayPseudonymEmail() {
        // STG-030: the member reference becomes a one-way pseudonym. The email
        // column is UNIQUE, so a shared constant made every erasure after the
        // first abort on the constraint.
        Human human = new Human();
        human.setId("h-1");
        human.setRbac("member");
        human.setEmail("member@corp.com");
        when(humanRepository.findByIdForUpdate("h-1")).thenReturn(Optional.of(human));
        when(dataHoldRepository.findByKindAndSubjectIdAndReleasedAtIsNull(anyString(), anyString()))
            .thenReturn(List.of());
        when(proposalRepository.findByProposedBy(anyString())).thenReturn(List.of());
        when(askRepository.findByFrom(anyString())).thenReturn(List.of());
        when(askRepository.findByTo(anyString())).thenReturn(List.of());
        when(boardTaskRepository.findByAssigneeMemberId(anyString())).thenReturn(List.of());
        when(memoryItemRepository.findByMemberId(anyString())).thenReturn(List.of());
        when(patRepository.findByMemberId(anyString())).thenReturn(List.of());
        Human admin = new Human();
        admin.setId("admin-1");
        when(memberService.findAdmins()).thenReturn(List.of(admin));

        orgService.erasure("h-1", "admin-1");

        assertNotEquals("member@corp.com", human.getEmail());
        assertTrue(human.getEmail().startsWith("erased-"));
        assertTrue(human.getEmail().endsWith("@erased.invalid"));

        // The pseudonym is stable for the member and unique across members.
        Human other = new Human();
        other.setId("h-2");
        assertNotEquals(human.getEmail(), OrgService.erasurePseudonymEmail("h-2"));
        assertEquals(human.getEmail(), OrgService.erasurePseudonymEmail("h-1"));
    }

    @Test
    void erasure_refusesTheLastAdmin() {
        // OFB-020: erasure deactivates the member, so the last-admin guard binds
        // here exactly as it does on the offboarding door.
        Human human = new Human();
        human.setId("h-1");
        human.setRbac("admin");
        when(humanRepository.findByIdForUpdate("h-1")).thenReturn(Optional.of(human));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> orgService.erasure("h-1", "admin-1"));
        verify(humanRepository, never()).save(any());
    }

    // --- the RBAC edit door: the walk runs on a reduction, never on an elevation ---

    @Test
    void updateRbac_ownerToMember_runsTheDemotionWalk() {
        // OFB-030/OFB-033: a role change that strips what the member holds runs
        // the dependency walk — owner → member sheds domain ownership.
        Human human = new Human();
        human.setId("h-1");
        human.setRbac("owner");
        when(humanRepository.findByIdForUpdate("h-1")).thenReturn(Optional.of(human));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(3L);
        when(humanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        orgService.updateRbac("h-1", "member", "admin-1");

        verify(offboardingWalkService).walkDemote("h-1", "member", "admin-1");
    }

    @Test
    void updateRbac_aRaise_runsNoWalk() {
        Human human = new Human();
        human.setId("h-1");
        human.setRbac("member");
        when(humanRepository.findByIdForUpdate("h-1")).thenReturn(Optional.of(human));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(3L);
        when(humanRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        orgService.updateRbac("h-1", "owner", "admin-1");

        verify(offboardingWalkService, never()).walkDemote(anyString(), anyString(), anyString());
    }

    @Test
    void demote_refusesAnElevation() {
        // OFB-030: the demotion walk is destructive — it transfers domains, closes
        // asks and revokes credentials — so the demote door never elevates.
        Human human = new Human();
        human.setId("h-1");
        human.setRbac("viewer");
        when(humanRepository.findByIdForUpdate("h-1")).thenReturn(Optional.of(human));
        when(humanRepository.countByDeactivatedAtIsNullAndRbac("admin")).thenReturn(3L);

        assertThrows(IllegalArgumentException.class, () -> orgService.demote("h-1", "admin", "admin-1"));
        verify(offboardingWalkService, never()).walkDemote(anyString(), anyString(), anyString());
    }
}

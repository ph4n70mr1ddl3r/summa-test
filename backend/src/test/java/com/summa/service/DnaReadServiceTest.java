package com.summa.service;

import com.summa.repository.DnaDomainRepository;
import com.summa.repository.DnaProposalRepository;
import com.summa.model.DnaDomain;
import com.summa.model.DnaProposal;
import com.summa.model.Agent;
import com.summa.model.Human;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaReadServiceTest {

    @Mock
    private DnaDomainRepository domainRepository;

    @Mock
    private DnaProposalRepository proposalRepository;

    @Mock
    private MemberService memberService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private DnaReadService service;

    @Test
    void search_rejectsBlankQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.search("", null, 10));
        assertThrows(IllegalArgumentException.class, () -> service.search("   ", null, 10));
    }

    @Test
    void search_rejectsOperatorOnlyQuery() {
        assertThrows(IllegalArgumentException.class, () -> service.search("* OR *", null, 10));
    }

    @Test
    void search_limitClamped() {
        assertDoesNotThrow(() -> service.search("test", null, 0));
    }

    @Test
    void search_limitCappedAt100() {
        assertDoesNotThrow(() -> service.search("test", null, 9999));
    }

    @Test
    void listDomains_delegatesToRepository() {
        DnaDomain domain = new DnaDomain();
        domain.setId("d1");
        domain.setName("Engineering");
        when(domainRepository.findAllActive()).thenReturn(List.of(domain));

        List<DnaDomain> domains = service.listDomains();

        assertEquals(1, domains.size());
        assertEquals("d1", domains.get(0).getId());
    }

    @Test
    void getReviewQueue_forDomain() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("p1");
        when(proposalRepository.findOpenByDomain("domain-1")).thenReturn(List.of(proposal));

        List<DnaProposal> result = service.getReviewQueue("domain-1");

        assertEquals(1, result.size());
        verify(proposalRepository).findOpenByDomain("domain-1");
    }

    @Test
    void getReviewQueue_allDomains() {
        DnaProposal proposal = new DnaProposal();
        proposal.setId("p1");
        when(proposalRepository.findAllOpen()).thenReturn(List.of(proposal));

        List<DnaProposal> result = service.getReviewQueue(null);

        assertEquals(1, result.size());
        verify(proposalRepository).findAllOpen();
    }

    @Test
    void getOrgSnapshot_returnsStructuredMap() {
        Human human = new Human();
        human.setId("h1");
        human.setName("Alice");
        human.setRbac("admin");

        Agent agent = new Agent();
        agent.setId("a1");
        agent.setName("Bot");
        agent.setAgentClass("persistent");
        agent.setStatus("active");
        agent.setSuspendedAt(null);
        agent.setRetiredAt(null);

        DnaDomain domain = new DnaDomain();
        domain.setId("d1");
        domain.setName("Eng");
        domain.setAccess("public");

        when(memberService.findAllActiveHumans()).thenReturn(List.of(human));
        when(memberService.findAllActiveAgents()).thenReturn(List.of(agent));
        when(domainRepository.findAllActive()).thenReturn(List.of(domain));

        Map<String, Object> snapshot = service.getOrgSnapshot();

        assertNotNull(snapshot);
        assertTrue(snapshot.containsKey("humans"));
        assertTrue(snapshot.containsKey("agents"));
        assertTrue(snapshot.containsKey("domains"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> humans = (List<Map<String, Object>>) snapshot.get("humans");
        assertEquals(1, humans.size());
        assertEquals("h1", humans.get(0).get("id"));
        assertEquals("admin", humans.get(0).get("rbac"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> agents = (List<Map<String, Object>>) snapshot.get("agents");
        assertEquals(1, agents.size());
        assertEquals("a1", agents.get(0).get("id"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> domains = (List<Map<String, Object>>) snapshot.get("domains");
        assertEquals(1, domains.size());
        assertEquals("d1", domains.get(0).get("id"));
    }
}

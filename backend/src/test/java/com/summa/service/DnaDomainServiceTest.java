package com.summa.service;

import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDomain;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DnaDomainServiceTest {

    @Mock
    private DnaDomainRepository domainRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DnaDomainService domainService;

    @Test
    void create_domainWithDefaults() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setName("Engineering");
        domain.setAccess("public");
        domain.setStore("git");
        domain.setReviewSlaDays(7);
        when(domainRepository.save(any())).thenReturn(domain);

        DnaDomain result = domainService.create("domain-1", "Engineering", "human-1", null, null, null, null);

        assertNotNull(result);
        assertEquals("Engineering", result.getName());
    }

    @Test
    void archive_setsArchivedStatus() {
        DnaDomain domain = new DnaDomain();
        domain.setId("domain-1");
        domain.setStatus("active");
        when(domainRepository.findById("domain-1")).thenReturn(Optional.of(domain));
        when(domainRepository.save(any())).thenReturn(domain);

        DnaDomain result = domainService.archive("domain-1", "admin");

        assertEquals("archived", result.getStatus());
    }

    @Test
    void archive_throwsWhenNotFound() {
        when(domainRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            domainService.archive("nonexistent", "admin");
        });
    }
}

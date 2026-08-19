package com.summa.service;

import com.summa.repository.PatRepository;
import com.summa.model.Pat;
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
class PatServiceTest {

    @Mock
    private PatRepository patRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private PatService patService;

    @Test
    void create_generatesTokenAndHash() {
        Pat pat = new Pat();
        pat.setId("pat-1");
        pat.setName("Deploy Key");
        when(patRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PatService.PatWithToken result = patService.create("human-1", "Deploy Key", List.of("runs:write"), 90);

        assertNotNull(result);
        assertNotNull(result.token());
        assertTrue(result.token().startsWith("summa_pat_"));
        assertNotNull(result.pat().getTokenHash());
    }

    @Test
    void create_defaultsScopesToEmpty() {
        Pat pat = new Pat();
        pat.setId("pat-2");
        when(patRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        PatService.PatWithToken result = patService.create("human-1", "Read Key", null, 30);

        assertEquals("[]", result.pat().getScopes());
    }

    @Test
    void revoke_pat() {
        Pat pat = new Pat();
        pat.setId("pat-1");
        when(patRepository.findByIdAndRevokedAtIsNull("pat-1")).thenReturn(Optional.of(pat));
        when(patRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Pat result = patService.revoke("pat-1", "human-1");

        assertNotNull(result.getRevokedAt());
    }

    @Test
    void revoke_throwsWhenNotFound() {
        when(patRepository.findByIdAndRevokedAtIsNull("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            patService.revoke("missing", "human-1");
        });
    }

    @Test
    void touchLastUsed_updatesTimestamp() {
        Pat pat = new Pat();
        pat.setId("pat-1");
        when(patRepository.findById("pat-1")).thenReturn(Optional.of(pat));
        when(patRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        patService.touchLastUsed("pat-1");

        verify(patRepository).save(pat);
    }

    @Test
    void touchLastUsed_skipsMissing() {
        when(patRepository.findById("missing")).thenReturn(Optional.empty());

        patService.touchLastUsed("missing");

        verify(patRepository).findById("missing");
        verifyNoMoreInteractions(patRepository);
    }

    @Test
    void findByMember() {
        Pat pat = new Pat();
        pat.setId("pat-1");
        when(patRepository.findByMemberId("human-1")).thenReturn(List.of(pat));

        List<Pat> result = patService.findByMember("human-1");

        assertEquals(1, result.size());
    }

    @Test
    void findByHash() {
        Pat pat = new Pat();
        pat.setId("pat-1");
        when(patRepository.findByTokenHash("hash1")).thenReturn(Optional.of(pat));

        Optional<Pat> result = patService.findByHash("hash1");

        assertTrue(result.isPresent());
    }
}

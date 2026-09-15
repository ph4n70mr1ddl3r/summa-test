package com.summa.service;

import com.summa.repository.DataHoldRepository;
import com.summa.model.DataHold;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class DataHoldServiceTest {

    @Mock
    private DataHoldRepository holdRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private DataHoldService dataHoldService;

    @Test
    void create_newHold() {
        DataHold hold = new DataHold();
        hold.setId("dh-1");
        when(holdRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DataHold result = dataHoldService.create("legal", "subject-1", "reason", "admin");

        assertNotNull(result);
        assertEquals("legal", result.getKind());
        assertEquals("subject-1", result.getSubjectId());
    }

    @Test
    void findById_returnsPresent() {
        DataHold hold = new DataHold();
        hold.setId("dh-1");
        when(holdRepository.findById("dh-1")).thenReturn(Optional.of(hold));

        Optional<DataHold> result = dataHoldService.findById("dh-1");

        assertTrue(result.isPresent());
    }

    @Test
    void hasActiveHold_returnsTrue() {
        when(holdRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("legal", "subject-1"))
            .thenReturn(true);

        assertTrue(dataHoldService.hasActiveHold("legal", "subject-1"));
    }

    @Test
    void hasActiveHold_returnsFalse() {
        when(holdRepository.existsByKindAndSubjectIdAndReleasedAtIsNull("legal", "subject-1"))
            .thenReturn(false);

        assertFalse(dataHoldService.hasActiveHold("legal", "subject-1"));
    }

    @Test
    void release_findsAndReleases() {
        DataHold hold = new DataHold();
        hold.setId("dh-1");
        when(holdRepository.findById("dh-1")).thenReturn(Optional.of(hold));
        when(holdRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        DataHold result = dataHoldService.release("dh-1", "admin");

        assertNotNull(result.getReleasedAt());
        verify(auditService).log(eq("admin"), eq("RELEASE_HOLD"), eq("data_hold"), eq("dh-1"), isNull());
    }

    @Test
    void release_throwsWhenNotFound() {
        when(holdRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            dataHoldService.release("missing", "admin"));
    }
}

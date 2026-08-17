package com.summa.service;

import com.summa.repository.AskRepository;
import com.summa.model.Ask;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AskServiceTest {

    @Mock
    private AskRepository askRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private AskService askService;

    @Test
    void createAsk_withDefaultValues() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setKind("approval");
        ask.setFrom("agent-1");
        ask.setTo("human-1");
        ask.setSlaTier("standard");
        ask.setExpiryBehavior("deny");
        
        when(askRepository.save(any())).thenReturn(ask);
        
        Ask result = askService.create(
            "approval", "agent-1", "human-1", "{}", 
            "standard", "deny", 1, 
            Instant.now().plusSeconds(3600), null, null
        );
        
        assertNotNull(result);
        assertEquals("approval", result.getKind());
    }

    @Test
    void createAsk_refusesPastDeadline() {
        assertThrows(IllegalArgumentException.class, () -> {
            askService.create(
                "approval", "agent-1", "human-1", "{}",
                "standard", "deny", 1,
                Instant.now().minusSeconds(3600), null, null
            );
        });
    }

    @Test
    void respond_updatesStatus() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setTo("human-1");
        
        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);
        
        Ask result = askService.respond("ask-1", "human-1", "approved");
        
        assertEquals("answered", result.getStatus());
        assertNotNull(result.getRespondedAt());
    }

    @Test
    void expire_updatesStatus() {
        Ask ask = new Ask();
        ask.setId("ask-1");
        ask.setStatus("pending");
        ask.setDeadline(Instant.now().minusSeconds(3600));
        
        when(askRepository.findById("ask-1")).thenReturn(Optional.of(ask));
        when(askRepository.save(any())).thenReturn(ask);
        
        Ask result = askService.expire("ask-1");
        
        assertEquals("expired", result.getStatus());
    }
}

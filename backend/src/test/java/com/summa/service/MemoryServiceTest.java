package com.summa.service;

import com.summa.repository.MemoryItemRepository;
import com.summa.model.MemoryItem;
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
class MemoryServiceTest {

    @Mock
    private MemoryItemRepository memoryItemRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private DnaDomainService domainService;

    @Mock
    private MemberService memberService;

    @Mock
    private WorkspaceService workspaceService;

    @InjectMocks
    private MemoryService memoryService;

    @Test
    void create_memoryItem() {
        when(memoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MemoryItem result = memoryService.create("short", "human-1", "ws-1", "Content", "{}", false);

        assertNotNull(result);
        assertEquals("short", result.getTier());
        assertEquals("human-1", result.getMemberId());
        assertFalse(result.isTainted());
    }

    @Test
    void create_defaultsProvenance() {
        when(memoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MemoryItem result = memoryService.create("long", null, null, "Content", null, false);

        assertEquals("{}", result.getProvenance());
        assertNull(result.getMemberId());
    }

    @Test
    void review_clearsTaint() {
        MemoryItem item = new MemoryItem();
        item.setId("mem-1");
        item.setTainted(true);
        when(memoryItemRepository.findById("mem-1")).thenReturn(Optional.of(item));
        when(memoryItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MemoryItem result = memoryService.review("mem-1", "reviewer-1");

        assertFalse(result.isTainted());
        assertNotNull(result.getReviewedBy());
        assertNotNull(result.getReviewedAt());
    }

    @Test
    void review_throwsWhenNotFound() {
        when(memoryItemRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            memoryService.review("missing", "reviewer-1");
        });
    }

    @Test
    void findByMember() {
        MemoryItem item = new MemoryItem();
        item.setId("mem-1");
        when(memoryItemRepository.findByMemberId("human-1")).thenReturn(List.of(item));

        List<MemoryItem> result = memoryService.findByMember("human-1");

        assertEquals(1, result.size());
    }

    @Test
    void findByWorkspace() {
        MemoryItem item = new MemoryItem();
        item.setId("mem-1");
        when(memoryItemRepository.findByWorkspaceId("ws-1")).thenReturn(List.of(item));

        List<MemoryItem> result = memoryService.findByWorkspace("ws-1");

        assertEquals(1, result.size());
    }

    @Test
    void findTainted() {
        MemoryItem item = new MemoryItem();
        item.setId("mem-1");
        item.setTainted(true);
        when(memoryItemRepository.findByTaintedTrue()).thenReturn(List.of(item));

        List<MemoryItem> result = memoryService.findTainted();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isTainted());
    }

    @Test
    void countByTier() {
        when(memoryItemRepository.countByTier("short")).thenReturn(5L);

        long count = memoryService.countByTier("short");

        assertEquals(5, count);
    }
}

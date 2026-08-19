package com.summa.service;

import com.summa.repository.GroupRepository;
import com.summa.model.Group;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private GroupService groupService;

    @Test
    void create_group() {
        when(groupRepository.findByNameAndStatusNot("Engineering", "archived")).thenReturn(Optional.empty());
        when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Group result = groupService.create("Engineering", "human-1");

        assertNotNull(result);
        assertEquals("Engineering", result.getName());
        assertEquals("human-1", result.getLeaderMemberId());
    }

    @Test
    void create_throwsOnDuplicateName() {
        Group existing = new Group();
        existing.setName("Engineering");
        when(groupRepository.findByNameAndStatusNot("Engineering", "archived"))
            .thenReturn(Optional.of(existing));

        assertThrows(IllegalArgumentException.class, () -> {
            groupService.create("Engineering", "human-2");
        });
    }

    @Test
    void archive_group() {
        Group group = new Group();
        group.setId("group-1");
        group.setStatus("active");
        when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Group result = groupService.archive("group-1", "admin");

        assertEquals("archived", result.getStatus());
    }

    @Test
    void archive_throwsWhenNotFound() {
        when(groupRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            groupService.archive("missing", "admin");
        });
    }

    @Test
    void setLeader() {
        Group group = new Group();
        group.setId("group-1");
        when(groupRepository.findById("group-1")).thenReturn(Optional.of(group));
        when(groupRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Group result = groupService.setLeader("group-1", "human-2", "admin");

        assertEquals("human-2", result.getLeaderMemberId());
    }
}

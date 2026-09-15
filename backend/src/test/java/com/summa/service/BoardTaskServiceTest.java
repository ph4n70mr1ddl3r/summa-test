package com.summa.service;

import com.summa.repository.BoardTaskRepository;
import com.summa.repository.InitiativeRepository;
import com.summa.model.BoardTask;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.model.Initiative;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class BoardTaskServiceTest {

    @Mock
    private BoardTaskRepository taskRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private MemberService memberService;

    @Mock
    private InitiativeRepository initiativeRepository;

    @InjectMocks
    private BoardTaskService boardTaskService;

    @Test
    void create_underOpenInitiative() {
        Initiative init = new Initiative();
        init.setStatus("active");
        when(initiativeRepository.findById("init-1")).thenReturn(Optional.of(init));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BoardTask result = boardTaskService.create("task-1", "desc", "user-1", "member-1", "init-1", 1, null);

        assertNotNull(result);
        assertEquals("task-1", result.getTitle());
    }

    @Test
    void create_throwsUnderClosedInitiative() {
        Initiative init = new Initiative();
        init.setStatus("closed");
        when(initiativeRepository.findById("init-1")).thenReturn(Optional.of(init));

        assertThrows(IllegalStateException.class, () ->
            boardTaskService.create("task-1", "desc", "user-1", null, "init-1", null, null));
    }

    @Test
    void create_withoutInitiative() {
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BoardTask result = boardTaskService.create("task-1", "desc", "user-1", null, null, null, null);

        assertNotNull(result);
        assertNull(result.getInitiativeId());
    }

    @Test
    void assign_toActiveHuman() {
        BoardTask task = new BoardTask();
        task.setId("t1");
        task.setStatus("open");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        Human human = new Human();
        human.setId("h1");
        human.setRbac("admin");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BoardTask result = boardTaskService.assign("t1", "h1", "admin");

        assertEquals("in_progress", result.getStatus());
        assertEquals("h1", result.getAssigneeMemberId());
    }

    @Test
    void assign_throwsForViewer() {
        BoardTask task = new BoardTask();
        task.setId("t1");
        task.setStatus("open");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        Human human = new Human();
        human.setRbac("viewer");
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));
        when(memberService.isViewer(human)).thenReturn(true);

        assertThrows(IllegalStateException.class, () ->
            boardTaskService.assign("t1", "h1", "admin"));
    }

    @Test
    void assign_throwsForInactiveHuman() {
        BoardTask task = new BoardTask();
        task.setId("t1");
        task.setStatus("open");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        Human human = new Human();
        human.setRbac("admin");
        human.setDeactivatedAt(java.time.Instant.now());
        when(memberService.findHuman("h1")).thenReturn(Optional.of(human));

        assertThrows(IllegalStateException.class, () ->
            boardTaskService.assign("t1", "h1", "admin"));
    }

    @Test
    void assign_throwsWhenAssigneeNotFound() {
        BoardTask task = new BoardTask();
        task.setId("t1");
        task.setStatus("open");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
        when(memberService.findHuman("missing")).thenReturn(Optional.empty());
        when(memberService.findAgent("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            boardTaskService.assign("t1", "missing", "admin"));
    }

    @Test
    void assign_throwsWhenTaskNotOpen() {
        BoardTask task = new BoardTask();
        task.setId("t1");
        task.setStatus("done");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));

        assertThrows(IllegalStateException.class, () ->
            boardTaskService.assign("t1", "h1", "admin"));
    }

    @Test
    void complete_findsAndCompletes() {
        BoardTask task = new BoardTask();
        task.setId("t1");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BoardTask result = boardTaskService.complete("t1", "admin");

        assertEquals("done", result.getStatus());
        assertNotNull(result.getCompletedAt());
    }

    @Test
    void complete_throwsWhenNotFound() {
        when(taskRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            boardTaskService.complete("missing", "admin"));
    }

    @Test
    void unassign_findsAndUnassigns() {
        BoardTask task = new BoardTask();
        task.setId("t1");
        task.setAssigneeMemberId("h1");
        task.setStatus("in_progress");
        when(taskRepository.findById("t1")).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        BoardTask result = boardTaskService.unassign("t1", "admin");

        assertNull(result.getAssigneeMemberId());
        assertEquals("open", result.getStatus());
    }

    @Test
    void unassign_throwsWhenNotFound() {
        when(taskRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
            boardTaskService.unassign("missing", "admin"));
    }
}

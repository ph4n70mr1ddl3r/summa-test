package com.summa.service;

import com.summa.repository.BoardTaskRepository;
import com.summa.model.BoardTask;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BoardTaskService {
    private final BoardTaskRepository taskRepository;
    private final AuditService auditService;

    public BoardTaskService(BoardTaskRepository taskRepository, AuditService auditService) {
        this.taskRepository = taskRepository;
        this.auditService = auditService;
    }

    @Transactional
    public BoardTask create(String title, String description, String createdBy, 
                             String assigneeMemberId, String initiativeId, Integer priority,
                             Instant dueAt) {
        BoardTask task = new BoardTask();
        task.setId(UUID.randomUUID().toString());
        task.setTitle(title);
        task.setDescription(description != null ? description : "");
        task.setCreatedBy(createdBy);
        task.setAssigneeMemberId(assigneeMemberId);
        task.setInitiativeId(initiativeId);
        task.setPriority(priority != null ? priority : 0);
        task.setDueAt(dueAt);

        BoardTask saved = taskRepository.save(task);
        auditService.log(createdBy, "CREATE", "board_task", task.getId(), 
            String.format("{\"title\":\"%s\"}", title));
        return saved;
    }

    public Optional<BoardTask> findById(String id) {
        return taskRepository.findById(id);
    }

    public List<BoardTask> findAll() {
        return taskRepository.findAll();
    }

    public List<BoardTask> findByAssignee(String assigneeMemberId) {
        return taskRepository.findByAssigneeMemberId(assigneeMemberId);
    }

    public List<BoardTask> findByInitiative(String initiativeId) {
        return taskRepository.findByInitiativeId(initiativeId);
    }

    @Transactional
    public BoardTask assign(String id, String assigneeMemberId, String actor) {
        BoardTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        if (!"open".equals(task.getStatus())) {
            throw new IllegalStateException("Task is not open: " + task.getStatus());
        }

        task.setAssigneeMemberId(assigneeMemberId);
        task.setStatus("in_progress");
        BoardTask saved = taskRepository.save(task);
        auditService.log(actor, "ASSIGN", "board_task", id,
            String.format("{\"assignee\":\"%s\"}", assigneeMemberId));
        return saved;
    }

    @Transactional
    public BoardTask complete(String id, String actor) {
        BoardTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        
        task.setStatus("done");
        task.setCompletedAt(Instant.now());
        BoardTask saved = taskRepository.save(task);
        auditService.log(actor, "COMPLETE", "board_task", id, null);
        return saved;
    }

    @Transactional
    public BoardTask unassign(String id, String actor) {
        BoardTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));
        
        task.setAssigneeMemberId(null);
        task.setStatus("open");
        BoardTask saved = taskRepository.save(task);
        auditService.log(actor, "UNASSIGN", "board_task", id, null);
        return saved;
    }
}

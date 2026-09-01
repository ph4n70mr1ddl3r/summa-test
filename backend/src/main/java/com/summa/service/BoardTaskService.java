package com.summa.service;

import com.summa.repository.BoardTaskRepository;
import com.summa.model.BoardTask;
import com.summa.model.Human;
import com.summa.model.Agent;
import com.summa.repository.InitiativeRepository;
import com.summa.model.Initiative;
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
    private final MemberService memberService;
    private final InitiativeRepository initiativeRepository;

    public BoardTaskService(BoardTaskRepository taskRepository, AuditService auditService,
                            MemberService memberService, InitiativeRepository initiativeRepository) {
        this.taskRepository = taskRepository;
        this.auditService = auditService;
        this.memberService = memberService;
        this.initiativeRepository = initiativeRepository;
    }

    @Transactional
    public BoardTask create(String title, String description, String createdBy,
                              String assigneeMemberId, String initiativeId, Integer priority,
                              Instant dueAt) {
        // INT-081: Board tasks join runs and spawns in the closed-slice refusal
        // proposed and paused keep task-filing open as planning; closed refuses
        if (initiativeId != null && !initiativeId.isBlank()) {
            Optional<Initiative> initOpt = initiativeRepository.findById(initiativeId);
            if (initOpt.isPresent()) {
                Initiative init = initOpt.get();
                if ("closed".equals(init.getStatus())) {
                    throw new IllegalStateException(
                        "Cannot create board task under closed initiative: " + initiativeId);
                }
            }
        }

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

    public List<BoardTask> findByStatus(String status) {
        return taskRepository.findByStatus(status);
    }

    /**
     * ORG-031: Tasks are assignable to humans or agents, never viewers, and the assignee
     * must be active at write. Suspension freezes an assignee's tasks (handled by the walk).
     */
    @Transactional
    public BoardTask assign(String id, String assigneeMemberId, String actor) {
        BoardTask task = taskRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + id));

        if (!"open".equals(task.getStatus())) {
            throw new IllegalStateException("Task is not open: " + task.getStatus());
        }

        // ORG-031: Refuse viewer assignees
        Optional<Human> assigneeHuman = memberService.findHuman(assigneeMemberId);
        if (assigneeHuman.isPresent()) {
            if (memberService.isViewer(assigneeHuman.get())) {
                throw new IllegalStateException("Viewers cannot be assigned board tasks");
            }
            if (!assigneeHuman.get().isActive()) {
                throw new IllegalStateException("Non-active members cannot be assigned board tasks");
            }
        } else {
            // Check if it's an agent
            Optional<Agent> assigneeAgent = memberService.findAgent(assigneeMemberId);
            if (assigneeAgent.isPresent()) {
                if (!assigneeAgent.get().isActive()) {
                    throw new IllegalStateException("Suspended/retired agents cannot be assigned board tasks");
                }
            } else {
                throw new IllegalArgumentException("Assignee not found: " + assigneeMemberId);
            }
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


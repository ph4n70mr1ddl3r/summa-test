package com.summa.service;

import com.summa.repository.RunRepository;
import com.summa.model.Run;
import com.summa.repository.InitiativeRepository;
import com.summa.model.Initiative;
import com.summa.repository.AgentRepository;
import com.summa.model.Agent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;

@Service
public class RunService {
    private final RunRepository runRepository;
    private final AuditService auditService;
    private final InitiativeRepository initiativeRepository;
    private final AgentRepository agentRepository;

    public RunService(RunRepository runRepository, AuditService auditService,
                      InitiativeRepository initiativeRepository, AgentRepository agentRepository) {
        this.runRepository = runRepository;
        this.auditService = auditService;
        this.initiativeRepository = initiativeRepository;
        this.agentRepository = agentRepository;
    }

    @Transactional
    public Run create(String agentId, String workspaceId, String initiativeId,
                        String triggerId, String prompt, String actor) {
        // INT-080: Only active initiatives launch runs
        if (initiativeId != null && !initiativeId.isBlank()) {
            Optional<Initiative> initOpt = initiativeRepository.findById(initiativeId);
            if (initOpt.isEmpty()) {
                throw new EntityNotFoundException("Initiative not found: " + initiativeId);
            }
            Initiative init = initOpt.get();
            if (!"active".equals(init.getStatus())) {
                throw new IllegalStateException(
                    "Cannot launch run under non-active initiative: " + initiativeId
                        + " (status: " + init.getStatus() + ")");
            }
        }

        Run run = new Run();
        run.setId(UUID.randomUUID().toString());
        run.setAgentId(agentId);
        run.setWorkspaceId(workspaceId);
        run.setInitiativeId(initiativeId);
        run.setTriggerId(triggerId);
        run.setPrompt(prompt != null ? prompt : "");
        run.setStatus("queued");

        Run saved = runRepository.save(run);
        auditService.log(actor, "CREATE_RUN", "run", saved.getId(),
            String.format("{\"agentId\":%s,\"status\":%s}", JsonHelpers.jsonString(agentId), JsonHelpers.jsonString("queued")));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Run> findById(String id) {
        return runRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public List<Run> findByAgent(String agentId, int limit) {
        return runRepository.findByAgentIdOrderByCreatedAtDesc(agentId, limit);
    }

    @Transactional(readOnly = true)
    public List<Run> findByWorkspace(String workspaceId, int limit) {
        return runRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId, limit);
    }

    @Transactional(readOnly = true)
    public List<Run> findByStatus(String status, int limit) {
        return runRepository.findByStatusOrderByCreatedAtDesc(status, limit);
    }

    @Transactional(readOnly = true)
    public List<Run> findRecent(int limit) {
        return runRepository.findByOrderByCreatedAtDesc(limit);
    }

    @Transactional
    public Run start(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        if (!"queued".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot start run with status: " + run.getStatus());
        }
        run.setStatus("running");
        run.setStartedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("START_RUN", "run", id, null);
        return saved;
    }

    @Transactional
    public Run complete(String id, String result, Long costTokens, Double costUsd) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        if (!"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot complete run with status: " + run.getStatus());
        }
        run.setStatus("completed");
        run.setResult(result);
        run.setCompletedAt(Instant.now());
        if (costTokens != null) run.setCostTokens(costTokens);
        if (costUsd != null) run.setCostUsd(costUsd);
        Run saved = runRepository.save(run);
        auditService.logSystem("COMPLETE_RUN", "run", id, null);
        return saved;
    }

    @Transactional
    public Run fail(String id, String errorMessage) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        if (!"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot fail run with status: " + run.getStatus());
        }
        run.setStatus("failed");
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("FAIL_RUN", "run", id,
            String.format("{\"error\":%s}", JsonHelpers.jsonString(errorMessage != null ? errorMessage.substring(0, Math.min(200, errorMessage.length())) : "")));
        return saved;
    }

    @Transactional
    public Run cancel(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        if (!"queued".equals(run.getStatus()) && !"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot cancel run with status: " + run.getStatus());
        }
        run.setStatus("cancelled");
        run.setCompletedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("CANCEL_RUN", "run", id, null);
        return saved;
    }

    @Transactional
    public Run suspend(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + id));
        if (!"running".equals(run.getStatus())) {
            throw new IllegalStateException("Cannot suspend run with status: " + run.getStatus());
        }
        run.setStatus("suspended");
        Run saved = runRepository.save(run);
        auditService.logSystem("SUSPEND_RUN", "run", id, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Run> findByStatus(String status) {
        return runRepository.findByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countByStatus(String status) {
        return runRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long countByAgent(String agentId) {
        return runRepository.countByAgentId(agentId);
    }

    @Transactional(readOnly = true)
    public boolean agentExists(String agentId) {
        return agentRepository.existsById(agentId);
    }
}

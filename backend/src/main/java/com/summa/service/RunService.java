package com.summa.service;

import com.summa.repository.RunRepository;
import com.summa.model.Run;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RunService {
    private final RunRepository runRepository;
    private final AuditService auditService;

    public RunService(RunRepository runRepository, AuditService auditService) {
        this.runRepository = runRepository;
        this.auditService = auditService;
    }

    @Transactional
    public Run create(String agentId, String workspaceId, String initiativeId,
                       String triggerId, String prompt, String actor) {
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
            String.format("{\"agentId\":\"%s\",\"status\":\"queued\"}", agentId));
        return saved;
    }

    public Optional<Run> findById(String id) {
        return runRepository.findById(id);
    }

    public List<Run> findByAgent(String agentId) {
        return runRepository.findByAgentId(agentId);
    }

    public List<Run> findByWorkspace(String workspaceId) {
        return runRepository.findByWorkspaceId(workspaceId);
    }

    public List<Run> findRunning() {
        return runRepository.findByStatus("running");
    }

    public List<Run> findQueued() {
        return runRepository.findByStatus("queued");
    }

    public List<Run> findRecent(int limit) {
        return runRepository.findByOrderByCreatedAtDesc().stream()
                .limit(limit)
                .toList();
    }

    @Transactional
    public Run start(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + id));
        run.setStatus("running");
        run.setStartedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("START_RUN", "run", id, null);
        return saved;
    }

    @Transactional
    public Run complete(String id, String result, Long costTokens, Double costUsd) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + id));
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
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + id));
        run.setStatus("failed");
        run.setErrorMessage(errorMessage);
        run.setCompletedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("FAIL_RUN", "run", id, 
            String.format("{\"error\":\"%s\"}", errorMessage != null ? errorMessage.substring(0, Math.min(200, errorMessage.length())) : ""));
        return saved;
    }

    @Transactional
    public Run cancel(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + id));
        run.setStatus("cancelled");
        run.setCompletedAt(Instant.now());
        Run saved = runRepository.save(run);
        auditService.logSystem("CANCEL_RUN", "run", id, null);
        return saved;
    }

    @Transactional
    public Run suspend(String id) {
        Run run = runRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + id));
        run.setStatus("suspended");
        Run saved = runRepository.save(run);
        auditService.logSystem("SUSPEND_RUN", "run", id, null);
        return saved;
    }

    public List<Run> findByStatus(String status) {
        return runRepository.findByStatus(status);
    }

    public long countByStatus(String status) {
        return runRepository.countByStatus(status);
    }

    public long countByAgent(String agentId) {
        return runRepository.countByAgentId(agentId);
    }
}

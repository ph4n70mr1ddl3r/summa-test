package com.summa.service;

import com.summa.repository.RoleTemplateRepository;
import com.summa.model.RoleTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoleTemplateService {
    private final RoleTemplateRepository templateRepository;
    private final AuditService auditService;

    public RoleTemplateService(RoleTemplateRepository templateRepository, AuditService auditService) {
        this.templateRepository = templateRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RoleTemplate create(String name, String agentClass, String body, String defaultScopes) {
        RoleTemplate template = new RoleTemplate();
        template.setId(UUID.randomUUID().toString());
        template.setName(name);
        template.setAgentClass(agentClass);
        template.setBody(body != null ? body : "{}");
        template.setDefaultScopes(defaultScopes != null ? defaultScopes : "{}");
        template.setStatus("draft");

        RoleTemplate saved = templateRepository.save(template);
        auditService.log("system", "CREATE", "role_template", template.getId(),
            String.format("{\"name\":\"%s\",\"class\":\"%s\"}", name, agentClass));
        return saved;
    }

    public Optional<RoleTemplate> findById(String id) {
        return templateRepository.findById(id);
    }

    public List<RoleTemplate> findAll() {
        return templateRepository.findAll();
    }

    @Transactional
    public RoleTemplate publish(String id, String actor) {
        RoleTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        template.setStatus("active");
        RoleTemplate saved = templateRepository.save(template);
        auditService.log(actor, "PUBLISH", "role_template", id, null);
        return saved;
    }

    @Transactional
    public RoleTemplate retire(String id, String actor) {
        RoleTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Template not found: " + id));

        template.setStatus("retired");
        RoleTemplate saved = templateRepository.save(template);
        auditService.log(actor, "RETIRE", "role_template", id, null);
        return saved;
    }
}

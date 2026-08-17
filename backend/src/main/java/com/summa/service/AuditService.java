package com.summa.service;

import com.summa.repository.AuditEventRepository;
import com.summa.model.AuditEvent;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public AuditEvent log(String actor, String action, String objectType, String objectId, String detail) {
        AuditEvent event = new AuditEvent();
        event.setId(UUID.randomUUID().toString());
        event.setActor(actor);
        event.setAction(action);
        event.setObjectType(objectType);
        event.setObjectId(objectId);
        event.setDetail(detail != null ? detail : "{}");
        event.setOrigin("live");
        return auditEventRepository.save(event);
    }

    public AuditEvent logSystem(String action, String objectType, String objectId, String detail) {
        return log("system", action, objectType, objectId, detail);
    }

    public AuditEvent logWithNode(String actor, String action, String objectType, String objectId, 
                                   String nodeId, String detail) {
        AuditEvent event = log(actor, action, objectType, objectId, detail);
        event.setNodeId(nodeId);
        return auditEventRepository.save(event);
    }
}

package com.summa.controller;

import com.summa.service.GovernanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/governance")
public class GovernanceController {
    private final GovernanceService governanceService;

    public GovernanceController(GovernanceService governanceService) {
        this.governanceService = governanceService;
    }

    @GetMapping("/policies")
    public ResponseEntity<Map<String, Object>> getPolicies() {
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @GetMapping("/quotas")
    public ResponseEntity<Map<String, Object>> getQuotas() {
        Map<String, Object> all = governanceService.getAllSettings();
        Map<String, Object> quotas = new java.util.HashMap<>();
        quotas.put("spawn.ephemeral.default_ttl_hours", all.get("spawn.ephemeral.default_ttl_hours"));
        quotas.put("spawn.ephemeral.max_concurrent_per_spawner", all.get("spawn.ephemeral.max_concurrent_per_spawner"));
        quotas.put("spawn.org_wide_max_active_agents", all.get("spawn.org_wide_max_active_agents"));
        quotas.put("spawn.depth_cap", all.get("spawn.depth_cap"));
        quotas.put("asks.tier_critical_deadline_hours", all.get("asks.tier_critical_deadline_hours"));
        quotas.put("asks.tier_bulk_deadline_hours", all.get("asks.tier_bulk_deadline_hours"));
        quotas.put("asks.storm_collapse_window_hours", all.get("asks.storm_collapse_window_hours"));
        quotas.put("asks.rate_limit_per_source_per_hour", all.get("asks.rate_limit_per_source_per_hour"));
        return ResponseEntity.ok(quotas);
    }

    @GetMapping("/spend")
    public ResponseEntity<Map<String, Object>> getSpend() {
        // Simplified spend view
        return ResponseEntity.ok(Map.of(
            "orgCeiling", governanceService.getSetting("spend.org_ceiling"),
            "criticalFloorPercent", governanceService.getSetting("spend.critical_floor_percent"),
            "halted", governanceService.isSpendHaltTripped()
        ));
    }

    @PutMapping("/policies")
    public ResponseEntity<?> updatePolicy(@RequestBody Map<String, Object> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        body.forEach((key, value) -> governanceService.setSetting(key, value));
        return ResponseEntity.ok(governanceService.getAllSettings());
    }

    @PutMapping("/quotas")
    public ResponseEntity<?> updateQuotas(@RequestBody Map<String, Object> body,
                                           @RequestHeader(value = "X-Actor", defaultValue = "system") String actor) {
        body.forEach((key, value) -> governanceService.setSetting(key, value));
        return ResponseEntity.ok(governanceService.getAllSettings());
    }
}

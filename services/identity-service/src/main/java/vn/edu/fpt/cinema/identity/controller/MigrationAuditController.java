package vn.edu.fpt.cinema.identity.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.cinema.identity.application.AccountMigrationService;

@RestController
@RequestMapping("/api/v1/admin/migration")
public class MigrationAuditController {

    private final AccountMigrationService migrationService;

    public MigrationAuditController(AccountMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @GetMapping("/audit")
    public ResponseEntity<Map<String, Object>> getAuditStatus() {
        return ResponseEntity.ok(migrationService.auditMigrationStatus());
    }

    @PostMapping("/reconcile")
    public ResponseEntity<Map<String, Object>> triggerReconcile() {
        migrationService.reconcileCustomerAccounts();
        return ResponseEntity.ok(migrationService.auditMigrationStatus());
    }
}

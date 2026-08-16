package com.example;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.model.ModelCallAuditRecord;
import com.example.service.ModelCallAuditService;

@RestController
@RequestMapping("/api")
public class ModelCallAuditController {
    private final ModelCallAuditService auditService;

    public ModelCallAuditController(ModelCallAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/model-call-audits")
    public List<ModelCallAuditRecord> getModelCallAudits() {
        return auditService.findAll();
    }
}

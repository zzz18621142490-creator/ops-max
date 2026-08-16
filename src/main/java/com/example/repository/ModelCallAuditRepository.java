package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.ModelCallAuditEntity;

public interface ModelCallAuditRepository extends JpaRepository<ModelCallAuditEntity, String> {
    List<ModelCallAuditEntity> findAllByOrderByCreatedAtDesc();
}

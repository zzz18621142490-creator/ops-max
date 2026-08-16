package com.example.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.entity.AnalysisHistoryEntity;

public interface AnalysisHistoryRepository extends JpaRepository<AnalysisHistoryEntity, String> {
    List<AnalysisHistoryEntity> findAllByOrderByCreatedAtDesc();
}

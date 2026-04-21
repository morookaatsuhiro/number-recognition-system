package com.example.demoapp;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingConfigRepository extends JpaRepository<TrainingConfig, Long> {
    TrainingConfig findBySaveModel(String saveModel); // 根据模型名称查找记录
}

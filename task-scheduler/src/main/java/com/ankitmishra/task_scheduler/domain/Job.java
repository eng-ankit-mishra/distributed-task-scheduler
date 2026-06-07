package com.ankitmishra.task_scheduler.domain;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="jobs")
@Data
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(name = "cron_expression",nullable = false)
    private String cronExpression;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status=JobStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "retry_count",nullable = false)
    private int retryCount;

    @Column(name = "next_run_at",nullable = false)
    private LocalDateTime nextRunAt;

    @Column(name="created_at",updatable = false)
    private LocalDateTime createdAt=LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt=LocalDateTime.now();

    @PreUpdate
    public void onUpdate(){
        this.updatedAt=LocalDateTime.now();
    }

}

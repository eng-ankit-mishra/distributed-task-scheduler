package com.ankitmishra.task_scheduler.messaging;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JobMessage {

    private UUID jobId;
    private String jobName;
    private String payload;
    private int retryCount;


}

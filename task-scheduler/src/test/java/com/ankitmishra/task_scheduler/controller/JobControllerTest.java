package com.ankitmishra.task_scheduler.controller;

import com.ankitmishra.task_scheduler.domain.Job;
import com.ankitmishra.task_scheduler.domain.JobStatus;
import com.ankitmishra.task_scheduler.repository.JobRepository;
import com.ankitmishra.task_scheduler.security.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JobControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JobRepository jobRepository;
    @Autowired private JwtService jwtService;
    @Autowired private ObjectMapper objectMapper;

    private String adminToken;
    private String viewerToken;

    @BeforeEach
    void setUp() {
        jobRepository.deleteAll();
        adminToken  = "Bearer " + jwtService.generateToken("admin",  "ROLE_ADMIN");
        viewerToken = "Bearer " + jwtService.generateToken("viewer", "ROLE_VIEWER");
    }

    @Test
    void createJob_withAdminToken_shouldReturn201() throws Exception {
        Map<String, String> body = Map.of(
                "name", "test-job",
                "cronExpression", "*/30 * * * * *",
                "payload", "{\"action\":\"test\"}"
        );

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("test-job"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void createJob_withViewerToken_shouldReturn403() throws Exception {
        Map<String, String> body = Map.of(
                "name", "test-job",
                "cronExpression", "*/30 * * * * *"
        );

        mockMvc.perform(post("/api/jobs")
                        .header("Authorization", viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllJobs_withValidToken_shouldReturn200() throws Exception {
        saveTestJob("job-1");
        saveTestJob("job-2");

        mockMvc.perform(get("/api/jobs")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllJobs_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(get("/api/jobs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void triggerJob_withAdmin_shouldUpdateNextRunAtToNow() throws Exception {
        Job job = saveTestJob("trigger-test");

        mockMvc.perform(post("/api/jobs/" + job.getId() + "/trigger")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void deleteJob_withAdmin_shouldReturn204() throws Exception {
        Job job = saveTestJob("delete-me");

        mockMvc.perform(delete("/api/jobs/" + job.getId())
                        .header("Authorization", adminToken))
                .andExpect(status().isNoContent());
    }

    private Job saveTestJob(String name) {
        Job job = new Job();
        job.setName(name);
        job.setCronExpression("*/10 * * * * *");
        job.setStatus(JobStatus.PENDING);
        job.setPayload("{}");
        job.setNextRunAt(LocalDateTime.now().plusMinutes(1));
        return jobRepository.save(job);
    }
}
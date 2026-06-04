package com.zephyr.deployments_data_srv.controller;

import com.zephyr.deployments_data_srv.exception.GlobalExceptionHandler;
import com.zephyr.deployments_data_srv.exception.ResourceNotFoundException;
import com.zephyr.deployments_data_srv.model.Deployment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentEnvironment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentStatus;
import com.zephyr.deployments_data_srv.service.interfaces.DeploymentService;
import com.zephyr.deployments_data_srv.service.interfaces.DeploymentMetricsService;
import com.zephyr.deployments_data_srv.GetMetrics200ResponseAllOfContentInner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class DeploymentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeploymentService service;

    @Mock
    private DeploymentMetricsService metricsService;

    @InjectMocks
    private DeploymentController controller;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    public void getDeploymentById_Success() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-30T14:32:00Z");
        Deployment deployment = Deployment.builder()
                .id("deploy_123")
                .service("billing-api")
                .environment(DeploymentEnvironment.PRODUCTION)
                .status(DeploymentStatus.SUCCESS)
                .duration(320)
                .timestamp(now)
                .commitSha("abc123")
                .deployedBy("github-actions[bot]")
                .build();

        when(service.getDeploymentById("deploy_123")).thenReturn(deployment);

        mockMvc.perform(get("/deployments/deploy_123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value("deploy_123"))
                .andExpect(jsonPath("$.service").value("billing-api"))
                .andExpect(jsonPath("$.environment").value("production"))
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.duration").value(320))
                .andExpect(jsonPath("$.timestamp").value("2026-05-30T14:32:00Z"));
    }

    @Test
    public void getDeploymentById_NotFound_Returns404() throws Exception {
        when(service.getDeploymentById("deploy_999"))
                .thenThrow(new ResourceNotFoundException("Deployment event with ID 'deploy_999' could not be found."));

        mockMvc.perform(get("/deployments/deploy_999")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource Not Found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Deployment event with ID 'deploy_999' could not be found."))
                .andExpect(jsonPath("$.instance").value("/deployments/deploy_999"));
    }

    @Test
    public void listDeployments_Success() throws Exception {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-30T14:32:00Z");
        Deployment deployment = Deployment.builder()
                .id("deploy_123")
                .service("billing-api")
                .environment(DeploymentEnvironment.PRODUCTION)
                .status(DeploymentStatus.SUCCESS)
                .duration(320)
                .timestamp(now)
                .commitSha("abc123")
                .deployedBy("github-actions[bot]")
                .build();

        PageImpl<Deployment> mockPage = new PageImpl<>(Collections.singletonList(deployment));
        when(service.getDeployments(eq("billing-api"), eq("success"), eq("production"), anyInt(), anyInt()))
                .thenReturn(mockPage);

        mockMvc.perform(get("/deployments")
                        .param("service", "billing-api")
                        .param("status", "success")
                        .param("environment", "production")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("deploy_123"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    public void listDeployments_InvalidEnum_Returns400() throws Exception {
        when(service.getDeployments(any(), eq("invalid_status"), any(), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Unknown status value: invalid_status"));

        mockMvc.perform(get("/deployments")
                        .param("status", "invalid_status")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Unknown status value: invalid_status"));
    }

    @Test
    public void getMetrics_Success() throws Exception {
        GetMetrics200ResponseAllOfContentInner inner = new GetMetrics200ResponseAllOfContentInner();
        inner.setService("billing-api");
        inner.setEnvironment("production");
        inner.setDeploymentFrequencyPerHour(0.05);
        inner.setFailureRate(0.12);
        inner.setP95DeploymentTime(320.0);

        org.springframework.data.domain.Page<GetMetrics200ResponseAllOfContentInner> mockPage = 
                new org.springframework.data.domain.PageImpl<>(Collections.singletonList(inner));

        when(metricsService.getServiceMetrics(any(), any(), eq("1d"), anyInt(), anyInt()))
                .thenReturn(mockPage);

        mockMvc.perform(get("/metrics")
                        .param("environment", "production")
                        .param("service", "billing-api")
                        .param("timeRange", "1d")
                        .param("page", "0")
                        .param("size", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].service").value("billing-api"))
                .andExpect(jsonPath("$.content[0].environment").value("production"))
                .andExpect(jsonPath("$.content[0].deploymentFrequencyPerHour").value(0.05))
                .andExpect(jsonPath("$.content[0].failureRate").value(0.12))
                .andExpect(jsonPath("$.content[0].p95DeploymentTime").value(320.0));
    }

    @Test
    public void getMetrics_InvalidRange_Returns400() throws Exception {
        when(metricsService.getServiceMetrics(any(), any(), eq("13m"), anyInt(), anyInt()))
                .thenThrow(new IllegalArgumentException("Maximum time range is 12 months"));

        mockMvc.perform(get("/metrics")
                        .param("timeRange", "13m")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Maximum time range is 12 months"));
    }
}

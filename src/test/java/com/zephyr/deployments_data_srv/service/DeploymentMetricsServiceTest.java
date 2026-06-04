package com.zephyr.deployments_data_srv.service;

import com.zephyr.deployments_data_srv.GetMetrics200ResponseAllOfContentInner;
import com.zephyr.deployments_data_srv.model.Deployment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentEnvironment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentStatus;
import com.zephyr.deployments_data_srv.repository.interfaces.DeploymentRepository;
import com.zephyr.deployments_data_srv.service.implementations.DeploymentMetricsServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeploymentMetricsServiceTest {

    @Mock
    private DeploymentRepository repository;

    @InjectMocks
    private DeploymentMetricsServiceImpl metricsService;

    @Test
    public void getServiceMetrics_ValidRanges_Success() {
        when(repository.findAll(any(Specification.class))).thenReturn(Collections.emptyList());

        // Min boundary
        assertNotNull(metricsService.getServiceMetrics(null, null, "1h", 0, 10));
        // Standard default day
        assertNotNull(metricsService.getServiceMetrics(null, null, "1d", 0, 10));
        // Max day boundary
        assertNotNull(metricsService.getServiceMetrics(null, null, "360d", 0, 10));
        // Max month boundary
        assertNotNull(metricsService.getServiceMetrics(null, null, "12m", 0, 10));

        verify(repository, times(4)).findAll(any(Specification.class));
    }

    @Test
    public void getServiceMetrics_InvalidRanges_ThrowsException() {
        // Invalid values (exceed boundary limits)
        assertThrows(IllegalArgumentException.class, () -> metricsService.getServiceMetrics(null, null, "361d", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> metricsService.getServiceMetrics(null, null, "13m", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> metricsService.getServiceMetrics(null, null, "", 0, 10));
        assertThrows(IllegalArgumentException.class, () -> metricsService.getServiceMetrics(null, null, null, 0, 10));

        verifyNoInteractions(repository);
    }

    @Test
    public void getServiceMetrics_Calculations_Success() {
        OffsetDateTime now = OffsetDateTime.now();
        List<Deployment> mockDeployments = Arrays.asList(
                Deployment.builder()
                        .id("dep_1")
                        .service("auth-service")
                        .environment(DeploymentEnvironment.PRODUCTION)
                        .status(DeploymentStatus.SUCCESS)
                        .duration(100)
                        .timestamp(now.minusHours(2))
                        .commitSha("sha1")
                        .deployedBy("user")
                        .build(),
                Deployment.builder()
                        .id("dep_2")
                        .service("auth-service")
                        .environment(DeploymentEnvironment.PRODUCTION)
                        .status(DeploymentStatus.FAILED)
                        .duration(200)
                        .timestamp(now.minusHours(4))
                        .commitSha("sha2")
                        .deployedBy("user")
                        .build(),
                Deployment.builder()
                        .id("dep_3")
                        .service("auth-service")
                        .environment(DeploymentEnvironment.PRODUCTION)
                        .status(DeploymentStatus.SUCCESS)
                        .duration(300)
                        .timestamp(now.minusHours(6))
                        .commitSha("sha3")
                        .deployedBy("user")
                        .build()
        );

        when(repository.findAll(any(Specification.class))).thenReturn(mockDeployments);

        // Fetch metrics for 1d (24 hours)
        Page<GetMetrics200ResponseAllOfContentInner> result = metricsService.getServiceMetrics(
                Collections.singletonList("production"), "auth-service", "1d", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());

        GetMetrics200ResponseAllOfContentInner metrics = result.getContent().get(0);
        assertEquals("auth-service", metrics.getService());
        assertEquals("production", metrics.getEnvironment());
        
        // 3 deployments in 24 hours = 0.125 frequency
        assertEquals(0.125, metrics.getDeploymentFrequencyPerHour(), 0.0001);
        
        // 1 failed out of 3 = 0.3333 failure rate
        assertEquals(1.0 / 3.0, metrics.getFailureRate(), 0.0001);
        
        // Sorted durations: [100, 200, 300]. p95 index is ceil(0.95 * 3) - 1 = 2 -> duration 300
        assertEquals(300.0, metrics.getP95DeploymentTime(), 0.0001);
    }
}

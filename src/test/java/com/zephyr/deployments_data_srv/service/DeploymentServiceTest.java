package com.zephyr.deployments_data_srv.service;

import com.zephyr.deployments_data_srv.exception.ResourceNotFoundException;
import com.zephyr.deployments_data_srv.model.Deployment;
import com.zephyr.deployments_data_srv.model.DeploymentEnvironment;
import com.zephyr.deployments_data_srv.model.DeploymentStatus;
import com.zephyr.deployments_data_srv.repository.DeploymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DeploymentServiceTest {

    @Mock
    private DeploymentRepository repository;

    @InjectMocks
    private DeploymentServiceImpl service;

    private Deployment mockDeployment;

    @BeforeEach
    public void setUp() {
        mockDeployment = Deployment.builder()
                .id("deploy_001")
                .service("billing-api")
                .environment(DeploymentEnvironment.PRODUCTION)
                .status(DeploymentStatus.SUCCESS)
                .duration(120)
                .timestamp(OffsetDateTime.now())
                .commitSha("abc123")
                .deployedBy("github-actions[bot]")
                .build();
    }

    @Test
    public void getDeploymentById_Success() {
        when(repository.findById("deploy_001")).thenReturn(Optional.of(mockDeployment));

        Deployment result = service.getDeploymentById("deploy_001");

        assertNotNull(result);
        assertEquals("deploy_001", result.getId());
        assertEquals("billing-api", result.getService());
        assertEquals(DeploymentStatus.SUCCESS, result.getStatus());
        verify(repository, times(1)).findById("deploy_001");
    }

    @Test
    public void getDeploymentById_NotFound_ThrowsException() {
        when(repository.findById("deploy_999")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            service.getDeploymentById("deploy_999");
        });
        verify(repository, times(1)).findById("deploy_999");
    }

    @Test
    public void getDeployments_ValidFilters_Success() {
        Page<Deployment> mockPage = new PageImpl<>(Collections.singletonList(mockDeployment));

        when(repository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(mockPage);

        Page<Deployment> result = service.getDeployments("billing-api", "success", "production", 0, 10);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("deploy_001", result.getContent().get(0).getId());
        verify(repository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    public void getDeployments_InvalidStatus_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            service.getDeployments("billing-api", "invalid_status", "production", 0, 10);
        });
        verifyNoInteractions(repository);
    }
}
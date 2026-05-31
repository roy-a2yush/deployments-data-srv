package com.zephyr.deployments_data_srv.service;

import com.zephyr.deployments_data_srv.exception.ResourceNotFoundException;
import com.zephyr.deployments_data_srv.model.Deployment;
import com.zephyr.deployments_data_srv.model.DeploymentEnvironment;
import com.zephyr.deployments_data_srv.model.DeploymentStatus;
import com.zephyr.deployments_data_srv.repository.DeploymentRepository;
import com.zephyr.deployments_data_srv.repository.DeploymentSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for managing {@link Deployment} entities.
 * Fully decoupled from custom tracing boilerplate; tracing and MDC are handled
 * globally at the Interceptor level.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
public class DeploymentServiceImpl implements DeploymentService {

    private final DeploymentRepository repository;

    public DeploymentServiceImpl(DeploymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<Deployment> getDeployments(String service, String status, String environment, int page, int size) {
        log.info("Processing getDeployments. Service: {}, Status: {}, Env: {}, Page: {}, Size: {}", 
                service, status, environment, page, size);

        // Enforce boundary checks on pagination parameters
        int validatedPage = Math.max(0, page);
        int validatedSize = Math.max(1, Math.min(100, size)); // Max page size is 100

        // Parse optional string parameters to strong Enums
        DeploymentStatus parsedStatus = null;
        if (status != null && !status.trim().isEmpty()) {
            parsedStatus = DeploymentStatus.fromValue(status);
        }

        DeploymentEnvironment parsedEnvironment = null;
        if (environment != null && !environment.trim().isEmpty()) {
            parsedEnvironment = DeploymentEnvironment.fromValue(environment);
        }

        log.debug("Building dynamic Specification with filters. parsedStatus: {}, parsedEnvironment: {}", 
                parsedStatus, parsedEnvironment);

        // Sort by timestamp descending so the most recent deployments appear first
        Pageable pageable = PageRequest.of(validatedPage, validatedSize, Sort.by("timestamp").descending());

        Specification<Deployment> spec = DeploymentSpecification.builder()
                .withService(service)
                .withStatus(parsedStatus)
                .withEnvironment(parsedEnvironment)
                .build();

        log.debug("Executing dynamic repository findAll specification query.");
        Page<Deployment> results = repository.findAll(spec, pageable);

        log.info("Successfully fetched deployments page. Elements: {}, Pages: {}", 
                results.getTotalElements(), results.getTotalPages());

        return results;
    }

    @Override
    public Deployment getDeploymentById(String id) {
        log.info("Processing getDeploymentById. ID: {}", id);

        log.debug("Executing repository lookup for ID: {}", id);
        Deployment result = repository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Lookup failed. Deployment not found for ID: {}", id);
                    return new ResourceNotFoundException("Deployment event with ID '" + id + "' could not be found.");
                });

        log.info("Successfully retrieved deployment details. ID: {}, Service: {}", id, result.getService());

        return result;
    }
}

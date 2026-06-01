package com.zephyr.deployments_data_srv.service.interfaces;

import com.zephyr.deployments_data_srv.model.Deployment;
import org.springframework.data.domain.Page;

public interface DeploymentService {
    
    Page<Deployment> getDeployments(String service, String status, String environment, int page, int size);
    
    Deployment getDeploymentById(String id);
}

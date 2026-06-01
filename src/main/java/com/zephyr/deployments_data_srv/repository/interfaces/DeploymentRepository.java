package com.zephyr.deployments_data_srv.repository.interfaces;

import com.zephyr.deployments_data_srv.model.Deployment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DeploymentRepository extends JpaRepository<Deployment, String>, JpaSpecificationExecutor<Deployment> {
}

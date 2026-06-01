package com.zephyr.deployments_data_srv.controller;

import com.zephyr.deployments_data_srv.DeploymentsApi;
import com.zephyr.deployments_data_srv.ListDeployments200Response;
import com.zephyr.deployments_data_srv.ListDeployments200ResponseAllOfContentInner;
import com.zephyr.deployments_data_srv.model.Deployment;
import com.zephyr.deployments_data_srv.service.interfaces.DeploymentService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
public class DeploymentController implements DeploymentsApi {

    private final DeploymentService service;

    public DeploymentController(DeploymentService service) {
        this.service = service;
    }

    @Override
    public ResponseEntity<ListDeployments200Response> listDeployments(
            String serviceName,
            String status,
            String environment,
            Integer page,
            Integer size) {
        
        Page<Deployment> deploymentPage = service.getDeployments(serviceName, status, environment, page, size);

        List<ListDeployments200ResponseAllOfContentInner> dtoList = deploymentPage.getContent()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

        ListDeployments200Response response = new ListDeployments200Response();
        response.setContent(dtoList);
        response.setTotalElements((int) deploymentPage.getTotalElements());
        response.setTotalPages(deploymentPage.getTotalPages());
        response.setSize(deploymentPage.getSize());
        response.setNumber(deploymentPage.getNumber());

        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ListDeployments200ResponseAllOfContentInner> getDeployment(String id) {
        Deployment deployment = service.getDeploymentById(id);
        return ResponseEntity.ok(mapToDto(deployment));
    }

    private ListDeployments200ResponseAllOfContentInner mapToDto(Deployment entity) {
        ListDeployments200ResponseAllOfContentInner dto = new ListDeployments200ResponseAllOfContentInner();
        dto.setId(entity.getId());
        dto.setService(entity.getService());
        dto.setEnvironment(ListDeployments200ResponseAllOfContentInner.EnvironmentEnum.fromValue(entity.getEnvironment().getValue()));
        dto.setStatus(ListDeployments200ResponseAllOfContentInner.StatusEnum.fromValue(entity.getStatus().getValue()));
        dto.setDuration(entity.getDuration());
        dto.setTimestamp(entity.getTimestamp());
        dto.setCommitSha(entity.getCommitSha());
        dto.setPreviousCommitSha(entity.getPreviousCommitSha());
        dto.setDeployedBy(entity.getDeployedBy());
        dto.setFailureReason(entity.getFailureReason());
        dto.setRollbackOf(entity.getRollbackOf());
        return dto;
    }
}

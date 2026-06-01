package com.zephyr.deployments_data_srv.model;

import com.zephyr.deployments_data_srv.model.enums.DeploymentEnvironment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "deployments", indexes = {
    @Index(name = "idx_service_env_status", columnList = "service, environment, status"),
    @Index(name = "idx_timestamp", columnList = "timestamp")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Deployment {

    @Id
    @Column(length = 50, nullable = false)
    private String id;

    @Column(length = 100, nullable = false)
    private String service;

    @Column(length = 50, nullable = false)
    private DeploymentEnvironment environment;

    @Column(length = 50, nullable = false)
    private DeploymentStatus status;

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false, columnDefinition = "DATETIME")
    private OffsetDateTime timestamp;

    @Column(name = "commit_sha", length = 50, nullable = false)
    private String commitSha;

    @Column(name = "previous_commit_sha", length = 50)
    private String previousCommitSha;

    @Column(name = "deployed_by", length = 100, nullable = false)
    private String deployedBy;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "rollback_of", length = 50)
    private String rollbackOf;
}

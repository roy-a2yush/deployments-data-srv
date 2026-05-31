package com.zephyr.deployments_data_srv.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeploymentStatus {
    SUCCESS("success"),
    FAILED("failed"),
    ROLLED_BACK("rolled_back"),
    IN_PROGRESS("in_progress"),
    CANCELLED("cancelled");

    private final String value;

    public static DeploymentStatus fromValue(String value) {
        for (DeploymentStatus status : DeploymentStatus.values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status value: " + value);
    }
}

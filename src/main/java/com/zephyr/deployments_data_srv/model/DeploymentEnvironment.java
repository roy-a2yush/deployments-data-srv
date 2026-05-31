package com.zephyr.deployments_data_srv.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeploymentEnvironment {
    PRODUCTION("production"),
    STAGING("staging"),
    CANARY("canary");

    private final String value;

    public static DeploymentEnvironment fromValue(String value) {
        for (DeploymentEnvironment env : DeploymentEnvironment.values()) {
            if (env.value.equalsIgnoreCase(value)) {
                return env;
            }
        }
        throw new IllegalArgumentException("Unknown environment value: " + value);
    }
}

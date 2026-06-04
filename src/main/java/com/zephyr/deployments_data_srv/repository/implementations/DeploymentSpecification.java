package com.zephyr.deployments_data_srv.repository.implementations;

import com.zephyr.deployments_data_srv.model.Deployment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentEnvironment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentStatus;
import org.springframework.data.jpa.domain.Specification;
import java.time.OffsetDateTime;
import java.util.Collection;

/**
 * Specifications for querying {@link Deployment} entities dynamically.
 * Provides granular specifications and a fluent builder pattern to allow clean 
 * specification composition in the service layer.
 */
public final class DeploymentSpecification {

    private DeploymentSpecification() {
        // Private constructor to prevent instantiation
    }

    /**
     * Creates a specification to filter by service name.
     */
    public static Specification<Deployment> withService(String service) {
        return (root, query, cb) -> {
            if (service == null || service.trim().isEmpty()) {
                return cb.conjunction();
            }
            return cb.equal(root.get("service"), service);
        };
    }

    /**
     * Creates a specification to filter by deployment status.
     */
    public static Specification<Deployment> withStatus(DeploymentStatus status) {
        return (root, query, cb) -> {
            if (status == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("status"), status);
        };
    }


    /**
     * Creates a specification to filter by target environment.
     */
    public static Specification<Deployment> withEnvironment(DeploymentEnvironment environment) {
        return (root, query, cb) -> {
            if (environment == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("environment"), environment);
        };
    }

    /**
     * Creates a specification to filter by multiple environments.
     */
    public static Specification<Deployment> withEnvironments(Collection<DeploymentEnvironment> environments) {
        return (root, query, cb) -> {
            if (environments == null || environments.isEmpty()) {
                return cb.conjunction();
            }
            return root.get("environment").in(environments);
        };
    }

    /**
     * Creates a specification to filter by timestamp boundary.
     */
    public static Specification<Deployment> withTimestampAfter(OffsetDateTime timestamp) {
        return (root, query, cb) -> {
            if (timestamp == null) {
                return cb.conjunction();
            }
            return cb.greaterThanOrEqualTo(root.get("timestamp"), timestamp);
        };
    }

    /**
     * Creates a composite specification containing all filter options.
     */
    public static Specification<Deployment> withFilters(
            String service,
            DeploymentStatus status,
            DeploymentEnvironment environment) {
        return builder()
                .withService(service)
                .withStatus(status)
                .withEnvironment(environment)
                .build();
    }

    /**
     * Initializes a new fluent {@link Builder} to construct a query.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Fluent Builder to construct dynamic Specifications dynamically in the service layer.
     */
    public static class Builder {
        private Specification<Deployment> spec = null;

        private void append(Specification<Deployment> other) {
            if (other != null) {
                spec = (spec == null) ? Specification.where(other) : spec.and(other);
            }
        }

        /**
         * Add filter by service name.
         */
        public Builder withService(String service) {
            if (service != null && !service.trim().isEmpty()) {
                append(DeploymentSpecification.withService(service));
            }
            return this;
        }

        /**
         * Add filter by status.
         */
        public Builder withStatus(DeploymentStatus status) {
            if (status != null) {
                append(DeploymentSpecification.withStatus(status));
            }
            return this;
        }

        /**
         * Add filter by environment.
         */
        public Builder withEnvironment(DeploymentEnvironment environment) {
            if (environment != null) {
                append(DeploymentSpecification.withEnvironment(environment));
            }
            return this;
        }

        /**
         * Add filter by multiple environments.
         */
        public Builder withEnvironments(Collection<DeploymentEnvironment> environments) {
            if (environments != null && !environments.isEmpty()) {
                append(DeploymentSpecification.withEnvironments(environments));
            }
            return this;
        }

        /**
         * Add filter by timestamp boundary.
         */
        public Builder withTimestampAfter(OffsetDateTime timestamp) {
            if (timestamp != null) {
                append(DeploymentSpecification.withTimestampAfter(timestamp));
            }
            return this;
        }

        /**
         * Conditionally appends another specification to the chain if the condition evaluates to true.
         */
        public Builder optionalAnd(boolean condition, Specification<Deployment> optionalSpec) {
            if (condition && optionalSpec != null) {
                append(optionalSpec);
            }
            return this;
        }

        /**
         * Appends another specification directly.
         */
        public Builder and(Specification<Deployment> otherSpec) {
            if (otherSpec != null) {
                append(otherSpec);
            }
            return this;
        }

        /**
         * Returns the compiled {@link Specification}, or a no-op conjunction if empty.
         */
        public Specification<Deployment> build() {
            return spec != null ? spec : (root, query, cb) -> cb.conjunction();
        }
    }
}

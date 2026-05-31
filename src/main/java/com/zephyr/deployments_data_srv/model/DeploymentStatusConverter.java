package com.zephyr.deployments_data_srv.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Attribute Converter to seamlessly map {@link DeploymentStatus} enums 
 * to lowercase database values and back. Ensures uppercase Java conventions are kept
 * while preserving lowercase database seeding and OpenAPI compliance.
 */
@Converter(autoApply = true)
public class DeploymentStatusConverter implements AttributeConverter<DeploymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(DeploymentStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public DeploymentStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return DeploymentStatus.fromValue(dbData);
    }
}

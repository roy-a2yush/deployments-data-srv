package com.zephyr.deployments_data_srv.model.enums.converters;

import com.zephyr.deployments_data_srv.model.enums.DeploymentEnvironment;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA Attribute Converter to seamlessly map {@link DeploymentEnvironment} enums
 * to lowercase database values and back. Ensures uppercase Java conventions are kept
 * while preserving lowercase database seeding and OpenAPI compliance.
 */
@Converter(autoApply = true)
public class DeploymentEnvironmentConverter implements AttributeConverter<DeploymentEnvironment, String> {

    @Override
    public String convertToDatabaseColumn(DeploymentEnvironment attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public DeploymentEnvironment convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return DeploymentEnvironment.fromValue(dbData);
    }
}

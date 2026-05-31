package com.zephyr.deployments_data_srv.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class DatabaseConfig {
    // Note: MySQL DataSource, JPA Entity Manager, and Transaction Managers
    // are automatically set up by Spring Boot using spring.datasource.* properties
    // defined in application.yml.
    //
    // This class serves to enable explicit declarative Transaction Management (@Transactional)
    // and as an extension point for future custom database configurations 
    // (e.g. Auditing, custom naming strategies, or multiple datasources).
}

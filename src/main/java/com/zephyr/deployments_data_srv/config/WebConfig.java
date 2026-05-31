package com.zephyr.deployments_data_srv.config;

import com.zephyr.deployments_data_srv.interceptor.TelemetryInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration class to register the custom telemetry logging interceptor.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TelemetryInterceptor telemetryInterceptor;

    public WebConfig(TelemetryInterceptor telemetryInterceptor) {
        this.telemetryInterceptor = telemetryInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(telemetryInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/health"); // Exclude trivial endpoints if desired, or keep to track
    }
}

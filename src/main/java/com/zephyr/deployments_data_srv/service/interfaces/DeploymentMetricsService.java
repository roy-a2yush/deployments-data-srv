package com.zephyr.deployments_data_srv.service.interfaces;

import com.zephyr.deployments_data_srv.GetMetrics200ResponseAllOfContentInner;
import org.springframework.data.domain.Page;

import java.util.List;

public interface DeploymentMetricsService {
    Page<GetMetrics200ResponseAllOfContentInner> getServiceMetrics(
            List<String> environments,
            String serviceName,
            String timeRange,
            int page,
            int size
    );
}

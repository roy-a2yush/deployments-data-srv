package com.zephyr.deployments_data_srv.service.implementations;

import com.zephyr.deployments_data_srv.GetMetrics200ResponseAllOfContentInner;
import com.zephyr.deployments_data_srv.model.Deployment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentEnvironment;
import com.zephyr.deployments_data_srv.model.enums.DeploymentStatus;
import com.zephyr.deployments_data_srv.repository.implementations.DeploymentSpecification;
import com.zephyr.deployments_data_srv.repository.interfaces.DeploymentRepository;
import com.zephyr.deployments_data_srv.service.interfaces.DeploymentMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
public class DeploymentMetricsServiceImpl implements DeploymentMetricsService {

    private final DeploymentRepository repository;
    private static final Pattern TIME_RANGE_PATTERN = Pattern.compile("^([1-9][0-9]*)([hdm])$");

    public DeploymentMetricsServiceImpl(DeploymentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Page<GetMetrics200ResponseAllOfContentInner> getServiceMetrics(
            List<String> environments,
            String serviceName,
            String timeRange,
            int page,
            int size) {

        log.info("Calculating service metrics. Env: {}, Service: {}, TimeRange: {}, Page: {}, Size: {}",
                environments, serviceName, timeRange, page, size);

        // 1. Parse and validate timeRange
        if (timeRange == null || timeRange.trim().isEmpty()) {
            throw new IllegalArgumentException("Time range query parameter cannot be null or empty");
        }

        Matcher matcher = TIME_RANGE_PATTERN.matcher(timeRange.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Time range format must match pattern ^([1-9][0-9]*)([hdm])$ (e.g., 1h, 1d, 12m)");
        }

        long quantity = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2);
        long hours;

        switch (unit) {
            case "h":
                hours = quantity;
                break;
            case "d":
                hours = quantity * 24;
                break;
            case "m":
                hours = quantity * 30 * 24;
                break;
            default:
                throw new IllegalArgumentException("Unknown time range unit: " + unit);
        }

        // Limit range: Min 1 hour, Max 12 months (12 * 30 * 24 = 8640 hours)
        if (hours < 1) {
            throw new IllegalArgumentException("Minimum time range is 1 hour");
        }
        if (hours > 8640) {
            throw new IllegalArgumentException("Maximum time range is 12 months (or equivalent 8640h or 360d)");
        }

        // 2. Parse optional string environments to strong enums
        List<DeploymentEnvironment> envEnums = null;
        if (environments != null && !environments.isEmpty()) {
            envEnums = environments.stream()
                    .map(DeploymentEnvironment::fromValue)
                    .collect(Collectors.toList());
        }

        // 3. Compute start timestamp boundary
        OffsetDateTime startTimestamp = OffsetDateTime.now().minusHours(hours);

        // 4. Query deployments matching criteria using specifications
        Specification<Deployment> spec = DeploymentSpecification.builder()
                .withService(serviceName)
                .withEnvironments(envEnums)
                .withTimestampAfter(startTimestamp)
                .build();

        List<Deployment> deployments = repository.findAll(spec);

        // 5. Group by service and environment
        Map<String, Map<DeploymentEnvironment, List<Deployment>>> grouped = deployments.stream()
                .collect(Collectors.groupingBy(
                        Deployment::getService,
                        Collectors.groupingBy(Deployment::getEnvironment)
                ));

        List<GetMetrics200ResponseAllOfContentInner> metricsList = new ArrayList<>();

        for (Map.Entry<String, Map<DeploymentEnvironment, List<Deployment>>> serviceEntry : grouped.entrySet()) {
            String service = serviceEntry.getKey();
            for (Map.Entry<DeploymentEnvironment, List<Deployment>> envEntry : serviceEntry.getValue().entrySet()) {
                DeploymentEnvironment env = envEntry.getKey();
                List<Deployment> groupDeployments = envEntry.getValue();

                // Compute metrics
                double deploymentFrequencyPerHour = (double) groupDeployments.size() / hours;

                long failedCount = groupDeployments.stream()
                        .filter(d -> d.getStatus() == DeploymentStatus.FAILED)
                        .count();
                double failureRate = groupDeployments.isEmpty() ? 0.0 : (double) failedCount / groupDeployments.size();

                List<Integer> durations = groupDeployments.stream()
                        .map(Deployment::getDuration)
                        .sorted()
                        .collect(Collectors.toList());

                double p95DeploymentTime = 0.0;
                if (!durations.isEmpty()) {
                    int idx = (int) Math.ceil(0.95 * durations.size()) - 1;
                    idx = Math.max(0, Math.min(durations.size() - 1, idx));
                    p95DeploymentTime = durations.get(idx).doubleValue();
                }

                GetMetrics200ResponseAllOfContentInner metrics = new GetMetrics200ResponseAllOfContentInner();
                metrics.setService(service);
                metrics.setEnvironment(env.getValue());
                metrics.setDeploymentFrequencyPerHour(deploymentFrequencyPerHour);
                metrics.setFailureRate(failureRate);
                metrics.setP95DeploymentTime(p95DeploymentTime);

                metricsList.add(metrics);
            }
        }

        // Sort by service name, then environment to ensure deterministic pagination
        metricsList.sort((a, b) -> {
            int comp = a.getService().compareTo(b.getService());
            if (comp != 0) return comp;
            return a.getEnvironment().compareTo(b.getEnvironment());
        });

        // 6. Perform offset pagination manually on the sorted results
        int start = Math.max(0, Math.min((int) PageRequest.of(page, size).getOffset(), metricsList.size()));
        int end = Math.min((start + size), metricsList.size());
        List<GetMetrics200ResponseAllOfContentInner> subList = (start < metricsList.size())
                ? metricsList.subList(start, end)
                : Collections.emptyList();

        return new PageImpl<>(subList, PageRequest.of(page, size), metricsList.size());
    }
}

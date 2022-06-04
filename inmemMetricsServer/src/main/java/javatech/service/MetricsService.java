package javatech.service;

import javatech.metrics.MetricsBatch;
import javatech.metrics.MultipleMetricsStoreRequest;
import javatech.metrics.MultipleMetricsTimeRangeQuery;

import java.util.List;

public interface MetricsService {
    MetricsBatch getMetricsForQuery(MultipleMetricsTimeRangeQuery metricsQuery);
    List<String> getMetricNamesForProject(String project);
    void storeMetrics(MultipleMetricsStoreRequest storeRequest);
    List<String> getProjectsNames();
}

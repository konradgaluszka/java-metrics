package javatech.service;

import javatech.metrics.MetricsBatch;
import javatech.metrics.MultipleMetricsStoreRequest;
import javatech.metrics.MultipleMetricsTimeRangeQuery;

public interface MetricsService {
    MetricsBatch getMetricsForQuery(MultipleMetricsTimeRangeQuery metricsQuery);
    void storeMetrics(MultipleMetricsStoreRequest storeRequest);

}

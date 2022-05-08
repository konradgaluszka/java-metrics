package javatech.metrics;

import java.util.List;
import java.util.Map;

public record MetricsBatch(
        List<MetricsValuesGroup> metricsValues,
        Map<String, MetricsMetadata> metricsMetadata) {

}

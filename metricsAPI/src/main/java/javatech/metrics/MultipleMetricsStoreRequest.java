package javatech.metrics;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record MultipleMetricsStoreRequest(String project,
                                          Instant timestamp,
                                          Map<String, MetricsMetadata> metadataMap,
                                          List<MetricsValuesGroup> metricsValues
) {
}

package javatech.metrics;

import java.time.Instant;

public record MetricsValuesGroup(MetricTags tags, MetricValues values, Instant time){
}

package javatech.metrics;

import java.time.Instant;
import java.util.List;

public record MultipleMetricsTimeRangeQuery(String project,
                                            Instant timeFrom,
                                            Instant timeTo,
                                            List<String> metricNames,
                                            List<MetricsQueryTag> queryTags) {
}

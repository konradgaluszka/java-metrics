package javatech.metrics;

import java.time.Instant;
import java.util.List;

public record MultipleMetricsTimeRangeQuery(String project,
                                            Instant timeFrom,
                                            Instant timeTo,
                                            List<String> metricNames,
                                            List<MetricsQueryTag> queryTags) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MultipleMetricsTimeRangeQuery that = (MultipleMetricsTimeRangeQuery) o;

        if (!project.equals(that.project)) return false;
        if (!timeFrom.equals(that.timeFrom)) return false;
        if (!timeTo.equals(that.timeTo)) return false;
        if (!metricNames.equals(that.metricNames)) return false;
        return queryTags.equals(that.queryTags);
    }

    @Override
    public int hashCode() {
        int result = project.hashCode();
        result = 31 * result + timeFrom.hashCode();
        result = 31 * result + timeTo.hashCode();
        result = 31 * result + metricNames.hashCode();
        result = 31 * result + queryTags.hashCode();
        return result;
    }
}

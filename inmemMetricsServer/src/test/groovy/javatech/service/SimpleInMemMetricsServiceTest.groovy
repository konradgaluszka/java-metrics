package javatech.service

import javatech.metrics.MetricTags
import javatech.metrics.MetricType
import javatech.metrics.MetricValues
import javatech.metrics.MetricsMetadata
import javatech.metrics.MetricsQueryTag
import javatech.metrics.MetricsValuesGroup
import javatech.metrics.MultipleMetricsStoreRequest
import javatech.metrics.MultipleMetricsTimeRangeQuery
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.Instant

class SimpleInMemMetricsServiceTest extends Specification {
    @Subject
    private MetricsService service

    def setup() {
        service = new SimpleInMemMetricsService()
    }

    def "GetMetricsForQueryWithOneTimestamp"() {
        given:
        def time = Instant.parse("2022-05-01T18:35:00.00Z")
        def storedMetrics = new MultipleMetricsStoreRequest(
                "project1",
                time,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 245]))]);
        when:
        service.storeMetrics(storedMetrics)
        def result = service.getMetricsForQuery(new MultipleMetricsTimeRangeQuery("project1", time, time, ["metric1"], []))

        then:
        def values = result.metricsValues()
        values.size() == 1
        values[0].tags().tags() == ["svc": "svc1", "method": "method1"]
        values[0].values().values() == ["metric1": 245]
    }

    def "GetMetricsForQueryReturnsMetricsWithinSpecifiedTimeWindow"() {
        given:
        def time1 = Instant.parse("2022-05-01T18:34:00.00Z")
        def time2 = Instant.parse("2022-05-01T18:35:00.00Z")
        def time3 = Instant.parse("2022-05-01T18:36:00.00Z")
        def time4 = Instant.parse("2022-05-01T18:37:00.00Z")
        def queryTimeFrom = Instant.parse("2022-05-01T18:35:00.00Z")
        def queryTimeTo = Instant.parse("2022-05-01T18:36:00.00Z")
        def storedMetrics1 = new MultipleMetricsStoreRequest(
                "project1",
                time1,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 245]))]);
        def storedMetrics2 = new MultipleMetricsStoreRequest(
                "project1",
                time2,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 1]))]);
        def storedMetrics3 = new MultipleMetricsStoreRequest(
                "project1",
                time3,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 9]))]);
        def storedMetrics4 = new MultipleMetricsStoreRequest(
                "project1",
                time4,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 20]))]);
        when:
        service.storeMetrics(storedMetrics1)
        service.storeMetrics(storedMetrics2)
        service.storeMetrics(storedMetrics3)
        service.storeMetrics(storedMetrics4)
        def result = service.getMetricsForQuery(new MultipleMetricsTimeRangeQuery("project1", queryTimeFrom, queryTimeTo, ["metric1"], []))

        then:
        def values = result.metricsValues()
        values.size() == 2
        values[0].tags().tags() == ["svc": "svc1", "method": "method1"]
        values[0].values().values() == ["metric1": 246]
        values[1].tags().tags() == ["svc": "svc1", "method": "method1"]
        values[1].values().values() == ["metric1": 255]
    }

    def "GetMetricsForQueryReturnsOnlyMetricsMatchingName"() {
        given:
        def time = Instant.parse("2022-05-01T18:35:00.00Z")
        def queryTime = Instant.parse("2022-05-01T18:35:00.00Z")
        def storedMetrics1 = new MultipleMetricsStoreRequest(
                "project1",
                time,
                ["metric1": new MetricsMetadata(MetricType.COUNTER), "metric2": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 245, "metric2": 100]))]);

        when:
        service.storeMetrics(storedMetrics1)
        def result = service.getMetricsForQuery(new MultipleMetricsTimeRangeQuery("project1", queryTime, queryTime, ["metric1"], []))

        then:
        def values = result.metricsValues()
        values.size() == 1
        values[0].tags().tags() == ["svc": "svc1", "method": "method1"]
        values[0].values().values() == ["metric1": 245]
    }

    def "GetMetricsForQueryReturnsOnlyMetricsMatchingTags"() {
        given:
        def time = Instant.parse("2022-05-01T18:35:00.00Z")
        def queryTime = Instant.parse("2022-05-01T18:35:00.00Z")
        def storedMetrics1 = new MultipleMetricsStoreRequest(
                "project1",
                time,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 245]))]);
        def storedMetrics2 = new MultipleMetricsStoreRequest(
                "project1",
                time,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method2"]), new MetricValues(["metric1": 230]))]);

        when:
        service.storeMetrics(storedMetrics1)
        service.storeMetrics(storedMetrics2)
        def result = service.getMetricsForQuery(new MultipleMetricsTimeRangeQuery("project1", queryTime, queryTime, ["metric1"],
                [new MetricsQueryTag("svc", "svc1"), new MetricsQueryTag("method", "method1")]))

        then:
        def values = result.metricsValues()
        values.size() == 1
        values[0].tags().tags() == ["svc": "svc1", "method": "method1"]
        values[0].values().values() == ["metric1": 245]
    }

    def "GetMetricsForQueryReturnsEmptyIfMetricsNotFound"() {
        given:
        def time = Instant.parse("2022-05-01T18:35:00.00Z")
        def queryTime = Instant.parse("2022-05-01T18:35:00.00Z")
        def storedMetrics1 = new MultipleMetricsStoreRequest(
                "project1",
                time,
                ["metric1": new MetricsMetadata(MetricType.COUNTER)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 245]))]);

        when:
        service.storeMetrics(storedMetrics1)
        def result = service.getMetricsForQuery(new MultipleMetricsTimeRangeQuery("project1", queryTime, queryTime, ["metric1"],
                [new MetricsQueryTag("svc", "svc1"), new MetricsQueryTag("method", "not-existing-method")]))

        then:
        def values = result.metricsValues()
        values.size() == 0
    }

    @Unroll
    def "GetMetricsForQueryReturnsCorrectlyUpdated #metricType"() {
        given:
        def time1 = Instant.parse("2022-05-01T18:35:00.00Z")
        def time2 = Instant.parse("2022-05-01T18:36:00.00Z")
        def storedInitialValue = new MultipleMetricsStoreRequest(
                "project1",
                time1,
                ["metric1": new MetricsMetadata(metricType)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 245]))]);
        def storedUpdatedValue = new MultipleMetricsStoreRequest(
                "project1",
                time2,
                ["metric1": new MetricsMetadata(metricType)],
                [new MetricsValuesGroup(new MetricTags(["svc": "svc1", "method": "method1"]), new MetricValues(["metric1": 230]))]);

        when:
        service.storeMetrics(storedInitialValue)
        service.storeMetrics(storedUpdatedValue)
        def result = service.getMetricsForQuery(new MultipleMetricsTimeRangeQuery("project1", time1, time2, ["metric1"], []))

        then:
        def values = result.metricsValues()
        values.size() == 2
        values[0].tags().tags() == ["svc": "svc1", "method": "method1"]
        values[0].values().values() == ["metric1": expectedValue1]
        values[1].tags().tags() == ["svc": "svc1", "method": "method1"]
        values[1].values().values() == ["metric1": expectedValue2]

        where:
        metricType         | expectedValue1 | expectedValue2
        MetricType.COUNTER | 245            | 475
        MetricType.GAUGE   | 245            | 230
    }
}

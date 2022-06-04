package javatech.service;

import javatech.metrics.MetricTags;
import javatech.metrics.MetricType;
import javatech.metrics.MetricValues;
import javatech.metrics.MetricsBatch;
import javatech.metrics.MetricsMetadata;
import javatech.metrics.MetricsQueryTag;
import javatech.metrics.MetricsValuesGroup;
import javatech.metrics.MultipleMetricsStoreRequest;
import javatech.metrics.MultipleMetricsTimeRangeQuery;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.springframework.util.Assert.isTrue;
import static org.springframework.util.Assert.notNull;

public class SimpleInMemMetricsService implements MetricsService {
    private final Map<String, TreeMap<Instant, Map<MetricTags, MetricValues>>> metricsPerProjectPerInstant = new ConcurrentHashMap<>();
    private final Map<String, Map<String, MetricsMetadata>> metricsMetadataPerProject = new ConcurrentHashMap<>();


    public SimpleInMemMetricsService() {
    }

    @Override
    public MetricsBatch getMetricsForQuery(MultipleMetricsTimeRangeQuery metricsQuery) {
        validateQuery(metricsQuery);
        Map<Instant, Set<MetricsValuesGroup>> results = filterOutResults(metricsQuery);
        return new MetricsBatch(results.entrySet().stream()
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toList()), new HashMap<>());
    }

    @Override
    public List<String> getMetricNamesForProject(String project) {
        if (metricsMetadataPerProject.containsKey(project)) {
            return metricsMetadataPerProject.get(project).keySet().stream().toList();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void storeMetrics(MultipleMetricsStoreRequest storeRequest) {
        validateStoreRequest(storeRequest);
        updateMetadata(storeRequest);
        updateMetrics(storeRequest);
    }

    @Override
    public List<String> getProjectsNames() {
        return metricsMetadataPerProject.keySet().stream().toList();
    }

    private void updateMetadata(MultipleMetricsStoreRequest storeRequest) {
        metricsMetadataPerProject.putIfAbsent(storeRequest.project(), new HashMap<>());
        Map<String, MetricsMetadata> currentMetaData = metricsMetadataPerProject.get(storeRequest.project());
        for (Map.Entry<String, MetricsMetadata> requestMetaDataEntry : storeRequest.metadataMap().entrySet()) {
            if (currentMetaData.containsKey(requestMetaDataEntry.getKey()) &&
                    !currentMetaData.get(requestMetaDataEntry.getKey()).equals(requestMetaDataEntry.getValue())) {
                throw new IllegalArgumentException("Cannot change metadata of a metric :" + requestMetaDataEntry.getKey());
            } else if (!currentMetaData.containsKey(requestMetaDataEntry.getKey())) {
                currentMetaData.put(requestMetaDataEntry.getKey(), requestMetaDataEntry.getValue());
            }
        }
    }

    private void updateMetrics(MultipleMetricsStoreRequest storeRequest) {
        metricsPerProjectPerInstant.putIfAbsent(storeRequest.project(), new TreeMap<>());
        metricsPerProjectPerInstant.get(storeRequest.project()).putIfAbsent(storeRequest.timestamp(), new HashMap<>());
        updateValues(storeRequest);
    }

    private void updateValues(MultipleMetricsStoreRequest storeRequest) {
        Map<MetricTags, MetricValues> currentMetrics = metricsPerProjectPerInstant.get(storeRequest.project()).get(storeRequest.timestamp());
        storeRequest.metricsValues().forEach(nv -> {
            currentMetrics.putIfAbsent(nv.tags(), new MetricValues(new HashMap<>()));
            updateExistingMetrics(nv.values(), currentMetrics.get(nv.tags()), nv.tags(), storeRequest);
        });
    }

    private void updateExistingMetrics(MetricValues candidates, MetricValues existing,
                                       MetricTags metricTags, MultipleMetricsStoreRequest storeRequest) {
        Map<String, MetricsMetadata> metadata = metricsMetadataPerProject.get(storeRequest.project());
        for (String metricName : candidates.values().keySet()) {
            existing.values().compute(metricName, (name, value) -> {
                if (value == null) {
                    if (metadata.get(name).metricType().equals(MetricType.COUNTER)) {
                        Optional<Integer> previousValue = getPreviousCounterValue(storeRequest.timestamp(), metricTags, metricName, storeRequest.project());
                        if (previousValue.isPresent()) {
                            return previousValue.get() + (Integer) candidates.values().get(name);
                        } else {
                            return candidates.values().get(name);
                        }
                    } else {
                        return candidates.values().get(name);
                    }
                } else {
                    if (metadata.get(name).metricType().equals(MetricType.COUNTER)) {
                        return (Double) value + (Double) candidates.values().get(name);
                    } else {
                        return candidates.values().get(name);
                    }
                }
            });
        }
    }

    private Optional<Integer> getPreviousCounterValue(Instant timestamp, MetricTags metricTags, String metricName, String project) {
        TreeMap<Instant, Map<MetricTags, MetricValues>> projectMetrics = metricsPerProjectPerInstant.get(project);
        Map.Entry<Instant, Map<MetricTags, MetricValues>> prevEntry = projectMetrics.lowerEntry(timestamp);
        if (prevEntry == null) {
            return Optional.empty();
        }
        MetricValues metricValues = prevEntry.getValue().get(metricTags);
        if (metricValues == null) {
            return Optional.empty();
        }
        return Optional.of((Integer) metricValues.values().get(metricName));
    }


    private void validateStoreRequest(MultipleMetricsStoreRequest storeRequest) {
        // todo: implement
    }

    private Map<Instant, Set<MetricsValuesGroup>> filterOutResults(MultipleMetricsTimeRangeQuery metricsQuery) {
        Map<Instant, Set<MetricsValuesGroup>> results = new ConcurrentHashMap<>();
        TreeMap<Instant, Map<MetricTags, MetricValues>> projectMetrics = metricsPerProjectPerInstant.get(metricsQuery.project());
        Set<Instant> timestampsMatchingRange = projectMetrics.navigableKeySet()
                .subSet(metricsQuery.timeFrom(), true, metricsQuery.timeTo(), true);
        for (Instant ts : timestampsMatchingRange) {
            for (Map.Entry<MetricTags, MetricValues> candidate : projectMetrics.get(ts).entrySet()) {
                if (!tagsMatching(metricsQuery.queryTags(), candidate.getKey().tags())) {
                    continue;
                }
                if (!metricsMatching(metricsQuery.metricNames(), candidate.getValue().values())) {
                    continue;
                }
                Map<String, Object> matchingMetrics = candidate.getValue().values().entrySet()
                        .stream()
                        .filter(e -> metricsQuery.metricNames().contains(e.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                results.putIfAbsent(ts, new HashSet<>());
                results.get(ts).add(new MetricsValuesGroup(new MetricTags(candidate.getKey().tags()), new MetricValues(matchingMetrics), ts));
            }
        }
        return results;
    }

    private boolean metricsMatching(List<String> metricNames, Map<String, Object> values) {
        Set<String> newSet = values.keySet();
        newSet.retainAll(metricNames);
        return !newSet.isEmpty();
    }

    private boolean tagsMatching(List<MetricsQueryTag> queryTags, Map<String, Object> tags) {
        return queryTags.stream().allMatch(queryTag ->
                tags.containsKey(queryTag.tagName()) && tags.get(queryTag.tagName()).equals(queryTag.tagValue()));
    }

    private void validateQuery(MultipleMetricsTimeRangeQuery metricsQuery) {
        isTrue(metricsPerProjectPerInstant.containsKey(metricsQuery.project()), "Server should contain metricsValues for project!");
        notNull(metricsQuery.timeFrom(), "Time from in specified time window should not be empty!");
        notNull(metricsQuery.timeTo(), "Time to in specified time window should not be empty!");
        isTrue(metricsQuery.metricNames().stream().noneMatch(StringUtils::isBlank), "List of queried metrics should not contain empty names!");
    }
}

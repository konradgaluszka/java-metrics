package javatech.endpoint;

import javatech.metrics.MetricsBatch;
import javatech.metrics.MultipleMetricsTimeRangeQuery;
import javatech.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {

    private final MetricsService metricsService;


    public MetricsController(@Autowired MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/projectNames")
    public List<String> getProjectNames() {
        return metricsService.getProjectsNames();
    }

    @GetMapping("/{projectName}/metricsNames")
    public List<String> namesForProject(@PathVariable("projectName") String projectName) {
        return metricsService.getMetricNamesForProject(projectName);
    }

    @PostMapping(consumes =  "application/json", produces = "application/json")
    public MetricsBatch getMetrics(@RequestBody MultipleMetricsTimeRangeQuery metricsQuery) {
        return metricsService.getMetricsForQuery(metricsQuery);
    }

}

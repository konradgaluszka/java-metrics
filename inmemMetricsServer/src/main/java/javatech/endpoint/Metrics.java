package javatech.endpoint;

import javatech.metrics.MetricsBatch;
import javatech.metrics.MultipleMetricsTimeRangeQuery;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/metricsValues")
public class Metrics {

    @PostMapping
    public MetricsBatch getMetrics(@RequestBody MultipleMetricsTimeRangeQuery metricsQuery) {
        return null;
    }

}

/**
 * @Controller
 * @ResponseBody
 * @RequestMapping("/api/tree")
 * public class TreeController {
 *
 *     @Autowired
 *     private TreeRepository repository;
 *
 *     @GetMapping("/{id}")
 *     public Tree getTreeById(@PathVariable int id) {
 *         return repository.findById(id);
 *     }
 *
 *     @GetMapping
 *     public Tree getTreeById(@RequestParam String name,
 *                             @RequestParam int age) {
 *         return repository.findFirstByCommonNameIgnoreCaseAndAge(name, age);
 *     }
 */

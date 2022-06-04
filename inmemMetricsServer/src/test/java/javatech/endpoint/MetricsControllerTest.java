package javatech.endpoint;

import com.fasterxml.jackson.databind.ObjectMapper;
import javatech.metrics.MetricTags;
import javatech.metrics.MetricType;
import javatech.metrics.MetricValues;
import javatech.metrics.MetricsBatch;
import javatech.metrics.MetricsMetadata;
import javatech.metrics.MetricsValuesGroup;
import javatech.metrics.MultipleMetricsTimeRangeQuery;
import javatech.service.MetricsService;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class MetricsControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MetricsService metricsService;

    @BeforeEach
    public void init() {
        objectMapper.findAndRegisterModules();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getsProjectNameOfAllRegisteredMetrics() throws Exception {
        when(metricsService.getProjectsNames()).thenReturn(Lists.list("project1", "project2"));

        //expect Status is 200 and response contains project names
        String contentAsString = mockMvc.perform(get("/api/metrics/projectNames")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> metricsBatch = objectMapper.readValue(contentAsString, List.class);
        Assertions.assertThat(metricsBatch).hasSameElementsAs(Lists.list("project1", "project2"));
    }


    @Test
    @SuppressWarnings("unchecked")
    void getsMetricsNamesForProject() throws Exception {
        when(metricsService.getMetricNamesForProject("project1")).thenReturn(Lists.list("metric1", "metric2"));

        //expect Status is 200 and response contains metrics result
        String contentAsString = mockMvc.perform(get("/api/metrics/project1/metricsNames")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<String> metricsBatch = objectMapper.readValue(contentAsString, List.class);
        Assertions.assertThat(metricsBatch).hasSameElementsAs(Lists.list("metric1", "metric2"));
    }

    @Test
    void getMetricsForQuery() throws Exception {
        String requestQueryStr = """
                {
                  "project": "project1",
                  "timeFrom": "2022-05-27T07:32:00.00Z",
                  "timeTo": "2022-05-27T07:33:00.00Z",
                  "metricNames": [
                    "metric1"
                  ],
                  "queryTags": [
                    {
                      "tagName": "svc",
                      "tagValue": "svc1"
                    }
                  ]
                }
                """;

        MultipleMetricsTimeRangeQuery requestQueryObj = objectMapper.readValue(requestQueryStr, MultipleMetricsTimeRangeQuery.class);

        when(metricsService.getMetricsForQuery(eq(requestQueryObj))).thenReturn(
                new MetricsBatch(
                        List.of(new MetricsValuesGroup(
                                new MetricTags(Map.of("svc", "svc1")),
                                new MetricValues(Map.of("metric1", 125)),
                                Instant.parse("2022-05-27T07:32:00.00Z"))),
                        Map.of("metric1", new MetricsMetadata(MetricType.COUNTER))));

        //expect Status is 200 and response contains metrics
        mockMvc.perform(post("/api/metrics").content(requestQueryStr).contentType("application/json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metricsMetadata.*", hasSize(1)))
                .andExpect(jsonPath("$.metricsMetadata.metric1.metricType", equalTo("COUNTER")))
                .andExpect(jsonPath("$.metricsValues[0].tags.tags.*", hasSize(1)))
                .andExpect(jsonPath("$.metricsValues[0].tags.tags.svc", equalTo("svc1")))
                .andExpect(jsonPath("$.metricsValues[0].time", equalTo("2022-05-27T07:32:00Z")))
                .andExpect(jsonPath("$.metricsValues", hasSize(1)))
                .andExpect(jsonPath("$.metricsValues[0].values.values.metric1", equalTo(125)));

    }
}

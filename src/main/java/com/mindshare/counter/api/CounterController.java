package com.mindshare.counter.api;

import com.mindshare.counter.api.dto.CountsResponse;
import com.mindshare.counter.schema.CounterSchema;
import com.mindshare.counter.service.CounterService;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/counter")
@Profile("!bootstrap-test")
public class CounterController {

    private final CounterService counterService;

    public CounterController(CounterService counterService) {
        this.counterService = counterService;
    }

    @GetMapping("/{etype}/{eid}")
    public ResponseEntity<CountsResponse> getCounts(
            @PathVariable("etype") String entityType,
            @PathVariable("eid") String entityId,
            @RequestParam(value = "metrics", required = false) String metricsText
    ) {
        List<String> metrics;
        if (metricsText == null || metricsText.isBlank()) {
            metrics = new ArrayList<>(CounterSchema.SUPPORTED_METRICS);
        } else {
            metrics = Arrays.stream(metricsText.split(","))
                    .map(String::trim)
                    .filter(CounterSchema.SUPPORTED_METRICS::contains)
                    .toList();
        }
        Map<String, Long> counts = counterService.getCounts(entityType, entityId, metrics);
        return ResponseEntity.ok(new CountsResponse(entityType, entityId, counts));
    }
}

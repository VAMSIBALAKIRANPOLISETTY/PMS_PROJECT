package com.pms.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Health", description = "Runtime health checks for local development and API monitoring.")
public class HealthController {
    @Operation(summary = "Check API health", description = "Returns a small status payload so the backend can be verified before authenticated testing.")
    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "service", "pms-spring-boot-api");
    }
}

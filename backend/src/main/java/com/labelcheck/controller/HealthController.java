package com.labelcheck.controller;

import com.labelcheck.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing the health check endpoint for monitoring service readiness.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final HealthResponse HEALTH_OK = new HealthResponse("UP", "LabelCheck Backend");

    /**
     * Health check endpoint to verify that the backend is alive and operational.
     *
     * @return HTTP 200 with status "UP" and service name
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        return ResponseEntity.ok(HEALTH_OK);
    }
}

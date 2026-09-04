package com.labelcheck.controller;

import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.HealthResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing the health check endpoint for monitoring service readiness and AI configuration.
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private final AiProperties aiProperties;

    public HealthController(@Autowired(required = false) AiProperties aiProperties) {
        this.aiProperties = aiProperties != null ? aiProperties : new AiProperties();
    }

    /**
     * Health check endpoint to verify that the backend is alive and operational.
     * Indicates whether Vision AI is configured without leaking any API keys or secrets.
     *
     * @return HTTP 200 with status "UP", service name, aiEnabled flag, aiProvider, and model name
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> getHealth() {
        boolean enabled = aiProperties.isEnabled() && aiProperties.getApiKey() != null && !aiProperties.getApiKey().isBlank();
        String model = enabled ? aiProperties.getModel() : null;
        String provider = enabled ? (aiProperties.getProvider() != null ? aiProperties.getProvider().toLowerCase() : "groq") : null;
        return ResponseEntity.ok(new HealthResponse("UP", "LabelCheck Backend", enabled, provider, model));
    }
}


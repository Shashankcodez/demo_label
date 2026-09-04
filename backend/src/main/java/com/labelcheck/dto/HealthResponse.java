package com.labelcheck.dto;

/**
 * DTO representing the response from the health check endpoint.
 *
 * @param status     the operational status of the service (e.g. "UP")
 * @param service    the name of the service (e.g. "LabelCheck Backend")
 * @param aiEnabled  whether Vision AI extraction is enabled and configured with an API key
 * @param aiProvider the configured Vision AI provider (e.g. "Gemini")
 * @param aiModel    the configured Vision AI model name (e.g. "gemini-3.8-flash", or null if disabled)
 */
public record HealthResponse(
        String status,
        String service,
        boolean aiEnabled,
        String aiProvider,
        String aiModel
) {
    public HealthResponse(String status, String service) {
        this(status, service, false, null, null);
    }

    public HealthResponse(String status, String service, boolean aiEnabled, String aiModel) {
        this(status, service, aiEnabled, aiEnabled ? "Gemini" : null, aiModel);
    }
}

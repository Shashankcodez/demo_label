package com.labelcheck.dto;

/**
 * DTO representing the response from the health check endpoint.
 *
 * @param status  the operational status of the service (e.g. "UP")
 * @param service the name of the service (e.g. "LabelCheck Backend")
 */
public record HealthResponse(
        String status,
        String service
) {
}

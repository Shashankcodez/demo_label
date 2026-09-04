package com.labelcheck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the Vision AI extraction layer (Groq Vision with qwen/qwen3.6-27b).
 * All properties are fully externalized via environment variables and application.properties.
 * The API key is stored strictly on the server and is never exposed to the frontend.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * Master toggle for Vision AI. When false, the system automatically runs in local deterministic OCR mode.
     */
    private boolean enabled = true;

    /**
     * Vision AI provider: "groq", "openai", or "gemini".
     */
    private String provider = "groq";

    /**
     * API key for the Vision AI provider (e.g. GROQ_API_KEY).
     * Strictly server-side; NEVER sent to the client or logged.
     */
    private String apiKey = "";

    /**
     * Base URL for the OpenAI-compatible Groq API endpoint.
     * Default: https://api.groq.com/openai/v1
     */
    private String baseUrl = "https://api.groq.com/openai/v1";

    /**
     * Vision multimodal model name (default: qwen/qwen3.6-27b).
     */
    private String model = "qwen/qwen3.6-27b";

    /**
     * Network connect/read timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Maximum retry attempts for transient HTTP failures (e.g. 429 rate limit or 503).
     */
    private int maxRetries = 1;

    /**
     * Maximum completion tokens for vision response (default: 2500).
     */
    private int maxCompletionTokens = 2500;

    /**
     * Reasoning effort setting (e.g. "none" for non-thinking mode on Qwen 3.6).
     */
    private String reasoningEffort = "none";

    /**
     * Sampling temperature (default: 0.2).
     */
    private double temperature = 0.2;

    /**
     * Legacy thinking budget property.
     */
    private int thinkingBudget = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider != null ? provider.trim().toLowerCase() : "groq";
    }

    public String getApiKey() {
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey;
        }
        String fromEnvFile = loadKeyFromDotEnv();
        if (fromEnvFile != null && !fromEnvFile.isBlank()) {
            return fromEnvFile;
        }
        return "";
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    private String loadKeyFromDotEnv() {
        java.util.List<java.nio.file.Path> candidatePaths = java.util.List.of(
                java.nio.file.Path.of(".env"),
                java.nio.file.Path.of("../.env"),
                java.nio.file.Path.of("backend/.env")
        );
        for (java.nio.file.Path p : candidatePaths) {
            if (java.nio.file.Files.exists(p)) {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(p);
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("#") || line.isBlank()) continue;
                        if (line.startsWith("GROQ_API_KEY=")) {
                            String val = line.substring("GROQ_API_KEY=".length()).trim();
                            val = val.replaceAll("^[\"']|[\"']$", "");
                            if (!val.isBlank()) return val;
                        } else if (line.startsWith("AI_API_KEY=")) {
                            String val = line.substring("AI_API_KEY=".length()).trim();
                            val = val.replaceAll("^[\"']|[\"']$", "");
                            if (!val.isBlank()) return val;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "https://api.groq.com/openai/v1";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : "qwen/qwen3.6-27b";
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = Math.max(0, Math.min(2, maxRetries));
    }

    public int getMaxCompletionTokens() {
        return maxCompletionTokens;
    }

    public void setMaxCompletionTokens(int maxCompletionTokens) {
        this.maxCompletionTokens = maxCompletionTokens > 0 ? maxCompletionTokens : 2500;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort != null ? reasoningEffort.trim() : "none";
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getThinkingBudget() {
        return thinkingBudget;
    }

    public void setThinkingBudget(int thinkingBudget) {
        this.thinkingBudget = Math.max(0, thinkingBudget);
    }
}


package com.labelcheck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Configuration properties for the Vision AI extraction layer:
 * Primary: Google Gemini Vision (gemini-3.6-flash)
 * Fallback: Groq Vision (qwen/qwen3.6-27b)
 *
 * All properties are externalized via environment variables and application.properties.
 * API keys are stored strictly on the server and are NEVER exposed to the frontend or logged.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /**
     * Master toggle for Vision AI. When false, the system automatically runs in local deterministic OCR mode.
     */
    private boolean enabled = true;

    /**
     * Primary Vision AI provider: "gemini" (default) or "groq".
     */
    private String provider = "gemini";

    /**
     * Generic API key fallback.
     */
    private String apiKey = "";

    /**
     * Gemini-specific API key (read from GEMINI_API_KEY).
     */
    private String geminiApiKey = "";

    /**
     * Groq-specific API key (read from GROQ_API_KEY).
     */
    private String groqApiKey = "";

    /**
     * Base URL for Google Gemini API endpoint.
     */
    private String geminiBaseUrl = "https://generativelanguage.googleapis.com";

    /**
     * Base URL for Groq OpenAI-compatible endpoint.
     */
    private String groqBaseUrl = "https://api.groq.com/openai/v1";

    /**
     * Generic baseUrl fallback.
     */
    private String baseUrl = "";

    /**
     * Primary Gemini model identifier (default: gemini-3.6-flash).
     */
    private String geminiModel = "gemini-3.6-flash";

    /**
     * Fallback Groq model identifier (default: qwen/qwen3.6-27b).
     */
    private String groqModel = "qwen/qwen3.6-27b";

    /**
     * Explicit model override (null/blank delegates to provider-specific model).
     */
    private String model = "";

    /**
     * Network connect/read timeout in seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Maximum retry attempts for transient HTTP failures.
     */
    private int maxRetries = 1;

    /**
     * Maximum completion tokens for vision response.
     */
    private int maxCompletionTokens = 4000;

    /**
     * Reasoning effort setting (e.g. "low" for lightweight thinking on Gemini Flash).
     */
    private String reasoningEffort = "low";

    /**
     * Sampling temperature (default: 0.1 for high extraction precision).
     */
    private double temperature = 0.1;

    /**
     * Thinking budget for Gemini models supporting thinkingConfig.
     */
    private int thinkingBudget = 1024;

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
        this.provider = provider != null ? provider.trim().toLowerCase() : "gemini";
    }

    public String getGeminiApiKey() {
        if (geminiApiKey != null && !geminiApiKey.isBlank()) {
            return geminiApiKey.trim();
        }
        String envKey = System.getenv("GEMINI_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        String fromDotEnv = loadKeyFromDotEnv("GEMINI_API_KEY");
        if (fromDotEnv != null && !fromDotEnv.isBlank()) {
            return fromDotEnv.trim();
        }
        if ("gemini".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        return "";
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey != null ? geminiApiKey.trim() : "";
    }

    public String getGroqApiKey() {
        if (groqApiKey != null && !groqApiKey.isBlank()) {
            return groqApiKey.trim();
        }
        String envKey = System.getenv("GROQ_API_KEY");
        if (envKey != null && !envKey.isBlank()) {
            return envKey.trim();
        }
        String fromDotEnv = loadKeyFromDotEnv("GROQ_API_KEY");
        if (fromDotEnv != null && !fromDotEnv.isBlank()) {
            return fromDotEnv.trim();
        }
        if ("groq".equalsIgnoreCase(provider) && apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }
        return "";
    }

    public void setGroqApiKey(String groqApiKey) {
        this.groqApiKey = groqApiKey != null ? groqApiKey.trim() : "";
    }

    public boolean isGeminiConfigured() {
        return !getGeminiApiKey().isBlank();
    }

    public boolean isGroqConfigured() {
        return !getGroqApiKey().isBlank();
    }

    public String getApiKey() {
        if ("groq".equalsIgnoreCase(provider)) {
            String key = getGroqApiKey();
            if (!key.isBlank()) return key;
        } else {
            String key = getGeminiApiKey();
            if (!key.isBlank()) return key;
        }

        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        String gemini = getGeminiApiKey();
        if (!gemini.isBlank()) return gemini;
        return getGroqApiKey();
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    private String loadKeyFromDotEnv(String keyName) {
        List<Path> candidatePaths = List.of(
                Path.of(".env"),
                Path.of("../.env"),
                Path.of("backend/.env")
        );
        for (Path p : candidatePaths) {
            if (Files.exists(p)) {
                try {
                    List<String> lines = Files.readAllLines(p);
                    for (String line : lines) {
                        line = line.trim();
                        if (line.startsWith("#") || line.isBlank()) continue;
                        if (line.startsWith(keyName + "=")) {
                            String val = line.substring((keyName + "=").length()).trim();
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

    public String getGeminiBaseUrl() {
        return geminiBaseUrl != null && !geminiBaseUrl.isBlank()
                ? geminiBaseUrl.trim().replaceAll("/+$", "")
                : "https://generativelanguage.googleapis.com";
    }

    public void setGeminiBaseUrl(String geminiBaseUrl) {
        this.geminiBaseUrl = geminiBaseUrl;
    }

    public String getGroqBaseUrl() {
        return groqBaseUrl != null && !groqBaseUrl.isBlank()
                ? groqBaseUrl.trim().replaceAll("/+$", "")
                : "https://api.groq.com/openai/v1";
    }

    public void setGroqBaseUrl(String groqBaseUrl) {
        this.groqBaseUrl = groqBaseUrl;
    }

    public String getBaseUrl() {
        if (baseUrl != null && !baseUrl.isBlank()) {
            return baseUrl.trim().replaceAll("/+$", "");
        }
        if ("groq".equalsIgnoreCase(provider)) {
            return getGroqBaseUrl();
        }
        return getGeminiBaseUrl();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.trim().replaceAll("/+$", "") : "";
    }

    public String getGeminiModel() {
        return geminiModel != null && !geminiModel.isBlank() ? geminiModel.trim() : "gemini-3.6-flash";
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel != null ? geminiModel.trim() : "gemini-3.6-flash";
    }

    public String getGroqModel() {
        return groqModel != null && !groqModel.isBlank() ? groqModel.trim() : "qwen/qwen3.6-27b";
    }

    public void setGroqModel(String groqModel) {
        this.groqModel = groqModel != null ? groqModel.trim() : "qwen/qwen3.6-27b";
    }

    public String getModel() {
        if (model != null && !model.isBlank()) {
            return model.trim();
        }
        if ("groq".equalsIgnoreCase(provider)) {
            return getGroqModel();
        }
        return getGeminiModel();
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : "";
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
        this.maxCompletionTokens = maxCompletionTokens > 0 ? maxCompletionTokens : 4000;
    }

    public String getReasoningEffort() {
        return reasoningEffort;
    }

    public void setReasoningEffort(String reasoningEffort) {
        this.reasoningEffort = reasoningEffort != null ? reasoningEffort.trim() : "low";
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getThinkingBudget() {
        if (thinkingBudget > 0) {
            return thinkingBudget;
        }
        if ("low".equalsIgnoreCase(reasoningEffort)) {
            return 1024;
        }
        return 0;
    }

    public void setThinkingBudget(int thinkingBudget) {
        this.thinkingBudget = Math.max(0, thinkingBudget);
    }
}

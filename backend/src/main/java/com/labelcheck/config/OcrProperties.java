package com.labelcheck.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly typed externalized configuration properties for the OCR extraction engine.
 */
@Configuration
@ConfigurationProperties(prefix = "app.ocr")
public class OcrProperties {

    private boolean enabled = true;
    private String defaultLanguage = "eng";
    private java.util.List<String> supportedLanguages = java.util.List.of(
            "eng", "hin", "tam", "tel", "kan", "mal", "mar", "ben", "guj", "pan", "ori"
    );
    private TesseractProperties tesseract = new TesseractProperties();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultLanguage() {
        return defaultLanguage;
    }

    public void setDefaultLanguage(String defaultLanguage) {
        this.defaultLanguage = defaultLanguage;
    }

    public java.util.List<String> getSupportedLanguages() {
        return supportedLanguages;
    }

    public void setSupportedLanguages(java.util.List<String> supportedLanguages) {
        this.supportedLanguages = supportedLanguages;
    }

    public TesseractProperties getTesseract() {
        return tesseract;
    }

    public void setTesseract(TesseractProperties tesseract) {
        this.tesseract = tesseract;
    }

    public static class TesseractProperties {
        private String datapath = "tessdata";
        private String language = "eng";

        public String getDatapath() {
            return datapath;
        }

        public void setDatapath(String datapath) {
            this.datapath = datapath;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }
    }
}


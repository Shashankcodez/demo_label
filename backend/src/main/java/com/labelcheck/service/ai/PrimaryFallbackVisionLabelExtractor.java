package com.labelcheck.service.ai;

import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.ai.AiExtractionStatus;
import com.labelcheck.dto.ai.AiLabelExtractionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

/**
 * Orchestrator implementing primary and fallback Vision AI extraction:
 * 1. Primary: Google Gemini Flash Vision (gemini-3.6-flash).
 * 2. Fallback: Groq Vision (qwen/qwen3.6-27b) if Gemini encounters rate limits or errors.
 * 3. Secondary/Fallback: Local Tesseract OCR (handled in ExtractionMergeService).
 */
@Service("visionLabelExtractor")
@Primary
public class PrimaryFallbackVisionLabelExtractor implements VisionLabelExtractor {

    private static final Logger log = LoggerFactory.getLogger(PrimaryFallbackVisionLabelExtractor.class);

    private final GeminiVisionLabelExtractor geminiExtractor;
    private final GroqVisionLabelExtractor groqExtractor;
    private final AiProperties aiProperties;

    @Autowired
    public PrimaryFallbackVisionLabelExtractor(
            @Qualifier("geminiVisionExtractor") GeminiVisionLabelExtractor geminiExtractor,
            @Qualifier("groqVisionExtractor") GroqVisionLabelExtractor groqExtractor,
            AiProperties aiProperties
    ) {
        this.geminiExtractor = geminiExtractor;
        this.groqExtractor = groqExtractor;
        this.aiProperties = aiProperties != null ? aiProperties : new AiProperties();
    }

    @Override
    public boolean isEnabled() {
        if (!aiProperties.isEnabled()) {
            return false;
        }
        return (geminiExtractor != null && geminiExtractor.isEnabled())
                || (groqExtractor != null && groqExtractor.isEnabled());
    }

    @Override
    public AiLabelExtractionResult extract(Path imagePath, String contentType) {
        String provider = aiProperties.getProvider() != null ? aiProperties.getProvider().trim().toLowerCase() : "gemini";

        if ("groq".equals(provider)) {
            return extractWithGroqPrimary(imagePath, contentType);
        } else {
            return extractWithGeminiPrimary(imagePath, contentType);
        }
    }

    private AiLabelExtractionResult extractWithGeminiPrimary(Path imagePath, String contentType) {
        if (geminiExtractor != null && geminiExtractor.isEnabled()) {
            log.info("Dispatching primary extraction to Gemini Vision [model={}]", aiProperties.getGeminiModel());
            try {
                AiLabelExtractionResult geminiResult = geminiExtractor.extract(imagePath, contentType);
                if (geminiResult != null && (geminiResult.status() == AiExtractionStatus.AI_SUCCESS
                        || geminiResult.status() == AiExtractionStatus.AI_PARTIAL)) {
                    log.info("Gemini Vision extraction succeeded: status=[{}], fieldsDetected=[{}]",
                            geminiResult.status(),
                            geminiResult.label() != null ? geminiResult.label().countDetectedFields() : 0);
                    return geminiResult;
                }
                log.warn("Gemini Vision extraction did not produce a successful result (status={}). Groq fallback is disabled for this runtime. Handing off directly to local Tesseract OCR.",
                        geminiResult != null ? geminiResult.status() : "null");
                return geminiResult != null ? geminiResult : createFallbackFailureResult();
            } catch (Exception ex) {
                log.warn("Gemini Vision extraction exception: {}. Groq fallback is disabled for this runtime. Handing off directly to local Tesseract OCR.", ex.getMessage());
                return createFallbackFailureResult();
            }
        } else {
            log.info("Gemini Vision is unconfigured or disabled. Groq fallback is disabled for this runtime. Handing off to local Tesseract OCR.");
            return createFallbackFailureResult();
        }
    }

    private AiLabelExtractionResult extractWithGroqPrimary(Path imagePath, String contentType) {
        if (groqExtractor != null && groqExtractor.isEnabled()) {
            log.info("Dispatching primary extraction to Groq Vision [model={}]", aiProperties.getGroqModel());
            try {
                AiLabelExtractionResult groqResult = groqExtractor.extract(imagePath, contentType);
                if (groqResult != null && (groqResult.status() == AiExtractionStatus.AI_SUCCESS
                        || groqResult.status() == AiExtractionStatus.AI_PARTIAL)) {
                    return groqResult;
                }
            } catch (Exception ex) {
                log.warn("Groq Vision extraction failed: {}. Attempting Gemini fallback...", ex.getMessage());
            }
        }

        if (geminiExtractor != null && geminiExtractor.isEnabled()) {
            log.info("Dispatching fallback extraction to Gemini Vision [model={}]", aiProperties.getGeminiModel());
            try {
                return geminiExtractor.extract(imagePath, contentType);
            } catch (Exception ex) {
                log.warn("Gemini Vision fallback extraction failed: {}", ex.getMessage());
            }
        }

        return createFallbackFailureResult();
    }

    private AiLabelExtractionResult createFallbackFailureResult() {
        return AiLabelExtractionResult.failed(
                AiExtractionStatus.AI_FAILED_TESSERACT_FALLBACK,
                aiProperties.getModel(),
                "Vision AI extraction failed or unconfigured. Local OCR fallback was used."
        );
    }
}

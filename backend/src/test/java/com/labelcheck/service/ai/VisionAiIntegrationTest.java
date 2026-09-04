package com.labelcheck.service.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelcheck.compliance.ComplianceRuleEngine;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.compliance.rules.DateMarkingRule;
import com.labelcheck.compliance.rules.MrpRule;
import com.labelcheck.compliance.rules.NetQuantityRule;
import com.labelcheck.config.AiProperties;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.dto.ScanResponse;
import com.labelcheck.dto.ai.FieldExtraction;
import com.labelcheck.dto.ai.StructuredAiLabel;
import com.labelcheck.entity.ScanEntity;
import com.labelcheck.repository.ScanRepository;
import com.labelcheck.service.FileStorageService;
import com.labelcheck.service.ImagePreprocessingService;
import com.labelcheck.service.LabelExtractionService;
import com.labelcheck.service.OcrService;
import com.labelcheck.service.ScanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VisionAiIntegrationTest {

    private static byte[] createSampleJpegBytes() throws IOException {
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("End-to-End Service Flow: Mock Vision AI → Structured Label → Deterministic Compliance Engine → Persistence")
    void testEndToEndWithVisionAi(@TempDir Path tempDir) throws Exception {
        FileStorageService fileStorageService = new FileStorageService(tempDir.toString());
        fileStorageService.init();

        OcrService mockOcrService = mock(OcrService.class);
        when(mockOcrService.extractText(any(Path.class))).thenReturn(
                new OcrResult("OCR_COMPLETE", "Raw OCR text evidence from tesseract", "eng")
        );

        LabelExtractionService labelExtractionService = new LabelExtractionService();
        ComplianceRuleEngine complianceEngine = new ComplianceRuleEngine(List.of(
                new MrpRule(),
                new NetQuantityRule(),
                new DateMarkingRule()
        ));

        ScanRepository mockScanRepository = mock(ScanRepository.class);
        when(mockScanRepository.save(any(ScanEntity.class))).thenAnswer(inv -> {
            ScanEntity e = inv.getArgument(0);
            e.setCreatedAt(Instant.now());
            return e;
        });

        ObjectMapper objectMapper = new ObjectMapper();

        AiProperties aiProperties = new AiProperties();
        aiProperties.setEnabled(true);
        aiProperties.setApiKey("gsk_mock_test_key_123");
        aiProperties.setProvider("groq");
        aiProperties.setModel("qwen/qwen3.6-27b");

        // Mock Vision AI extractor returning structured fields
        VisionLabelExtractor mockExtractor = mock(VisionLabelExtractor.class);
        when(mockExtractor.isEnabled()).thenReturn(true);

        StructuredAiLabel mockLabel = new StructuredAiLabel(
                0.95,
                FieldExtraction.of("Organic Roasted Cashews", 0.98, "Top headline"),
                FieldExtraction.of("NatureFresh", 0.92, "Brand logo"),
                FieldExtraction.of("200 g", 0.96, "Net Weight declaration"),
                FieldExtraction.of("299.00", 0.97, "MRP : Rs 299.00"),
                FieldExtraction.of("true", 0.95, "Inclusive of all taxes"),
                FieldExtraction.of("Rs 1.50 per g", 0.90, "Unit sale price line"),
                FieldExtraction.of("CASHEW2026", 0.93, "Batch code"),
                FieldExtraction.of("10/01/2026", 0.95, "Mfg Date"),
                FieldExtraction.of("10/07/2026", 0.95, "Best before 6 months"),
                FieldExtraction.of("10020011000888", 0.94, "FSSAI Lic No"),
                FieldExtraction.of("NUMBER_DETECTED", 0.95, "FSSAI valid"),
                FieldExtraction.of("NatureFresh Nut Corp", 0.92, "Manufacturer details"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("Plot 12, Industrial Area, Pune - 411001", 0.95, "Address text"),
                FieldExtraction.of("India", 0.90, "Country of Origin"),
                FieldExtraction.of("+91 9988776655", 0.96, "Customer phone"),
                FieldExtraction.of("care@naturefresh.com", 0.97, "Customer email"),
                FieldExtraction.of("Phone: +91 9988776655", 0.95, "Customer helpline"),
                FieldExtraction.empty(),
                FieldExtraction.empty(),
                FieldExtraction.of("vegetarian", 0.92, "Green dot"),
                FieldExtraction.empty(),
                List.of(),
                List.of()
        );

        when(mockExtractor.extract(any(Path.class), any())).thenReturn(
                com.labelcheck.dto.ai.AiLabelExtractionResult.success(0.95, "Groq Vision", "qwen/qwen3.6-27b", mockLabel)
        );

        ExtractionMergeService mergeService = new ExtractionMergeService();
        ImagePreprocessingService imagePreprocessingService = new ImagePreprocessingService();

        ScanService scanService = new ScanService(
                fileStorageService,
                mockOcrService,
                labelExtractionService,
                complianceEngine,
                mockScanRepository,
                objectMapper,
                mockExtractor,
                mergeService,
                imagePreprocessingService,
                aiProperties
        );

        MockMultipartFile file = new MockMultipartFile(
                "image", "cashews.jpg", "image/jpeg", createSampleJpegBytes()
        );

        ScanResponse response = scanService.processUpload(file);

        assertThat(response).isNotNull();
        assertThat(response.extractionSource()).isEqualTo("Groq Vision");
        assertThat(response.extractionStatus()).isEqualTo("AI_SUCCESS");
        assertThat(response.aiEnabled()).isTrue();
        assertThat(response.aiModel()).isEqualTo("qwen/qwen3.6-27b");
        assertThat(response.extractedLabel().productName()).isEqualTo("Organic Roasted Cashews");
        assertThat(response.extractedLabel().mrp()).isEqualTo("299.00");
        assertThat(response.extractedLabel().netQuantity()).isEqualTo("200 g");
        assertThat(response.extractedLabel().rawOcrText()).isEqualTo("Raw OCR text evidence from tesseract");

        // Assert deterministic compliance evaluation executed on AI extracted data
        assertThat(response.compliance()).isNotNull();
        assertThat(response.compliance().overallStatus()).isEqualTo(RuleStatus.PASS);
        assertThat(response.compliance().overallScore()).isGreaterThanOrEqualTo(90);

        // Verify entity was persisted with AI metadata
        verify(mockScanRepository, times(1)).save(argThat(entity ->
                "Groq Vision".equals(entity.getExtractionSource())
                        && "AI_SUCCESS".equals(entity.getExtractionStatus())
                        && "qwen/qwen3.6-27b".equals(entity.getAiModel())
        ));
    }
}


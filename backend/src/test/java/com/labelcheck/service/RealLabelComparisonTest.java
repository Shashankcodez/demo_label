package com.labelcheck.service;

import com.labelcheck.config.OcrProperties;
import com.labelcheck.dto.OcrResult;
import com.labelcheck.dto.StructuredLabelData;
import net.sourceforge.tess4j.Tesseract;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RealLabelComparisonTest {

    @Test
    void compareRealSmartphoneFoodLabel() throws Exception {
        Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
        if (!uploadsDir.toFile().exists()) {
            return;
        }

        File[] files = uploadsDir.toFile().listFiles((dir, name) -> name.endsWith(".jpg"));
        if (files == null || files.length == 0) return;

        LabelExtractionService extractionService = new LabelExtractionService();
        OcrProperties properties = new OcrProperties();
        properties.setEnabled(true);
        properties.getTesseract().setDatapath("tessdata");
        properties.getTesseract().setLanguage("eng");

        ImagePreprocessingService preprocessingService = new ImagePreprocessingService();
        TesseractOcrService improvedOcrService = new TesseractOcrService(properties, preprocessingService);
        improvedOcrService.init();

        Tesseract baselineTess = new Tesseract();
        baselineTess.setDatapath("tessdata");
        baselineTess.setLanguage("eng");
        baselineTess.setPageSegMode(3);

        com.labelcheck.compliance.ComplianceRuleEngine complianceEngine = new com.labelcheck.compliance.ComplianceRuleEngine(
                java.util.List.of(
                        new com.labelcheck.compliance.rules.MrpRule(),
                        new com.labelcheck.compliance.rules.UnitSalePriceRule(),
                        new com.labelcheck.compliance.rules.NetQuantityRule(),
                        new com.labelcheck.compliance.rules.FssaiLicenseRule(),
                        new com.labelcheck.compliance.rules.DateMarkingRule(),
                        new com.labelcheck.compliance.rules.ManufacturerRule(),
                        new com.labelcheck.compliance.rules.ConsumerCareRule(),
                        new com.labelcheck.compliance.rules.OriginRule()
                )
        );

        for (File file : files) {
            Path realImagePath = file.toPath();
            System.out.println("==================================================");
            System.out.println("REAL FOOD LABEL TEST: " + file.getName() + " (" + file.length() / 1024 + " KB)");
            System.out.println("==================================================");

            // 1. BEFORE (Baseline)
            long beforeStart = System.currentTimeMillis();
            String beforeOcrText = "";
            try {
                beforeOcrText = baselineTess.doOCR(file);
            } catch (Exception e) {
                beforeOcrText = "";
            }
            long beforeDuration = System.currentTimeMillis() - beforeStart;
            StructuredLabelData beforeExtracted = extractionService.extract(beforeOcrText);

            System.out.println("--- [BEFORE PIPELINE] ---");
            System.out.println("Time: " + beforeDuration + " ms | Chars: " + (beforeOcrText != null ? beforeOcrText.trim().length() : 0));
            System.out.println("Extracted: MRP=" + beforeExtracted.mrp() + ", NetQty=" + beforeExtracted.netQuantity() +
                    ", FSSAI=" + beforeExtracted.fssaiLicenseNumber() + ", Mfg=" + beforeExtracted.manufacturerName() +
                    ", Date=" + beforeExtracted.manufactureOrPackingDate());

            // 2. AFTER (Improved Pipeline)
            long afterStart = System.currentTimeMillis();
            OcrResult afterOcrResult = improvedOcrService.extractText(realImagePath);
            long afterDuration = System.currentTimeMillis() - afterStart;
            StructuredLabelData afterExtracted = extractionService.extract(afterOcrResult.text());
            com.labelcheck.compliance.ComplianceResult afterCompliance = complianceEngine.evaluate(afterExtracted);

            long warnCount = afterCompliance.checks().stream().filter(c -> c.status() == com.labelcheck.compliance.RuleStatus.WARNING).count();
            long violCount = afterCompliance.checks().stream().filter(c -> c.status() == com.labelcheck.compliance.RuleStatus.VIOLATION).count();

            System.out.println("--- [AFTER PIPELINE (eng)] ---");
            System.out.println("Time: " + afterDuration + " ms | Chars: " + afterOcrResult.text().length());
            System.out.println("Extracted: MRP=" + afterExtracted.mrp() + ", NetQty=" + afterExtracted.netQuantity() +
                    ", FSSAI=" + afterExtracted.fssaiLicenseNumber() + ", Mfg=" + afterExtracted.manufacturerName() +
                    ", Date=" + afterExtracted.manufactureOrPackingDate() +
                    ", ConsumerCare=" + (afterExtracted.customerCarePhone() != null ? afterExtracted.customerCarePhone() : afterExtracted.customerCareEmail()));
            System.out.println("Compliance: Status=" + afterCompliance.overallStatus() + ", Score=" + afterCompliance.overallScore() +
                    "/100, Warnings=" + warnCount + ", Violations=" + violCount);

            // 3. MULTILINGUAL (eng+hin)
            long multiStart = System.currentTimeMillis();
            OcrResult multiOcrResult = improvedOcrService.extractText(realImagePath, "eng+hin");
            long multiDuration = System.currentTimeMillis() - multiStart;
            StructuredLabelData multiExtracted = extractionService.extract(multiOcrResult.text());
            com.labelcheck.compliance.ComplianceResult multiCompliance = complianceEngine.evaluate(multiExtracted);

            long multiWarn = multiCompliance.checks().stream().filter(c -> c.status() == com.labelcheck.compliance.RuleStatus.WARNING).count();
            long multiViol = multiCompliance.checks().stream().filter(c -> c.status() == com.labelcheck.compliance.RuleStatus.VIOLATION).count();

            System.out.println("--- [MULTILINGUAL PIPELINE (eng+hin)] ---");
            System.out.println("Time: " + multiDuration + " ms | Chars: " + multiOcrResult.text().length());
            System.out.println("Extracted: MRP=" + multiExtracted.mrp() + ", NetQty=" + multiExtracted.netQuantity() +
                    ", FSSAI=" + multiExtracted.fssaiLicenseNumber() + ", Mfg=" + multiExtracted.manufacturerName() +
                    ", Date=" + multiExtracted.manufactureOrPackingDate() +
                    ", ConsumerCare=" + (multiExtracted.customerCarePhone() != null ? multiExtracted.customerCarePhone() : multiExtracted.customerCareEmail()));
            System.out.println("Compliance: Status=" + multiCompliance.overallStatus() + ", Score=" + multiCompliance.overallScore() +
                    "/100, Warnings=" + multiWarn + ", Violations=" + multiViol);
            System.out.println();
        }
    }
}



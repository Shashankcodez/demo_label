package com.labelcheck.service;

import com.labelcheck.config.OcrProperties;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PreprocessDebugTest {

    @Test
    void testPreprocessingAndOcr() throws Exception {
        Path imagePath = Paths.get("uploads/39f08c28-a077-40f2-b937-704491fcee81.png").toAbsolutePath();
        if (!imagePath.toFile().exists()) return;

        OcrProperties props = new OcrProperties();
        props.setEnabled(true);
        props.getTesseract().setDatapath("tessdata");
        props.setDefaultLanguage("eng");

        ImagePreprocessingService prep = new ImagePreprocessingService();
        TesseractOcrService ocr = new TesseractOcrService(props, prep);
        ocr.init();

        long start = System.currentTimeMillis();
        var result = ocr.extractText(imagePath, "eng");
        long dur = System.currentTimeMillis() - start;

        System.out.println("==================================================");
        System.out.println("CURRENT PIPELINE OCR RESULT (" + dur + " ms):");
        System.out.println("==================================================");
        System.out.println(result.text());
        System.out.println("==================================================");

        LabelExtractionService extractor = new LabelExtractionService();
        var structured = extractor.extract(result.text());
        System.out.println("CURRENT EXTRACTION RESULT:");
        System.out.println("Product Name: " + structured.productName());
        System.out.println("Net Qty: " + structured.netQuantity());
        System.out.println("MRP: " + structured.mrp() + " (incl=" + structured.mrpInclusiveOfTaxes() + ")");
        System.out.println("Packed Date: " + structured.manufactureOrPackingDate());
        System.out.println("Expiry Date: " + structured.bestBeforeOrExpiry());
        System.out.println("FSSAI: " + structured.fssaiLicenseNumber());
        System.out.println("Manufacturer: " + structured.manufacturerName());
        System.out.println("Phone: " + structured.customerCarePhone());
        System.out.println("Email: " + structured.customerCareEmail());
        System.out.println("Address: " + structured.manufacturerAddress());
    }
}

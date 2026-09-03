package com.labelcheck.service;

import net.sourceforge.tess4j.Tesseract;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TraceRealOcrTest {

    @Test
    void traceRawOcrForAppleSlice() throws Exception {
        Path imagePath = Paths.get("uploads/39f08c28-a077-40f2-b937-704491fcee81.png").toAbsolutePath();
        if (!imagePath.toFile().exists()) {
            System.out.println("IMAGE NOT FOUND: " + imagePath);
            return;
        }

        BufferedImage img = ImageIO.read(imagePath.toFile());

        Tesseract tess = new Tesseract();
        tess.setDatapath("tessdata");
        tess.setLanguage("eng");

        int[] psms = {3, 6, 11};
        for (int psm : psms) {
            tess.setPageSegMode(psm);
            String ocr = tess.doOCR(img);
            System.out.println("==================================================");
            System.out.println("RAW OCR - PSM " + psm);
            System.out.println("==================================================");
            System.out.println(ocr);
            System.out.println("--------------------------------------------------");
            System.out.println("CHECKS FOR PSM " + psm + ":");
            check(ocr, "MRP", "mrp");
            check(ocr, "150");
            check(ocr, "Applied For", "applied for");
            check(ocr, "+91 8888 720 520", "+91", "8888", "720", "520");
            check(ocr, "Packed On", "packed on");
            check(ocr, "09/05/2025");
            check(ocr, "Best Before", "best before");
            check(ocr, "14/05/2025");
            check(ocr, "Protein");
            check(ocr, "0.5 Gms");
        }
    }

    private void check(String text, String... targets) {
        String lower = text.toLowerCase();
        boolean all = true;
        for (String t : targets) {
            if (!lower.contains(t.toLowerCase())) {
                all = false;
                break;
            }
        }
        System.out.println("Contains [" + String.join(", ", targets) + "]: " + all);
    }
}

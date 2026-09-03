package com.labelcheck.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class AppleSliceTest {

    @Test
    void testAppleSliceOcrExploration() throws Exception {
        Path imagePath = Paths.get("uploads/39f08c28-a077-40f2-b937-704491fcee81.png").toAbsolutePath();
        if (!imagePath.toFile().exists()) {
            System.out.println("Image not found: " + imagePath);
            return;
        }

        BufferedImage img = ImageIO.read(imagePath.toFile());
        System.out.println("Image loaded: " + img.getWidth() + "x" + img.getHeight());

        Tesseract tess = new Tesseract();
        tess.setDatapath("tessdata");
        tess.setLanguage("eng");

        int[] psms = {3, 4, 6, 11};
        for (int psm : psms) {
            tess.setPageSegMode(psm);
            long start = System.currentTimeMillis();
            String res = tess.doOCR(img);
            long dur = System.currentTimeMillis() - start;
            System.out.println("==================================================");
            System.out.println("ORIGINAL IMAGE - PSM " + psm + " (" + dur + " ms, " + res.length() + " chars)");
            System.out.println("==================================================");
            System.out.println(res);
        }
    }
}

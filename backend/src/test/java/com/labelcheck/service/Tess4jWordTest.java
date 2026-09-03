package com.labelcheck.service;

import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Tess4jWordTest {

    @Test
    void testGetWords() throws Exception {
        Path imagePath = Paths.get("uploads/39f08c28-a077-40f2-b937-704491fcee81.png").toAbsolutePath();
        if (!imagePath.toFile().exists()) return;

        BufferedImage img = ImageIO.read(imagePath.toFile());
        Tesseract tess = new Tesseract();
        tess.setDatapath("tessdata");
        tess.setLanguage("eng");

        List<Word> words = tess.getWords(img, ITessAPI.TessPageIteratorLevel.RIL_WORD);
        System.out.println("Total words extracted: " + words.size());

        for (int i = 0; i < Math.min(30, words.size()); i++) {
            Word w = words.get(i);
            System.out.println(String.format("Word: '%s', Conf: %.1f, BBox: [x=%d, y=%d, w=%d, h=%d]",
                    w.getText(), w.getConfidence(),
                    w.getBoundingBox().x, w.getBoundingBox().y,
                    w.getBoundingBox().width, w.getBoundingBox().height));
        }

        System.out.println("\nWords between y=1400 and 1900:");
        words.stream()
                .filter(w -> w.getBoundingBox().y >= 1400 && w.getBoundingBox().y <= 1900)
                .sorted((a, b) -> {
                    int dy = a.getBoundingBox().y - b.getBoundingBox().y;
                    if (Math.abs(dy) < 25) {
                        return a.getBoundingBox().x - b.getBoundingBox().x;
                    }
                    return dy;
                })
                .forEach(w -> System.out.println(String.format("  [y=%d, x=%d, h=%d] '%s' (conf: %.1f)",
                        w.getBoundingBox().y, w.getBoundingBox().x, w.getBoundingBox().height,
                        w.getText(), w.getConfidence())));
    }
}

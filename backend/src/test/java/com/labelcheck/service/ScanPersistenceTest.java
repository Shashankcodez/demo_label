package com.labelcheck.service;

import com.labelcheck.compliance.ComplianceResult;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.PageResponse;
import com.labelcheck.dto.ScanResponse;
import com.labelcheck.dto.ScanSummaryResponse;
import com.labelcheck.dto.StructuredLabelData;
import com.labelcheck.exception.ResourceNotFoundException;
import com.labelcheck.repository.ScanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ScanPersistenceTest {

    @Autowired
    private ScanService scanService;

    @Autowired
    private ScanRepository scanRepository;

    private static byte[] createSamplePngBytes() throws IOException {
        BufferedImage image = new BufferedImage(30, 30, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    @Test
    @DisplayName("A. Save successful scan persists entity and can be retrieved by scanId")
    void saveSuccessfulScan_persistsInDatabase() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "persisted_test.png", "image/png", createSamplePngBytes()
        );

        ScanResponse scanResponse = scanService.processUpload(file);
        assertThat(scanResponse).isNotNull();
        assertThat(scanResponse.scanId()).isNotNull();
        assertThat(scanResponse.createdAt()).isNotNull();

        // Retrieve by scanId
        ScanResponse retrieved = scanService.getScanByScanId(scanResponse.scanId());
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.scanId()).isEqualTo(scanResponse.scanId());
        assertThat(retrieved.filename()).isEqualTo(scanResponse.filename());
        assertThat(retrieved.status()).isEqualTo(scanResponse.status());
        assertThat(retrieved.compliance()).isNotNull();
    }

    @Test
    @DisplayName("B. Non-existent scanId throws ResourceNotFoundException")
    void getScanByScanId_unknownId_throwsResourceNotFound() {
        UUID unknown = UUID.randomUUID();
        assertThatThrownBy(() -> scanService.getScanByScanId(unknown))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("C. History pagination supports page and size, clamping max size to 100")
    void getScanHistory_paginationAndClamping() throws Exception {
        // Create 3 scans
        for (int i = 0; i < 3; i++) {
            MockMultipartFile file = new MockMultipartFile(
                    "image", "page_test_" + i + ".png", "image/png", createSamplePngBytes()
            );
            scanService.processUpload(file);
        }

        PageResponse<ScanSummaryResponse> pageResponse = scanService.getScanHistory(0, 2);
        assertThat(pageResponse.content()).hasSize(2);
        assertThat(pageResponse.size()).isEqualTo(2);
        assertThat(pageResponse.totalElements()).isGreaterThanOrEqualTo(3);

        // Clamping test: size = 500 should be clamped to 100
        PageResponse<ScanSummaryResponse> clamped = scanService.getScanHistory(0, 500);
        assertThat(clamped.size()).isEqualTo(100);
    }

    @Test
    @DisplayName("D. History ordering returns scans newest-first (createdAt DESC)")
    void getScanHistory_newestFirstOrdering() throws Exception {
        MockMultipartFile file1 = new MockMultipartFile("image", "first.png", "image/png", createSamplePngBytes());
        ScanResponse res1 = scanService.processUpload(file1);

        Thread.sleep(10); // Ensure distinct timestamps

        MockMultipartFile file2 = new MockMultipartFile("image", "second.png", "image/png", createSamplePngBytes());
        ScanResponse res2 = scanService.processUpload(file2);

        PageResponse<ScanSummaryResponse> page = scanService.getScanHistory(0, 10);
        assertThat(page.content()).isNotEmpty();

        // The most recently created should appear before earlier scans
        int idx1 = -1;
        int idx2 = -1;
        for (int i = 0; i < page.content().size(); i++) {
            if (page.content().get(i).scanId().equals(res1.scanId())) idx1 = i;
            if (page.content().get(i).scanId().equals(res2.scanId())) idx2 = i;
        }

        assertThat(idx2).isLessThan(idx1);
    }
}

package com.labelcheck.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", () -> tempDir.toString());
    }

    private static byte[] createSampleJpegBytes() throws IOException {
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }

    private static byte[] createSamplePngBytes() throws IOException {
        BufferedImage image = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }

    private static byte[] createSampleWebpBytes() {
        byte[] vp8Payload = new byte[]{
                (byte) 0x30, (byte) 0x01, (byte) 0x00,
                (byte) 0x9D, (byte) 0x01, (byte) 0x2A,
                (byte) 0x0A, (byte) 0x00,
                (byte) 0x0A, (byte) 0x00
        };
        int fileSize = 4 + 8 + vp8Payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(8 + fileSize).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put("RIFF".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(fileSize);
        buffer.put("WEBP".getBytes(StandardCharsets.US_ASCII));
        buffer.put("VP8 ".getBytes(StandardCharsets.US_ASCII));
        buffer.putInt(vp8Payload.length);
        buffer.put(vp8Payload);
        return buffer.array();
    }

    @Test
    @DisplayName("A. Successful JPEG upload stores file and returns 200 with scanId")
    void uploadJpeg_shouldSucceedAndStoreFile() throws Exception {
        byte[] jpegBytes = createSampleJpegBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "label-front.jpg",
                "image/jpeg",
                jpegBytes
        );

        String responseString = mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").isNotEmpty())
                .andExpect(jsonPath("$.filename").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value("image/jpeg"))
                .andExpect(jsonPath("$.sizeBytes").value(jpegBytes.length))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andReturn().getResponse().getContentAsString();

        // Verify that the file was actually stored on disk in the isolated temp directory
        assertThat(Files.list(tempDir).count()).isGreaterThan(0);
        assertThat(responseString).doesNotContain(tempDir.toString());
    }

    @Test
    @DisplayName("B. Successful PNG upload stores file and returns 200 with scanId")
    void uploadPng_shouldSucceedAndStoreFile() throws Exception {
        byte[] pngBytes = createSamplePngBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "label-nutrition.png",
                "image/png",
                pngBytes
        );

        mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(pngBytes.length));
    }

    @Test
    @DisplayName("C. Successful WebP upload with valid RIFF/VP8 structure is accepted")
    void uploadWebp_shouldSucceedWithValidStructure() throws Exception {
        byte[] webpBytes = createSampleWebpBytes();
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "label-declaration.webp",
                "image/webp",
                webpBytes
        );

        mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value("image/webp"))
                .andExpect(jsonPath("$.sizeBytes").value(webpBytes.length));
    }

    @Test
    @DisplayName("D. Missing 'image' multipart field returns HTTP 400 with structured ErrorResponse")
    void missingImageField_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/scan"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Missing Required Field"))
                .andExpect(jsonPath("$.message").value("Required multipart field 'image' is missing in the request"))
                .andExpect(jsonPath("$.path").value("/api/v1/scan"));
    }

    @Test
    @DisplayName("E. Empty image file returns HTTP 400 with structured ErrorResponse")
    void emptyFile_shouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Image"))
                .andExpect(jsonPath("$.message").value("Uploaded image file is missing or empty"))
                .andExpect(jsonPath("$.path").value("/api/v1/scan"));
    }

    @Test
    @DisplayName("F. Unsupported MIME type (e.g. PDF or text/plain) returns HTTP 400")
    void unsupportedMimeType_shouldReturnBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "document.pdf",
                "application/pdf",
                "%PDF-1.4 dummy binary".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Image"))
                .andExpect(jsonPath("$.message").value("Unsupported media type: 'application/pdf'. Supported image formats are JPEG, PNG, and WebP."))
                .andExpect(jsonPath("$.path").value("/api/v1/scan"));
    }

    @Test
    @DisplayName("G. Renamed text file pretending to be JPEG is rejected via decoding inspection")
    void renamedTextFileAsJpeg_shouldBeRejectedByContentValidation() throws Exception {
        byte[] fakeBytes = "Hello this is a plain text file renamed to fake.jpg".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "fake.jpg",
                "image/jpeg",
                fakeBytes
        );

        mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Image"))
                .andExpect(jsonPath("$.message").value("File content does not match JPEG signature"))
                .andExpect(jsonPath("$.path").value("/api/v1/scan"));
    }

    @Test
    @DisplayName("H. Fake file with WebP MIME but corrupted header is rejected")
    void fakeWebpContent_shouldBeRejectedByStructureValidation() throws Exception {
        byte[] fakeBytes = "NOT_A_WEBP_RIFF_CONTAINER_PAYLOAD_HERE".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "fake.webp",
                "image/webp",
                fakeBytes
        );

        mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Invalid Image"))
                .andExpect(jsonPath("$.message").value("File content does not match WebP RIFF container header"))
                .andExpect(jsonPath("$.path").value("/api/v1/scan"));
    }

    @Test
    @DisplayName("I. Full pipeline test: Upload image with label text and verify OCR, extraction, and compliance")
    void uploadLabelImage_extractsOcrTextSuccessfully() throws Exception {
        BufferedImage img = new BufferedImage(500, 100, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = img.createGraphics();
        g2d.setColor(java.awt.Color.WHITE);
        g2d.fillRect(0, 0, 500, 100);
        g2d.setColor(java.awt.Color.BLACK);
        g2d.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 24));
        g2d.drawString("MRP Rs 99 Net Wt 250g", 20, 60);
        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        byte[] bytes = baos.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "image", "label_product.jpg", "image/jpeg", bytes
        );

        mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("ANALYSIS_COMPLETE"))
                .andExpect(jsonPath("$.text").isNotEmpty())
                .andExpect(jsonPath("$.extractedLabel.mrp").value("99"))
                .andExpect(jsonPath("$.extractedLabel.netQuantity").value("250g"))
                .andExpect(jsonPath("$.compliance.overallStatus").isNotEmpty())
                .andExpect(jsonPath("$.compliance.overallScore").isNumber())
                .andExpect(jsonPath("$.compliance.checks").isArray())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("J. GET /api/v1/scans/{scanId} returns 404 for unknown scanId")
    void getScanById_unknownId_returns404() throws Exception {
        java.util.UUID unknownId = java.util.UUID.randomUUID();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/scans/" + unknownId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    @DisplayName("K. GET /api/v1/scans returns paginated scan history and GET /api/v1/scans/{scanId} returns full record")
    void getScansHistoryAndDetail_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "history_test.png", "image/png", createSamplePngBytes()
        );

        String responseJson = mockMvc.perform(multipart("/api/v1/scan").file(file))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseJson);
        String scanIdStr = root.get("scanId").asText();

        // 1. Verify GET /api/v1/scans
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/scans")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.content[0].scanId").isNotEmpty())
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());

        // 2. Verify GET /api/v1/scans/{scanId}
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/scans/" + scanIdStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").value(scanIdStr))
                .andExpect(jsonPath("$.filename").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.compliance").isNotEmpty())
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("L. GET /api/v1/ocr/languages returns list of installed language models")
    void getOcrLanguages_returnsInstalledLanguages() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/ocr/languages"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@ == 'eng')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@ == 'hin')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@ == 'tam')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@ == 'tel')]").isNotEmpty());
    }

    @Test
    @DisplayName("M. POST /api/v1/scan with language parameter processes correctly")
    void uploadScan_withLanguageParameter_succeeds() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "image", "multilingual_upload.png", "image/png", createSamplePngBytes()
        );

        mockMvc.perform(multipart("/api/v1/scan")
                        .file(file)
                        .param("language", "eng+hin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scanId").isNotEmpty())
                .andExpect(jsonPath("$.status").isNotEmpty())
                .andExpect(jsonPath("$.language").value("eng+hin"));
    }
}


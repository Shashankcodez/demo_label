package com.labelcheck.controller;

import com.labelcheck.dto.PageResponse;
import com.labelcheck.dto.ScanResponse;
import com.labelcheck.dto.ScanSummaryResponse;
import com.labelcheck.service.ScanService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * REST Controller exposing label scanning ingestion, local OCR extraction,
 * statutory compliance screening, and persistent scan history endpoints.
 */
@RestController
@RequestMapping("/api/v1")
public class ScanController {

    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    /**
     * Ingests, validates, temporarily stores, executes local OCR, parses statutory entities,
     * evaluates compliance rules, and persists the analysis record.
     *
     * @param image the multipart file containing the packaged product label photograph
     * @param language optional requested OCR language code or combination (e.g. "eng+hin")
     * @return HTTP 200 with ScanResponse containing scanId, status, structured label, and compliance checks
     */
    @PostMapping(value = "/scan", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ScanResponse> uploadScan(
            @RequestParam("image") MultipartFile image,
            @RequestParam(value = "language", required = false) String language
    ) {
        ScanResponse response = scanService.processUpload(image, language);
        return ResponseEntity.ok(response);
    }

    /**
     * Discovers all supported and locally installed OCR languages.
     *
     * @return HTTP 200 with set of available language codes
     */
    @GetMapping("/ocr/languages")
    public ResponseEntity<java.util.Set<String>> getSupportedLanguages() {
        return ResponseEntity.ok(scanService.getSupportedLanguages());
    }


    /**
     * Retrieves paginated scan history ordered newest-first by creation timestamp.
     *
     * @param page page number (0-indexed, default 0)
     * @param size page size (default 20, max 100)
     * @return HTTP 200 with PageResponse containing lightweight ScanSummaryResponse records
     */
    @GetMapping("/scans")
    public ResponseEntity<PageResponse<ScanSummaryResponse>> getScanHistory(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        PageResponse<ScanSummaryResponse> history = scanService.getScanHistory(page, size);
        return ResponseEntity.ok(history);
    }

    /**
     * Retrieves full stored scan analysis details by public scanId.
     *
     * @param scanId the unique scan UUID
     * @return HTTP 200 with complete ScanResponse, or 404 if not found
     */
    @GetMapping("/scans/{scanId}")
    public ResponseEntity<ScanResponse> getScanById(
            @PathVariable("scanId") UUID scanId
    ) {
        ScanResponse scan = scanService.getScanByScanId(scanId);
        return ResponseEntity.ok(scan);
    }
}

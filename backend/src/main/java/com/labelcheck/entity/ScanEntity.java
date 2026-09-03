package com.labelcheck.entity;

import com.labelcheck.compliance.RuleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA Entity representing a completed label scan analysis and its statutory compliance evaluation.
 * Stores indexed operational columns for fast querying and history listing,
 * while serializing detailed structured label entities and compliance rule sets as JSON CLOBs.
 */
@Entity
@Table(name = "scans", indexes = {
        @Index(name = "idx_scan_id", columnList = "scan_id", unique = true),
        @Index(name = "idx_created_at", columnList = "created_at")
})
public class ScanEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "scan_id", nullable = false, unique = true, updatable = false)
    private UUID scanId;

    @Column(name = "filename", nullable = false, length = 255)
    private String filename;

    @Column(name = "content_type", nullable = false, length = 64)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "status", nullable = false, length = 64)
    private String status;

    @Lob
    @Column(name = "ocr_text", columnDefinition = "CLOB")
    private String ocrText;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 32)
    private RuleStatus overallStatus;

    @Column(name = "overall_score", nullable = false)
    private int overallScore;

    @Column(name = "summary", length = 1000)
    private String summary;

    @Column(name = "product_name", length = 255)
    private String productName;

    @Column(name = "brand", length = 255)
    private String brand;

    @Column(name = "net_quantity", length = 128)
    private String netQuantity;

    @Column(name = "mrp", length = 64)
    private String mrp;

    @Lob
    @Column(name = "extracted_label_json", columnDefinition = "CLOB")
    private String extractedLabelJson;

    @Lob
    @Column(name = "compliance_result_json", columnDefinition = "CLOB")
    private String complianceResultJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public ScanEntity() {
    }

    public ScanEntity(
            UUID scanId,
            String filename,
            String contentType,
            long sizeBytes,
            String status,
            String ocrText,
            RuleStatus overallStatus,
            int overallScore,
            String summary,
            String productName,
            String brand,
            String netQuantity,
            String mrp,
            String extractedLabelJson,
            String complianceResultJson
    ) {
        this.scanId = scanId;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.status = status;
        this.ocrText = ocrText;
        this.overallStatus = overallStatus;
        this.overallScore = overallScore;
        this.summary = summary;
        this.productName = productName;
        this.brand = brand;
        this.netQuantity = netQuantity;
        this.mrp = mrp;
        this.extractedLabelJson = extractedLabelJson;
        this.complianceResultJson = complianceResultJson;
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public UUID getScanId() {
        return scanId;
    }

    public void setScanId(UUID scanId) {
        this.scanId = scanId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public RuleStatus getOverallStatus() {
        return overallStatus;
    }

    public void setOverallStatus(RuleStatus overallStatus) {
        this.overallStatus = overallStatus;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getNetQuantity() {
        return netQuantity;
    }

    public void setNetQuantity(String netQuantity) {
        this.netQuantity = netQuantity;
    }

    public String getMrp() {
        return mrp;
    }

    public void setMrp(String mrp) {
        this.mrp = mrp;
    }

    public String getExtractedLabelJson() {
        return extractedLabelJson;
    }

    public void setExtractedLabelJson(String extractedLabelJson) {
        this.extractedLabelJson = extractedLabelJson;
    }

    public String getComplianceResultJson() {
        return complianceResultJson;
    }

    public void setComplianceResultJson(String complianceResultJson) {
        this.complianceResultJson = complianceResultJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}

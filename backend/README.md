# LabelCheck Backend (SIH26034)

Backend service for **LabelCheck** — AI-Powered Packaged Product Compliance Scanner developed for **Smart India Hackathon 2026 (Problem Statement SIH26034)**.

This service performs automated statutory screening of packaged food and commodity labels against statutory Indian labeling mandates, specifically:
1. **Department of Consumer Affairs** — *Legal Metrology (Packaged Commodities) Rules, 2011* (including 2017 amendments and the 2021/2022 Unit Sale Price amendments).
2. **Food Safety and Standards Authority of India (FSSAI)** — *Food Safety and Standards (Labelling and Display) Regulations, 2020* (including current compendia).

---

## 1. Technology Stack

- **Language:** Java 21 (LTS)
- **Framework:** Spring Boot 3.4.3
- **Persistence:** Spring Data JPA + Hibernate 6.6.x
- **Database Engine:** Embedded H2 Database (File-based local storage in development; in-memory for automated tests)
- **OCR Engine:** Tesseract 5.4.x via Tess4J 5.14.0 (Local native JNA execution, zero external API keys or cloud bills)
- **Extraction Engine:** Deterministic regex & statutory pattern parser
- **Rule Engine:** Modular Java SPI rule engine (Legal Metrology & FSSAI)
- **Build Tool:** Apache Maven 3.9+ (includes Maven Wrapper `mvnw` and `mvnw.cmd`)

---

## 2. End-to-End Processing Pipeline

```
Client Image Upload (POST /api/v1/scan)
       ↓
Multipart & Content Binary Validation (JPEG, PNG, WebP)
       ↓
Secure Storage (Preserves original photograph in uploads/ with sanitized UUID filename)
       ↓
Image Preprocessing (Smart upscaling, grayscale, contrast stretch)
       ↓
Local Tesseract OCR (Tess4J native in-memory execution)
       ↓
Deterministic Label Extraction (MRP, USP, Net Qty, FSSAI, Dates, Consumer Care, Manufacturer, Origin)
       ↓
Statutory Compliance Rule Engine (Evaluates Legal Metrology & FSSAI rules)
       ↓
Database Persistence (Saves ScanEntity to file-based H2 database in ./data/labelcheck)
       ↓
Unified Scan & Compliance Response DTO (Includes createdAt timestamp)
```

---

## 3. Database Persistence & Architecture (Step 6)

### Embedded Relational Storage
- **Development Profile:** Local file-based H2 database stored at `./data/labelcheck.mv.db`. Scan records, structured declarations, and statutory compliance evaluations survive backend application restarts without requiring external database servers.
- **Test Profile:** Automated tests run against an isolated in-memory database (`jdbc:h2:mem:labelcheck-test`), guaranteeing fast, isolated, and reproducible test runs that never mutate development data.

### Entity & Table Design (`scans`)

| Column | Type | Description |
| :--- | :--- | :--- |
| `id` | `BIGINT AUTO_INCREMENT` | Internal primary key. |
| `scan_id` | `UUID UNIQUE NOT NULL` | Public scan UUID used by client applications. Indexed for fast lookup. |
| `filename` | `VARCHAR(255) NOT NULL` | Sanitized server filename (e.g. `<uuid>.jpg`). Never exposes physical directory path. |
| `content_type` | `VARCHAR(64) NOT NULL` | Validated MIME type (`image/jpeg`, `image/png`, `image/webp`). |
| `size_bytes` | `BIGINT NOT NULL` | Uploaded image size in bytes. |
| `status` | `VARCHAR(64) NOT NULL` | Pipeline status (`ANALYSIS_COMPLETE`, `OCR_NO_TEXT`). |
| `ocr_text` | `CLOB` | Unmodified raw text extracted by local Tesseract OCR. |
| `overall_status` | `VARCHAR(32) NOT NULL` | Overall statutory screening status (`PASS`, `WARNING`, `VIOLATION`). |
| `overall_score` | `INT NOT NULL` | Aggregated compliance score (0–100). |
| `summary` | `VARCHAR(1000)` | Concise statutory screening summary. |
| `product_name` | `VARCHAR(255)` | Common or generic product name if detected. |
| `brand` | `VARCHAR(255)` | Brand name if detected. |
| `net_quantity` | `VARCHAR(128)` | Net quantity declaration (e.g. `150 g`). |
| `mrp` | `VARCHAR(64)` | Numerical MRP (e.g. `65.00`). |
| `extracted_label_json` | `CLOB` | Full JSON serialized `StructuredLabelData` record. |
| `compliance_result_json`| `CLOB` | Full JSON serialized `ComplianceResult` with all individual rule checks. |
| `created_at` | `TIMESTAMP NOT NULL` | Creation timestamp. Indexed for reverse-chronological sorting. |

---

## 4. API Endpoints

### `POST /api/v1/scan`
- **Method:** `POST`
- **Consumes:** `multipart/form-data`
- **Field Name:** `image`
- **Supported Formats:** `image/jpeg`, `image/png`, `image/webp` (Max 10MB)

---

### `GET /api/v1/scans` (Scan History)
Retrieves paginated, lightweight history records ordered **newest-first** (`createdAt DESC`). Heavy OCR text and detailed rule check sets are omitted to keep responses lightweight.

- **Query Parameters:**
  - `page` *(optional, default: `0`)*: Zero-indexed page number.
  - `size` *(optional, default: `20`, max: `100`)*: Number of records per page. Values over 100 are automatically clamped to 100.

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/v1/scans?page=0&size=10"
```

#### Example Response (`200 OK`):
```json
{
  "content": [
    {
      "scanId": "b15a8361-deaa-42c7-82d3-54f757f53f0a",
      "filename": "b15a8361-deaa-42c7-82d3-54f757f53f0a.jpg",
      "productName": null,
      "brand": null,
      "status": "ANALYSIS_COMPLETE",
      "overallStatus": "PASS",
      "overallScore": 100,
      "summary": "All 8 statutory packaging declarations verified in accordance with Legal Metrology & FSSAI standards.",
      "createdAt": "2026-09-02T19:00:38.879891Z"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

---

### `GET /api/v1/scans/{scanId}` (Single Scan Detail)
Retrieves the complete stored scan analysis record, including raw OCR text, structured entity declarations, statutory rule checks, and creation timestamp.

- **Path Variable:**
  - `scanId` *(UUID)*: The public scan identifier.

#### Example Request:
```bash
curl -X GET "http://localhost:8080/api/v1/scans/b15a8361-deaa-42c7-82d3-54f757f53f0a"
```

#### Example Response (`200 OK`):
```json
{
  "scanId": "b15a8361-deaa-42c7-82d3-54f757f53f0a",
  "filename": "b15a8361-deaa-42c7-82d3-54f757f53f0a.jpg",
  "contentType": "image/jpeg",
  "sizeBytes": 45081,
  "status": "ANALYSIS_COMPLETE",
  "text": "MRP Rs 65.00 (incl. of all taxes)\nUnit Sale Price: Rs 0.43 perg\nNet Quantity: 150 g\nPKD: 09/2026\nManufactured by Tasty Snacks Pvt Ltd\nFSSAI Lic. No. 10019999000123\nCustomer Helpline: 1800-999-8888\nEmail: feedback@tastysnacks.in",
  "ocrText": "MRP Rs 65.00 (incl. of all taxes)\nUnit Sale Price: Rs 0.43 perg\nNet Quantity: 150 g\nPKD: 09/2026\nManufactured by Tasty Snacks Pvt Ltd\nFSSAI Lic. No. 10019999000123\nCustomer Helpline: 1800-999-8888\nEmail: feedback@tastysnacks.in",
  "language": "eng",
  "message": "Stored product label analysis retrieved successfully.",
  "extractedLabel": {
    "productName": null,
    "brand": null,
    "netQuantity": "150 g",
    "mrp": "65.00",
    "mrpInclusiveOfTaxes": true,
    "unitSalePrice": "0.43 perg",
    "manufacturerName": "Tasty Snacks Pvt Ltd",
    "manufacturerAddress": null,
    "importerName": null,
    "importerAddress": null,
    "countryOfOrigin": null,
    "manufactureOrPackingDate": "09/2026",
    "bestBeforeOrExpiry": null,
    "fssaiLicenseNumber": "10019999000123",
    "customerCarePhone": "1800-999-8888",
    "customerCareEmail": "feedback@tastysnacks.in",
    "customerCareAddress": null,
    "rawOcrText": "..."
  },
  "compliance": {
    "overallStatus": "PASS",
    "overallScore": 100,
    "checks": [
      {
        "id": "RULE_MRP",
        "ruleReference": "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(e)",
        "title": "Maximum Retail Price (MRP) Declaration",
        "status": "PASS",
        "detected": "₹65.00 (incl. of all taxes)",
        "legalReason": "Maximum Retail Price (MRP) numerical declaration detected with the mandatory 'inclusive of all taxes' indication under Rule 6(1)(e).",
        "recommendation": "Ensure the price declaration remains unambiguous and unaltered across distribution channels.",
        "severity": "NONE"
      },
      {
        "id": "RULE_USP",
        "ruleReference": "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(11)",
        "title": "Unit Sale Price (USP) Declaration",
        "status": "PASS",
        "detected": "0.43 perg",
        "legalReason": "Unit Sale Price (USP) declaration detected on label in accordance with Rule 6(11).",
        "recommendation": "Ensure the Unit Sale Price is rounded off to the nearest two decimal places and clearly displayed on the principal display panel.",
        "severity": "NONE"
      },
      {
        "id": "RULE_NET_QTY",
        "ruleReference": "Legal Metrology Rules, 2011 - Rule 6(1)(c) & FSSAI Reg. 5(2)",
        "title": "Net Quantity Declaration",
        "status": "PASS",
        "detected": "150 g",
        "legalReason": "Net quantity declaration using recognizable standard metric units detected on package. Note: Minimum statutory font height and principal display panel area compliance cannot be evaluated from a 2D photograph and requires physical measurement under Schedule II.",
        "recommendation": "Confirm that the numeral and unit font size complies with Schedule II requirements corresponding to the package's principal display panel area.",
        "severity": "NONE"
      },
      {
        "id": "RULE_FSSAI",
        "ruleReference": "FSSAI (Labelling and Display) Regulations, 2020 - Regulation 5(1)",
        "title": "FSSAI Food License / Registration Number",
        "status": "PASS",
        "detected": "10019999000123",
        "legalReason": "A 14-digit numeric sequence matching the statutory FSSAI license/registration number format was detected on the label as required by Regulation 5(1). Note: Format detection verifies numerical structure only; it does not confirm active license validity, authenticity, or licensee identity on the official FoSCoS portal.",
        "recommendation": "Verify the operational status and licensee details of this 14-digit number on the official FSSAI FoSCoS portal (foscos.fssai.gov.in) before distribution.",
        "severity": "NONE"
      },
      {
        "id": "RULE_DATE_MARKING",
        "ruleReference": "Legal Metrology Rules, 2011 - Rule 6(1)(d) & FSSAI Reg. 5(6)",
        "title": "Date of Manufacture, Packing, or Expiry",
        "status": "PASS",
        "detected": "MFD/PKD: 09/2026",
        "legalReason": "Date of manufacture/packing detected (satisfying Legal Metrology Rule 6(1)(d)). For food products, an additional Expiry or Best Before date is required under FSSAI Regulation 5(6) if not already declared on another panel.",
        "recommendation": "Verify whether an Expiry or Best Before date is declared on the crimp, seal, or secondary display panel.",
        "severity": "NONE"
      },
      {
        "id": "RULE_MANUFACTURER",
        "ruleReference": "Legal Metrology Rules, 2011 - Rule 6(1)(a) & FSSAI Reg. 5(4)",
        "title": "Manufacturer / Packer / Marketer Details",
        "status": "PASS",
        "detected": "Mfg/Packer: Tasty Snacks Pvt Ltd",
        "legalReason": "Name and commercial identity of manufacturer, packer, or importer detected on the label in accordance with Rule 6(1)(a) and FSSAI Regulation 5(4).",
        "recommendation": "Ensure the complete postal address, including premises, city, state, and valid PIN code, is legibly printed on the package.",
        "severity": "NONE"
      },
      {
        "id": "RULE_CONSUMER_CARE",
        "ruleReference": "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(f)",
        "title": "Consumer Care / Grievance Redressal Mechanism",
        "status": "PASS",
        "detected": "Phone: 1800-999-8888 | Email: feedback@tastysnacks.in",
        "legalReason": "Comprehensive consumer grievance redressal contacts (both telephone number and email address) detected in accordance with Rule 6(1)(f).",
        "recommendation": "Ensure consumer helpline phone numbers and email support channels remain continuously operational.",
        "severity": "NONE"
      },
      {
        "id": "RULE_ORIGIN",
        "ruleReference": "Legal Metrology Rules, 2011 - Rule 6(10) & FSSAI Reg. 5(5)",
        "title": "Country of Origin Declaration",
        "status": "PASS",
        "detected": "Domestic (Substantiated by Manufacturer)",
        "legalReason": "For domestic commodities, country of origin is legally established by the registered domestic manufacturer address under Rule 6(1)(a). Explicit 'Made in India' wording is recommended for consumer clarity but is not a standalone packaging infraction under Rule 6(10).",
        "recommendation": "Consider printing explicit 'Made in India' or 'Country of Origin: India' for consumer transparency.",
        "severity": "NONE"
      }
    ],
    "summary": "All 8 statutory packaging declarations verified in accordance with Legal Metrology & FSSAI standards."
  },
  "createdAt": "2026-09-02T19:00:38.879891Z"
}
```

#### Example Error Response (`404 Not Found` for unknown scanId):
```json
{
  "timestamp": "2026-09-03T00:29:45.123456Z",
  "status": 404,
  "error": "Not Found",
  "message": "Scan analysis record not found for ID: 00000000-0000-0000-0000-000000000000",
  "path": "/api/v1/scans/00000000-0000-0000-0000-000000000000"
}
```

---

## 5. Automated Tests

Execute the complete automated test suite (43 passing tests across 8 test classes):

```bash
mvn clean test
# or
.\mvnw.cmd clean test
```

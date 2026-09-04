# Legal Metrology Validation Architecture (LM-PCR-2026.01)

## Architecture Principles

1. **Conservative & Evidence-Driven**: No statutory violation is declared without affirmative optical evidence. Missing text yields `WARNING` or `NOT_DETECTED`.
2. **Deterministic Evaluation**: Given the same normalized label data, the engine generates the exact same score and checks every time.
3. **Decoupled Pipeline**: Optical character recognition (Gemini Vision AI with Tesseract local fallback) is strictly separated from statutory legal rule validation via `NormalizedLabel`.
4. **Resilient & Fail-Safe**: Any isolated exception thrown during a rule check is caught and converted to `REQUIRES_MANUAL_VERIFICATION`, guaranteeing 100% pipeline uptime.
5. **Traceable & Versioned**: Every check references statutory legislation, section/rule number, and engine version `LM-PCR-2026.01`.

---

## Architectural Pipeline Flow

```
[Uploaded Image]
       │
       ▼
[Image Preprocessing & Normalization]
       │
       ▼
[Vision AI Extraction (Gemini 3.6 Flash / Flash Latest)] ──(Error/Offline)──► [Local Tesseract OCR Dual-Pass]
       │                                                                                │
       └───────────────────────────────┬────────────────────────────────────────────────┘
                                       │
                                       ▼
                        [Extraction Merging & Evidence Map]
                                       │
                                       ▼
                             [NormalizedLabel Model]
                                       │
                                       ▼
                       [Applicability Profile Generator]
                        (Retail vs Exempt, Food vs Non-Food,
                         Metric Measurement Type, Packaging)
                                       │
                                       ▼
                   [ComplianceRuleEngine (LM-PCR-2026.01)]
                        ├── RULE_MRP (Rule 6(1)(e))
                        ├── RULE_NET_QTY (Rule 6(1)(c) & Rule 12)
                        ├── RULE_USP (Rule 6(1)(e) Second Proviso)
                        ├── RULE_MANUFACTURER (Rule 6(1)(a))
                        ├── RULE_ORIGIN (Rule 6(1)(a) & 6(10))
                        ├── RULE_DATE (Rule 6(1)(d))
                        ├── RULE_CONSUMER_CARE (Rule 6(1)(da))
                        ├── RULE_FSSAI (FSSAI Reg 2020)
                        ├── RULE_GENERIC_NAME (Rule 6(1)(b))
                        ├── RULE_QUANTITY_QUALIFIER (Rule 13(5))
                        ├── RULE_FONT_SIZE (Rule 7 & Schedule II)
                        ├── RULE_LEGIBILITY (Rule 9)
                        └── RULE_EXEMPTION (Rule 26)
                                       │
                                       ▼
                      [ComplianceResult Aggregator]
                        ├── Violations List
                        ├── Warnings List
                        ├── Manual Review Items
                        ├── Passed Checks
                        ├── Transparent Score Breakdown
                        └── Legal Explanations & Advice
                                       │
                                       ▼
                         [H2 JPA Database Persistence]
                                       │
                                       ▼
                      [Frontend React Screening UI]
```

---

## Scoring Model & Transparent Weighting

- **Initial Base Score**: 100
- **Confirmed Statutory Violation (`FAIL` / `VIOLATION`)**: -25 points
- **High Severity Warning (`WARNING`)**: -15 points
- **Medium Severity Warning (`WARNING`)**: -8 points
- **Low Severity Warning (`WARNING`)**: -4 points
- **Informational / Uncalibrated Review (`REQUIRES_MANUAL_VERIFICATION`)**: 0 points deduction
- **Exempt / Not Detected (`NOT_APPLICABLE` / `NOT_DETECTED`)**: 0 points deduction
- **Score Range**: Clamped strictly between 0 and 100.

---

## Status Decision Logic

- If any confirmed statutory violation exists (`FAIL` or `VIOLATION`): Overall Status = **`VIOLATION`** / **`NON_COMPLIANT`**.
- Else if any discrepancy warning exists (`WARNING`): Overall Status = **`WARNING`** / **`NEEDS REVIEW`**.
- Else if any actionable physical verification item exists: Overall Status = **`REQUIRES_MANUAL_VERIFICATION`**.
- Otherwise: Overall Status = **`PASS`** / **`COMPLIANT`**.

# Legal Metrology Compliance Rule Matrix (LM-PCR-2026.01)

## Executive Overview

The **Legal Metrology Compliance Evaluation Engine (LM-PCR-2026.01)** serves as an automated statutory screening and decision-support system under:
- **The Legal Metrology Act, 2009 (Act No. 1 of 2010)**
- **The Legal Metrology (Packaged Commodities) Rules, 2011** (incorporating 2017, 2021, and 2022 amendments)
- **The Food Safety and Standards Act, 2006** and **Food Safety and Standards (Labelling and Display) Regulations, 2020**

The engine is engineered according to the fundamental administrative jurisprudence principle:
> **Automated screening tools must NEVER invent or fabricate a legal violation.** Missing visual context, unphotographed panels, low image resolution, or absent optical millimeter calibration targets must result in `WARNING`, `NOT_DETECTED`, or `REQUIRES_MANUAL_VERIFICATION`, never automatic statutory non-compliance (`FAIL` / `VIOLATION`).

---

## 1. Statutory Rule Catalog & Evaluation Matrix

| Rule Identifier | Statutory Act / Regulation Citation | Rule Title & Mandate | Target Data Fields | Deterministic Pass Condition | Non-Compliance (FAIL/VIOLATION) Condition | Manual Review / Warning Condition | Severity & Weight |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **RULE_MRP** | Legal Metrology Rules, 2011 — Rule 6(1)(e) | Maximum Retail Price (MRP) Declaration | `mrp`, `mrpInclusiveOfTaxes` | Numerals > 0 declared with explicit "inclusive of all taxes" statement. | MRP numerals <= 0, or altered/overwritten price markings. | Not detected in single photograph (WARNING: Low/Medium); Price detected without explicit tax statement (WARNING: Low). | High (25 pts deduction on FAIL) |
| **RULE_NET_QTY** | Legal Metrology Rules, 2011 — Rule 6(1)(c) & Rule 12; Schedule I | Net Quantity & Standard Units Declaration | `netQuantity`, `netQuantityNumeric`, `netQuantityUnit` | Net quantity declared using standard SI metric units (g, kg, ml, l, L, m, N). | Prohibited unit symbols or negative/zero measure. | Not detected on scanned panel (WARNING: Medium); Non-standard unit abbreviations (WARNING: Low). | Medium (15 pts on WARNING; 25 on FAIL) |
| **RULE_USP** | Legal Metrology Rules, 2011 — Rule 6(1)(e) Second Proviso (Amend. 2021) | Unit Sale Price (USP) Declaration | `unitSalePrice`, `netQuantity` | USP declared in rupees per unit (per g, kg, ml, l, m, N) rounded to 2 decimals. | Negative or conflicting calculation where net qty > 1kg/1L. | Not detected on single panel (WARNING: Low); Exempt if package net qty <= 10g/ml or retail price = unit price. | Low (8 pts on WARNING) |
| **RULE_MANUFACTURER** | Legal Metrology Rules, 2011 — Rule 6(1)(a); FSSAI Reg. 5(4) | Manufacturer / Packer / Importer Name & Complete Address | `manufacturerName`, `manufacturerAddress`, `importerName`, `importerAddress` | Legal identity and registered address detected with premises/city/PIN code. | Blank identity where retail commercial distribution is asserted. | Not detected on current panel (WARNING: Medium). Physical package review recommended. | Medium (8 pts on WARNING) |
| **RULE_ORIGIN** | Legal Metrology Rules, 2011 — Rule 6(1)(a) & Rule 6(10); FSSAI Reg. 5(5) | Country of Origin Declaration | `countryOfOrigin`, `importerName`, `manufacturerName` | Explicit country of origin detected (e.g. "India") or legally established by domestic manufacturer. | Explicit foreign goods lacking Country of Origin on commercial packaging. | Importer detected without explicit country of origin (WARNING: High); Not detected in domestic photograph (WARNING: Low). | High for imports (15 pts on WARNING); None for verified domestic |
| **RULE_DATE** | Legal Metrology Rules, 2011 — Rule 6(1)(d); FSSAI Reg. 5(6) | Month and Year of Manufacture / Packing / Import | `manufactureOrPackingDate`, `bestBeforeOrExpiry` | Date of manufacture/packing or expiry declared in statutory MM/YY or DD/MM/YY format. | Contradictory dates (e.g., expiry date precedes manufacture date). | Missing MFD or Expiry on crimp/seal (WARNING: Medium). Visual inspection of lid/crimp recommended. | Medium (8 pts on WARNING) |
| **RULE_CONSUMER_CARE** | Legal Metrology Rules, 2011 — Rule 6(1)(da) | Consumer Care Grievance Redressal Mechanism | `customerCarePhone`, `customerCareEmail`, `customerCareAddress` | Dual contact channels (both phone helpline and email/address) detected. | Explicit refusal or fraudulent contact format. | Telephone detected without email (WARNING: Low); Neither detected on photographed facet (WARNING: Low). | Low (4 pts on WARNING) |
| **RULE_FSSAI** | Food Safety & Standards Act, 2006, Sec 31; FSS (Labelling) Reg 2020 | FSSAI 14-Digit License Display & FoSCoS Status | `fssaiLicenseNumber`, `fssaiStatus` | Valid 14-digit numeric sequence detected with FSSAI logo. FoSCoS verification advised. | Counterfeit or distorted format where food category requires licensing. | License marked "Applied For" (WARNING: Medium); Not detected on non-food or single panel (WARNING: Medium). | Medium (8 pts on WARNING) |
| **RULE_GENERIC_NAME** | Legal Metrology Rules, 2011 — Rule 6(1)(b) & Rule 6(1)(l) | Common or Generic Name of Commodity | `genericName`, `productName`, `brand` | Clear descriptive commodity name (e.g. "Refined Sunflower Oil", "Biscuits") distinct from brand. | Sole trademark with zero commodity descriptor in retail marketing. | Trademark detected without common commodity identifier (WARNING: Low). | Low (4 pts on WARNING) |
| **RULE_QUANTITY_QUALIFIER**| Legal Metrology Rules, 2011 — Rule 13(5) | Prohibition of Misleading Quantity Qualifiers | `netQuantity`, `rawOcrText` | Definite net quantity stated without misleading qualifying adjectives. | Expression "when packed", "approximate", "approx.", "minimum", or "not less than" adjacent to net quantity. | Ambiguous text snippet adjacent to net quantity numerals (WARNING: Low). | High (25 pts deduction on FAIL) |
| **RULE_FONT_SIZE** | Legal Metrology Rules, 2011 — Rule 7 & Schedule II | Minimum Font Height (Schedule II Table) | `netQuantityNumeric`, optical calibration target | Verified against millimeter scale (1.0mm, 2.0mm, 4.0mm, 6.0mm). | Never declared from 2D uncalibrated photograph alone. | Uncalibrated photo scale (REQUIRES_MANUAL_VERIFICATION: Info). Requires physical optical gauge. | Info (0 pts deduction; prevents false conviction) |
| **RULE_LEGIBILITY** | Legal Metrology Rules, 2011 — Rule 9 | General Prominence, Legibility, and Contrast | `rawOcrText`, detected fields count | Text conspicuously printed with clear optical contrast against background. | Severe superimposition of price over net quantity numerals. | Blur, lighting glare, or low contrast (WARNING: Low / REQUIRES_MANUAL_VERIFICATION). No WCAG AAA enforcement. | Info / Low (0-4 pts) |
| **RULE_EXEMPTION** | Legal Metrology Rules, 2011 — Rule 26 | Statutory Scope & Exemption Verification | `netQuantityNumeric`, package usage declarations | Standard retail scope verified, or statutory exemption under Rule 26(a)-(f) confirmed. | Retail sale of goods marked "For Institutional Use Only". | Small package (<= 10g/ml) exempt from detailed rules except MRP & date (PASS: Informative). | None (Scope classification) |

---

## 2. Statutory Negative Proof Doctrine

In legal enforcement, the absence of an element in an uncalibrated optical image of one side of a three-dimensional container **does not prove its absence** on the remaining facets (crimps, bottom stamp, lid, back panel).

Accordingly, the evaluation engine enforces:
1. **Never Invent a Violation**: A field missing from the image is recorded as `WARNING` or `NOT_DETECTED`.
2. **Deterministic Evidence Only**: Only unambiguous contradictions (e.g., negative MRP, prohibited words under Rule 13(5), or expired dates) produce `FAIL` / `VIOLATION`.
3. **Physical Scale Disclaimer**: Schedule II numeral height requirements mandate physical millimeter verification. Automated vision tools must never fabricate font infractions without physical verification.

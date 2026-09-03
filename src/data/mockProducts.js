// Pre-loaded realistic sample products compliant and non-compliant with
// Legal Metrology (Packaged Commodities) Rules 2011 and FSSAI guidelines.

export const SAMPLE_PRODUCTS = [
  {
    id: "demo-haldirams",
    name: "Haldiram's Nagpur Aloo Bhujia (150g)",
    brand: "Haldiram's",
    category: "Packaged Snacks / Savories",
    barcode: "8904004401298",
    batchNumber: "AB-260114-N",
    mrp: "₹45.00",
    unitSalePrice: "₹0.30 / g",
    netQuantity: "150 g",
    netQuantityRaw: "150 g",
    fontHeightDetected: "3.2 mm",
    fontHeightRequired: "2.0 mm",
    mfgDate: "14/01/2026",
    expiryDate: "13/07/2026 (Best before 6 months)",
    countryOfOrigin: "India",
    fssaiLicense: "10014022002725",
    manufacturer: "Haldiram Foods International Pvt. Ltd.",
    manufacturerAddress: "Plot No. 145/146, Old Pardi Naka, Bhandara Road, Nagpur, Maharashtra - 440035",
    packerDetails: "Same as Manufacturer",
    customerCare: {
      officer: "Consumer Care Executive",
      phone: "1800-209-1234",
      email: "feedback@haldirams.com",
      address: "Haldiram Foods, Old Pardi Naka, Nagpur - 440035, Maharashtra"
    },
    overallScore: 96,
    status: "PASS", // PASS | WARNING | VIOLATION
    summary: "Fully compliant with Legal Metrology (Packaged Commodities) Rules 2011 and 2021 USP amendments. All mandatory declarations clearly visible.",
    imageBadgeColor: "from-amber-600 to-orange-700",
    imageEmoji: "🥨",
    labelSnippet: "MRP ₹45.00 (INCL. OF ALL TAXES) • USP: ₹0.30/g • NET QTY: 150g • MFG: 14/01/2026 • FSSAI Lic. No. 10014022002725 • Customer Care: 1800-209-1234",
    checks: [
      {
        id: "mrp",
        rule: "Rule 6(1)(e) & 2021 Amendment",
        title: "Maximum Retail Price (MRP) & Unit Sale Price",
        status: "PASS",
        detected: "MRP ₹45.00 (inclusive of all taxes) with Unit Sale Price (USP) ₹0.30/g declared.",
        legalReason: "Complies with statutory requirement that retail price must include all taxes and specify unit sale price for packages over 100g.",
        recommendation: "No action needed. Compliant artwork.",
        severity: "none"
      },
      {
        id: "net-qty",
        rule: "Rule 6(1)(c) & Rule 7 (Schedule II)",
        title: "Net Quantity & Minimum Font Height",
        status: "PASS",
        detected: "Net Quantity '150 g' with standard metric symbol. Measured font height is 3.2mm.",
        legalReason: "Meets Schedule II requirement where minimum numeral height must be at least 2.0mm for net quantity between 100g to 200g.",
        recommendation: "No action needed. Meets all Legal Metrology standards.",
        severity: "none"
      },
      {
        id: "mfg-details",
        rule: "Rule 6(1)(a)",
        title: "Manufacturer / Packer Name & Physical Address",
        status: "PASS",
        detected: "Complete physical address with Plot No, Road, City, State, and 6-digit PIN code (440035).",
        legalReason: "Rule 6(1)(a) mandates that full address where goods are packaged/manufactured must be readable.",
        recommendation: "Properly declared.",
        severity: "none"
      },
      {
        id: "consumer-care",
        rule: "Rule 6(1)(n)",
        title: "Consumer Care & Grievance Mechanism",
        status: "PASS",
        detected: "Toll-free number (1800-209-1234), email ID (feedback@haldirams.com), postal address, and designation provided.",
        legalReason: "Full compliance with mandatory 4-point consumer grievance declaration under Rule 6(1)(n).",
        recommendation: "No action needed.",
        severity: "none"
      },
      {
        id: "date-declaration",
        rule: "Rule 6(1)(d)",
        title: "Date of Packaging & Expiry",
        status: "PASS",
        detected: "Mfg: 14/01/2026, Best before 6 months clearly specified.",
        legalReason: "Complies with month and year declaration norms.",
        recommendation: "Fully compliant.",
        severity: "none"
      },
      {
        id: "origin-fssai",
        rule: "Rule 6(10) & FSSAI Act Sec 23",
        title: "Country of Origin & FSSAI License",
        status: "PASS",
        detected: "Country of Origin: 'India' clearly declared; 14-digit FSSAI Lic. No. 10014022002725 present.",
        legalReason: "Mandatory country of origin and food safety validation verified.",
        recommendation: "No action required.",
        severity: "none"
      }
    ]
  },
  {
    id: "demo-puredrop",
    name: "PureDrop Natural Mineral Water (500ml)",
    brand: "PureDrop Beverages",
    category: "Packaged Drinking Water",
    barcode: "8906012903310",
    batchNumber: "Unreadable / Missing",
    mrp: "Rs. 20",
    unitSalePrice: "Missing",
    netQuantity: "500 ML",
    netQuantityRaw: "500 ML",
    fontHeightDetected: "1.8 mm",
    fontHeightRequired: "4.0 mm",
    mfgDate: "02/26",
    expiryDate: "Best before 6 months from mfg",
    countryOfOrigin: "Not Declared",
    fssaiLicense: "11518012000451",
    manufacturer: "PureDrop Beverages",
    manufacturerAddress: "Industrial Area, Solan (Incomplete address)",
    packerDetails: "Not specified",
    customerCare: {
      officer: "Not specified",
      phone: "Not provided",
      email: "Not provided",
      address: "For queries call us"
    },
    overallScore: 42,
    status: "VIOLATION",
    summary: "Critical non-compliance detected: Missing consumer grievance telephone and email, non-compliant font size for net quantity, and absent tax declaration on MRP.",
    imageBadgeColor: "from-blue-600 to-cyan-700",
    imageEmoji: "💧",
    labelSnippet: "PureDrop Mineral Water • Rs. 20 • 500 ML • Mfg 02/26 • For queries call us • Industrial Area Solan",
    checks: [
      {
        id: "mrp",
        rule: "Rule 6(1)(e) & 2021 Amendment",
        title: "Maximum Retail Price (MRP) & Unit Sale Price",
        status: "VIOLATION",
        detected: "Printed as 'Rs. 20'. Missing mandatory wording '(inclusive of all taxes)' and missing Unit Sale Price (USP).",
        legalReason: "Section 18 of Legal Metrology Act, 2009 & Rule 6(1)(e) requires price in format 'MRP ₹xx.xx (incl. of all taxes)' and per-ml Unit Sale Price.",
        recommendation: "Update print format to: 'MRP ₹20.00 (inclusive of all taxes)' and display USP as '₹0.04 / ml'.",
        severity: "high"
      },
      {
        id: "net-qty",
        rule: "Rule 6(1)(c) & Rule 7 (Schedule II)",
        title: "Net Quantity & Minimum Font Height",
        status: "VIOLATION",
        detected: "Declared as '500 ML'. Measured font height is only 1.8mm (minimum required is 4.0mm). Non-standard casing 'ML'.",
        legalReason: "Under Schedule-II of Legal Metrology Rules, for volume between 200ml and 1000ml, numeral height must be at least 4mm. Metric symbol must be 'ml' or 'mL'.",
        recommendation: "Increase font size of '500 ml' to at least 4.0mm on the principal display panel.",
        severity: "high"
      },
      {
        id: "consumer-care",
        rule: "Rule 6(1)(n)",
        title: "Consumer Care & Grievance Redressal Mechanism",
        status: "VIOLATION",
        detected: "Vague statement 'For queries call us'. No telephone number, email ID, or contact person provided.",
        legalReason: "Rule 6(1)(n) strictly commands that name, address, telephone number, and email of person or office to contact for complaints must be on label.",
        recommendation: "Immediately add telephone helpline number and customer care email ID. This is a compoundable offense under Section 36.",
        severity: "high"
      },
      {
        id: "mfg-details",
        rule: "Rule 6(1)(a)",
        title: "Manufacturer / Packer Physical Address",
        status: "WARNING",
        detected: "'PureDrop Beverages, Industrial Area, Solan' lacks factory plot number, district, and PIN code.",
        legalReason: "Rule 6(1)(a) requires complete postal address enabling consumers and inspection authorities to locate the registered premise.",
        recommendation: "Declare complete premise address including Shed/Plot No, Village/Phase, and 6-digit PIN code.",
        severity: "medium"
      },
      {
        id: "date-declaration",
        rule: "Rule 6(1)(d)",
        title: "Date of Packaging & Batch Number",
        status: "WARNING",
        detected: "'02/26' lacks exact packaging date and batch/lot identifier is missing or illegible.",
        legalReason: "Rule 6(1)(d) requires readable month and year of manufacture or packing, with traceable batch coding for packaged water.",
        recommendation: "Ensure clear ink-jet coding with both Month/Year and unique Batch Identification Number.",
        severity: "medium"
      },
      {
        id: "origin-fssai",
        rule: "Rule 6(10) & FSSAI Standards",
        title: "Country of Origin",
        status: "VIOLATION",
        detected: "Country of Origin not found on the label wrapper.",
        legalReason: "Rule 6(10) mandates declaration of 'Country of Origin' or 'Made in India' on all packaged commodities.",
        recommendation: "Add prominent declaration: 'Country of Origin: India'.",
        severity: "medium"
      }
    ]
  },
  {
    id: "demo-chocodelight",
    name: "ChocoDelight Hazelnut Cocoa Spread (200g)",
    brand: "ChocoDelight",
    category: "Imported Confectionery / Spreads",
    barcode: "8710200345091",
    batchNumber: "CD-9941",
    mrp: "₹280.00",
    unitSalePrice: "₹1.40 / g",
    netQuantity: "200 g",
    netQuantityRaw: "200 g",
    fontHeightDetected: "2.4 mm",
    fontHeightRequired: "2.0 mm",
    mfgDate: "11/2025",
    expiryDate: "Best before 12 months from packing",
    countryOfOrigin: "Netherlands",
    fssaiLicense: "10020011000987",
    manufacturer: "SweetTreats BV, Amsterdam, Netherlands",
    manufacturerAddress: "Keizersgracht 421, 1016 EK Amsterdam, Netherlands",
    packerDetails: "Imported & Marketed by: GlobalFoods India, Andheri East, Mumbai",
    customerCare: {
      officer: "Customer Support Cell",
      phone: "Not provided",
      email: "support@globalfoods.in",
      address: "GlobalFoods India, Andheri East, Mumbai - 400069"
    },
    overallScore: 72,
    status: "WARNING",
    summary: "Moderate compliance issues: Importer's telephone helpline missing under Rule 6(1)(n), and importer postal address lacks specific premise number.",
    imageBadgeColor: "from-amber-800 to-yellow-900",
    imageEmoji: "🍫",
    labelSnippet: "ChocoDelight Hazelnut Spread • MRP ₹280.00 (incl. taxes) • Net 200g • Mfg 11/2025 • Imported by GlobalFoods India • support@globalfoods.in",
    checks: [
      {
        id: "mrp",
        rule: "Rule 6(1)(e)",
        title: "Maximum Retail Price (MRP) & Unit Sale Price",
        status: "PASS",
        detected: "MRP ₹280.00 (inclusive of all taxes) with Unit Sale Price ₹1.40/g stated.",
        legalReason: "Full compliance with price declaration rules.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "net-qty",
        rule: "Rule 6(1)(c) & Rule 7",
        title: "Net Quantity & Font Size",
        status: "PASS",
        detected: "Net Quantity '200 g' with font height 2.4mm (exceeds 2.0mm minimum).",
        legalReason: "Standard unit and size verified.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "consumer-care",
        rule: "Rule 6(1)(n)",
        title: "Consumer Care Contact Helpline",
        status: "WARNING",
        detected: "Only email (support@globalfoods.in) declared. Mandatory telephone helpline number is missing.",
        legalReason: "Rule 6(1)(n) requires both telephone number and email address to be displayed for imported commodities.",
        recommendation: "Provide a working Indian landline or toll-free telephone number for consumer complaints.",
        severity: "medium"
      },
      {
        id: "mfg-details",
        rule: "Rule 6(1)(a) - Importer Norms",
        title: "Importer Identification & Address",
        status: "WARNING",
        detected: "'GlobalFoods India, Andheri East, Mumbai' lacks building name, unit number, and complete street address.",
        legalReason: "Importer address must be complete to verify valid registration under Rule 27 of Legal Metrology Rules.",
        recommendation: "Add complete registered Indian address with Building/Unit number and street.",
        severity: "low"
      },
      {
        id: "origin-fssai",
        rule: "Rule 6(10) & FSSAI Import Clearance",
        title: "Country of Origin & FSSAI Importer License",
        status: "PASS",
        detected: "Country of Origin 'Netherlands' clearly declared; FSSAI Importer License 10020011000987 verified.",
        legalReason: "Properly validated for import compliance.",
        recommendation: "Compliant.",
        severity: "none"
      }
    ]
  },
  {
    id: "demo-darkfantasy",
    name: "Sunfeast Dark Fantasy Choco Fills (300g)",
    brand: "ITC Limited",
    category: "Biscuits / Bakery Products",
    barcode: "8901725131024",
    batchNumber: "ITC-DF-2601",
    mrp: "₹120.00",
    unitSalePrice: "₹0.40 / g",
    netQuantity: "300 g",
    netQuantityRaw: "300 g",
    fontHeightDetected: "3.6 mm",
    fontHeightRequired: "3.0 mm",
    mfgDate: "05/01/2026",
    expiryDate: "04/07/2026 (6 Months)",
    countryOfOrigin: "India",
    fssaiLicense: "10012031000312",
    manufacturer: "ITC Limited, Foods Division",
    manufacturerAddress: "Virginia House, 37 J.L. Nehru Road, Kolkata, West Bengal - 700071",
    packerDetails: "ITC Foods Unit, Haridwar Industrial Estate, Uttarakhand - 249403",
    customerCare: {
      officer: "Executive - Consumer Care Cell",
      phone: "1800-425-4444",
      email: "itccares@itc.in",
      address: "P.O. Box No. 592, Bengaluru - 560001, Karnataka"
    },
    overallScore: 98,
    status: "PASS",
    summary: "Gold standard compliance: Exceeds Legal Metrology 2011 norms and FSSAI Front-of-Pack standards with prominent font sizes and full declarations.",
    imageBadgeColor: "from-amber-950 to-stone-900",
    imageEmoji: "🍪",
    labelSnippet: "Dark Fantasy Choco Fills • MRP ₹120.00 (INCL. TAXES) • USP ₹0.40/g • Net Qty 300g • FSSAI 10012031000312 • 1800-425-4444",
    checks: [
      {
        id: "mrp",
        rule: "Rule 6(1)(e)",
        title: "MRP & Unit Sale Price Declaration",
        status: "PASS",
        detected: "MRP ₹120.00 (inclusive of all taxes) with USP ₹0.40/g prominently stated.",
        legalReason: "Full compliance with Section 18 and 2021 USP amendments.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "net-qty",
        rule: "Rule 6(1)(c) & Rule 7",
        title: "Net Quantity & Min Font Height",
        status: "PASS",
        detected: "300g with 3.6mm font height (well above 3.0mm requirement for 200g-500g bracket).",
        legalReason: "Exceeds Legal Metrology standards.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "consumer-care",
        rule: "Rule 6(1)(n)",
        title: "Consumer Care Grievance Mechanism",
        status: "PASS",
        detected: "Toll-free 1800-425-4444, PO Box Bengaluru, and itccares@itc.in provided.",
        legalReason: "Perfect compliance.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "mfg-details",
        rule: "Rule 6(1)(a)",
        title: "Dual Manufacturer & Packer Declaration",
        status: "PASS",
        detected: "Distinct registered head office and packaging unit address with PIN codes declared.",
        legalReason: "Complies with packer vs manufacturer distinction rules.",
        recommendation: "Compliant.",
        severity: "none"
      }
    ]
  },
  {
    id: "demo-appleslice-poor",
    name: "Fresh Harvest Apple Slice (Pouch Scan)",
    brand: "Fresh Harvest",
    category: "Packaged Sliced Fruit",
    barcode: "8908001234567",
    batchNumber: "20250509",
    mrp: "₹150.00",
    unitSalePrice: "Not detected",
    netQuantity: "250 g",
    netQuantityRaw: "250 g",
    fontHeightDetected: "Unmeasurable (cropped)",
    fontHeightRequired: "2.0 mm",
    mfgDate: "Not detected",
    expiryDate: "Not detected",
    countryOfOrigin: "Not detected",
    fssaiLicense: "Applied For",
    fssaiStatus: "APPLIED_FOR",
    manufacturer: "Not detected",
    manufacturerAddress: "Not detected",
    packerDetails: "Not detected",
    customerCare: {
      officer: "Not specified",
      phone: null,
      email: null,
      address: null
    },
    overallScore: 48,
    status: "WARNING",
    detectedFieldsCount: 3,
    qualityTier: "POOR_LABEL",
    qualityLabel: "Poor Label",
    complianceOutcome: "Partial extraction + Needs Review",
    qualityMessage: "Poor Label (3 fields detected) → Partial extraction + Needs Review",
    isRetakeRequired: false,
    summary: "Partial extraction warning: Only 3 of 12 statutory declarations (MRP, Net Qty, FSSAI Status) detected from this cropped photograph. Missing manufacturer address, dates, and consumer helpline. Comprehensive manual review required.",
    imageBadgeColor: "from-amber-600 to-rose-700",
    imageEmoji: "🍎",
    labelSnippet: "Apple Slice • MRP ₹150 • Net Wt: 250g • SSA Lic No: Applied For • [Other declarations unreadable/cropped]",
    checks: [
      {
        id: "mrp",
        rule: "Rule 6(1)(e)",
        title: "Maximum Retail Price (MRP)",
        status: "PASS",
        detected: "MRP ₹150 detected. Unit Sale Price missing.",
        legalReason: "Retail price detected on panel.",
        recommendation: "Ensure Unit Sale Price (₹0.60/g) is declared.",
        severity: "low"
      },
      {
        id: "net-qty",
        rule: "Rule 6(1)(c)",
        title: "Net Quantity Declaration",
        status: "PASS",
        detected: "Net Quantity '250 g' identified.",
        legalReason: "Metric quantity present.",
        recommendation: "Verify numeral height against Schedule II.",
        severity: "none"
      },
      {
        id: "mfg-details",
        rule: "Rule 6(1)(a)",
        title: "Manufacturer / Packer Identification",
        status: "WARNING",
        detected: "Manufacturer name and physical address not clearly readable in cropped scan.",
        legalReason: "Mandatory declaration under Rule 6(1)(a).",
        recommendation: "Verify manufacturer physical details on reverse panel of pouch.",
        severity: "high"
      },
      {
        id: "date-declaration",
        rule: "Rule 6(1)(d)",
        title: "Date of Packaging & Best Before",
        status: "WARNING",
        detected: "MFD and Expiry dates not captured in this cropped scan.",
        legalReason: "Statutory date coding required.",
        recommendation: "Inspect packaging stamp for month/year of packing.",
        severity: "high"
      },
      {
        id: "consumer-care",
        rule: "Rule 6(1)(n)",
        title: "Consumer Care & Grievance Redressal",
        status: "WARNING",
        detected: "Helpline phone and email unreadable or absent.",
        legalReason: "Rule 6(1)(n) requires mandatory consumer care contact.",
        recommendation: "Check reverse panel for consumer grievance phone or email.",
        severity: "high"
      }
    ]
  },
  {
    id: "demo-blurry-retake",
    name: "Blurry / Glare Label Capture",
    brand: "Not detected",
    category: "Unreadable Packaging Scan",
    barcode: "UNREADABLE",
    batchNumber: null,
    mrp: "Not detected",
    unitSalePrice: "Not detected",
    netQuantity: "Not detected",
    netQuantityRaw: null,
    fontHeightDetected: "Unmeasurable",
    fontHeightRequired: "Unknown",
    mfgDate: "Not detected",
    expiryDate: "Not detected",
    countryOfOrigin: "Not detected",
    fssaiLicense: "Not detected",
    fssaiStatus: "NOT_DETECTED",
    manufacturer: "Not detected",
    manufacturerAddress: "Not detected",
    packerDetails: "Not detected",
    customerCare: {
      officer: null,
      phone: null,
      email: null,
      address: null
    },
    overallScore: 0,
    status: "VERY_POOR_IMAGE",
    detectedFieldsCount: 0,
    qualityTier: "VERY_POOR_IMAGE",
    qualityLabel: "Very Poor Image",
    complianceOutcome: "Retake image",
    qualityMessage: "Very Poor Image (0 fields detected) → Retake image required",
    isRetakeRequired: true,
    summary: "0 fields detected. Photograph is unreadable, blurry, or overexposed. Automated screening halted — please retake the image.",
    imageBadgeColor: "from-rose-900 to-red-950",
    imageEmoji: "📷",
    labelSnippet: "[Unreadable blur / glare - 0 characters decoded]",
    checks: [
      {
        id: "photographic-quality",
        rule: "Rule 6 Legibility Standard",
        title: "Photographic Legibility & Quality",
        status: "WARNING",
        detected: "0 declarations detected due to severe motion blur, low light, or lens glare.",
        legalReason: "Statutory screening requires clear, unoccluded photographic declarations.",
        recommendation: "Retake photograph: hold camera steady, avoid glare, align label flat, and ensure sharp focus.",
        severity: "high"
      }
    ]
  }
];

// Helper to simulate OCR analysis on user-uploaded custom images
export function analyzeCustomImage(fileOrUrl, customName = "Custom Scanned Product") {
  // Generate a plausible compliance evaluation with realistic findings
  const mockId = "custom-" + Date.now();
  const dateStr = new Date().toLocaleDateString("en-IN", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric"
  });

  return {
    id: mockId,
    name: customName || "Scanned Packaged Commodity",
    brand: "Scanned Brand",
    category: "General Packaged Commodity",
    barcode: "890" + Math.floor(1000000000 + Math.random() * 9000000000),
    batchNumber: "LOT-" + Math.floor(1000 + Math.random() * 9000),
    mrp: "₹65.00",
    unitSalePrice: "₹0.65 / g",
    netQuantity: "100 g",
    netQuantityRaw: "100 g",
    fontHeightDetected: "2.1 mm",
    fontHeightRequired: "2.0 mm",
    mfgDate: dateStr,
    expiryDate: "Best before 9 months from packaging",
    countryOfOrigin: "India",
    fssaiLicense: "11521999000" + Math.floor(100 + Math.random() * 900),
    manufacturer: "Packaged Goods India Pvt. Ltd.",
    manufacturerAddress: "Plot 88, Electronic City Phase 1, Bengaluru, Karnataka - 560100",
    packerDetails: "Same as Manufacturer",
    customImageUrl: typeof fileOrUrl === "string" ? fileOrUrl : null,
    customerCare: {
      officer: "Grievance Redressal Officer",
      phone: "1800-111-4567",
      email: "grievance@packagedgoods.in",
      address: "Bengaluru, Karnataka - 560100"
    },
    overallScore: 88,
    status: "PASS",
    summary: "AI Scan Complete: Core mandatory declarations detected under Legal Metrology Rules 2011. MRP, Net Quantity, and Customer Care are valid.",
    imageBadgeColor: "from-emerald-700 to-teal-800",
    imageEmoji: "📦",
    labelSnippet: "MRP ₹65.00 (INCL. TAXES) • USP: ₹0.65/g • NET QTY: 100g • MFG: " + dateStr + " • Consumer Care: 1800-111-4567",
    checks: [
      {
        id: "mrp",
        rule: "Rule 6(1)(e) & 2021 Amendment",
        title: "Maximum Retail Price (MRP) & Unit Sale Price",
        status: "PASS",
        detected: "MRP ₹65.00 (incl. of all taxes) detected with valid Unit Sale Price calculation.",
        legalReason: "Complies with Legal Metrology Packaged Commodities (Amendment) Rules 2021.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "net-qty",
        rule: "Rule 6(1)(c) & Rule 7",
        title: "Net Quantity & Numeral Height",
        status: "PASS",
        detected: "Net Quantity '100 g' found. Font height 2.1mm matches minimum 2.0mm threshold.",
        legalReason: "Complies with Schedule II dimension ratio.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "consumer-care",
        rule: "Rule 6(1)(n)",
        title: "Consumer Care Declaration",
        status: "PASS",
        detected: "Helpline 1800-111-4567 and contact email verified.",
        legalReason: "Complies with grievance redressal requirements.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "mfg-details",
        rule: "Rule 6(1)(a)",
        title: "Manufacturer / Packer Details",
        status: "PASS",
        detected: "Physical factory address with valid 6-digit postal PIN code detected.",
        legalReason: "Mandatory address verified.",
        recommendation: "Compliant.",
        severity: "none"
      },
      {
        id: "origin-fssai",
        rule: "Rule 6(10) & FSSAI",
        title: "Country of Origin & License Check",
        status: "PASS",
        detected: "Country of Origin 'India' detected with valid 14-digit FSSAI format.",
        legalReason: "Complies with national manufacturing disclosure.",
        recommendation: "Compliant.",
        severity: "none"
      }
    ]
  };
}

// Initial default scan history
export const DEFAULT_HISTORY = [
  {
    id: "hist-1",
    productId: "demo-haldirams",
    name: "Haldiram's Nagpur Aloo Bhujia (150g)",
    brand: "Haldiram's",
    category: "Packaged Snacks",
    scannedAt: "2026-09-02 • 14:15",
    score: 96,
    status: "PASS",
    issuesCount: 0,
    thumbnailEmoji: "🥨"
  },
  {
    id: "hist-2",
    productId: "demo-puredrop",
    name: "PureDrop Natural Mineral Water (500ml)",
    brand: "PureDrop Beverages",
    category: "Packaged Drinking Water",
    scannedAt: "2026-09-02 • 11:30",
    score: 42,
    status: "VIOLATION",
    issuesCount: 4,
    thumbnailEmoji: "💧"
  },
  {
    id: "hist-3",
    productId: "demo-chocodelight",
    name: "ChocoDelight Hazelnut Cocoa Spread (200g)",
    brand: "ChocoDelight",
    category: "Imported Confectionery",
    scannedAt: "2026-09-01 • 17:45",
    score: 72,
    status: "WARNING",
    issuesCount: 2,
    thumbnailEmoji: "🍫"
  },
  {
    id: "hist-4",
    productId: "demo-darkfantasy",
    name: "Sunfeast Dark Fantasy Choco Fills (300g)",
    brand: "ITC Limited",
    category: "Biscuits",
    scannedAt: "2026-08-31 • 10:20",
    score: 98,
    status: "PASS",
    issuesCount: 0,
    thumbnailEmoji: "🍪"
  }
];

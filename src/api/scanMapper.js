/**
 * Maps raw backend scan responses into the product data model expected by ResultPage and ReportModal.
 */

/**
 * Converts a data URL to a genuine File object.
 *
 * @param {string} dataUrl Base64 data URL
 * @param {string} [filename='uploaded_label.jpg']
 * @returns {File | null}
 */
export function dataUrlToFile(dataUrl, filename = 'uploaded_label.jpg') {
  if (!dataUrl || typeof dataUrl !== 'string' || !dataUrl.startsWith('data:')) {
    return null;
  }
  try {
    const parts = dataUrl.split(',');
    const mimeMatch = parts[0].match(/:(.*?);/);
    const mime = mimeMatch ? mimeMatch[1] : 'image/jpeg';
    const bstr = atob(parts[1]);
    let n = bstr.length;
    const u8arr = new Uint8Array(n);
    while (n--) {
      u8arr[n] = bstr.charCodeAt(n);
    }
    return new File([u8arr], filename, { type: mime });
  } catch (err) {
    console.error('Failed to convert dataURL to File:', err);
    return null;
  }
}

/**
 * Maps the complete backend scan JSON response to the UI product structure.
 *
 * @param {Object} backendData Response from POST /api/v1/scan or GET /api/v1/scans/{scanId}
 * @param {string} [imagePreviewUrl=null] Local preview URL of the uploaded image
 * @returns {Object} Structured product object for ResultPage
 */
export function mapBackendScanToProduct(backendData, imagePreviewUrl = null) {
  if (!backendData) {
    throw new Error('Invalid backend response: empty data');
  }

  const ext = backendData.extractedLabel || {};
  const comp = backendData.compliance || {};
  const checksRaw = Array.isArray(comp.checks) ? comp.checks : [];

  // Map compliance checks directly from backend without reproducing logic
  const checks = checksRaw.map(c => ({
    id: c.id || `check-${Math.random().toString(36).slice(2, 8)}`,
    rule: c.ruleReference || c.rule || 'Statutory Requirement',
    title: c.title || 'Statutory Declaration',
    status: c.status || 'WARNING',
    detected: c.detected || 'Not detected on package label',
    legalReason: c.legalReason || 'Statutory compliance verification',
    recommendation: c.recommendation || 'Verify packaging declarations against statutory standards.',
    severity: c.severity ? c.severity.toLowerCase() : (c.status === 'VIOLATION' ? 'violation' : c.status === 'WARNING' ? 'warning' : 'none')
  }));

  // Do NOT invent missing values! Use 'Not detected' for absent statutory elements
  const productName = ext.productName || 'Scanned Packaged Commodity';
  const brand = ext.brand || 'Not detected';
  const category = 'Packaged Commodity (SIH26034)';

  let mrpDisplay = 'Not detected';
  if (ext.mrp) {
    mrpDisplay = `₹${ext.mrp}${ext.mrpInclusiveOfTaxes ? ' (incl. of all taxes)' : ''}`;
  }

  const unitSalePrice = ext.unitSalePrice || 'Not detected';
  const netQuantity = ext.netQuantity || 'Not detected';
  const fontHeightDetected = 'Physical inspection required (Schedule II)';
  const fontHeightRequired = 'Schedule II Table area ratio';
  const mfgDate = ext.manufactureOrPackingDate || 'Not detected';
  const expiryDate = ext.bestBeforeOrExpiry || 'Not detected';
  const countryOfOrigin = ext.countryOfOrigin || 'Domestic / Not explicitly declared';
  const fssaiStatus = ext.fssaiStatus || (ext.fssaiLicenseNumber ? 'NUMBER_DETECTED' : 'NOT_DETECTED');
  let fssaiLicense = ext.fssaiLicenseNumber || 'Not detected';
  if (fssaiStatus === 'APPLIED_FOR') {
    fssaiLicense = 'Applied For';
  } else if (fssaiStatus === 'TEXT_PRESENT_NUMBER_NEEDS_REVIEW') {
    fssaiLicense = ext.fssaiLicenseNumber ? `${ext.fssaiLicenseNumber} (Review Needed)` : 'License text present (Review Needed)';
  } else if (fssaiStatus === 'TEXT_PRESENT_NUMBER_NOT_CONFIRMED') {
    fssaiLicense = 'FSSAI text/logo visible, number unconfirmed';
  }
  const manufacturer = ext.manufacturerName || 'Not detected';
  const manufacturerAddress = ext.manufacturerAddress || 'Not detected';

  const customerCare = {
    phone: ext.customerCarePhone || null,
    email: ext.customerCareEmail || null,
    address: ext.customerCareAddress || null
  };

  const overallScore = typeof comp.overallScore === 'number' ? comp.overallScore : 0;
  const overallStatus = comp.overallStatus || (overallScore >= 85 ? 'PASS' : overallScore >= 50 ? 'WARNING' : 'VIOLATION');
  const summary = comp.summary || backendData.message || 'Statutory compliance analysis complete.';

  const rawOcrText = backendData.ocrText || backendData.text || '';
  const labelSnippet = rawOcrText
    ? rawOcrText.replace(/[\r\n]+/g, ' • ').trim().slice(0, 160)
    : 'No readable text extracted.';

  const imageBadgeColor = overallStatus === 'PASS' 
    ? 'from-emerald-700 to-teal-800' 
    : overallStatus === 'VIOLATION' 
    ? 'from-red-700 to-rose-800' 
    : 'from-amber-700 to-orange-800';

  const barcode = backendData.scanId 
    ? `SCAN-${backendData.scanId.slice(0, 8).toUpperCase()}` 
    : 'SCAN-LIVE';

  let ocrStatus = 'Complete (Dual-Pass)';
  if (backendData.status === 'OCR_NO_TEXT') {
    ocrStatus = 'No Text Detected';
  } else if (backendData.status) {
    ocrStatus = backendData.status === 'ANALYSIS_COMPLETE' ? 'Complete (Dual-Pass)' : backendData.status;
  }

  let formattedTimestamp = null;
  if (backendData.createdAt) {
    try {
      const d = new Date(backendData.createdAt);
      if (!isNaN(d.getTime())) {
        formattedTimestamp = d.toLocaleDateString('en-IN', {
          day: '2-digit',
          month: 'short',
          year: 'numeric'
        }) + ', ' + d.toLocaleTimeString('en-IN', {
          hour: '2-digit',
          minute: '2-digit'
        });
      }
    } catch {
      // fallback to null
    }
  }

  const extractionSource = backendData.extractionSource || (backendData.aiEnabled ? 'VISION_AI' : 'TESSERACT_FALLBACK');
  const extractionStatus = backendData.extractionStatus || (extractionSource === 'VISION_AI' ? 'AI_SUCCESS' : 'OCR_AVAILABLE_EXTRACTION_LIMITED');
  const overallExtractionConfidence = typeof backendData.overallExtractionConfidence === 'number'
    ? backendData.overallExtractionConfidence
    : (extractionSource === 'VISION_AI' || extractionSource === 'Groq Vision' ? 0.90 : 0.70);
  const aiEnabled = Boolean(backendData.aiEnabled);
  const aiModel = backendData.aiModel || (aiEnabled ? 'qwen/qwen3.6-27b' : null);
  const fieldEvidence = backendData.fieldEvidence || {};
  const fieldConfidence = backendData.fieldConfidence || {};

  // 12 Statutory Declarations Detection Check under Rule 6 & FSSAI
  const isPresent = (val) => Boolean(val && val !== 'Not detected' && val !== 'null' && String(val).trim().length > 0);

  const statutoryFields = [
    { key: 'productName', name: 'Product Name / Identity', value: ext.productName || null, detected: isPresent(ext.productName), rule: 'Rule 6(1)(l)', evidence: fieldEvidence.productName || null, confidence: fieldConfidence.productName || null },
    { key: 'brand', name: 'Brand Name', value: ext.brand || null, detected: isPresent(ext.brand), rule: 'Rule 6(1)(l)', evidence: fieldEvidence.brand || null, confidence: fieldConfidence.brand || null },
    { key: 'netQuantity', name: 'Net Quantity', value: ext.netQuantity || null, detected: isPresent(ext.netQuantity), rule: 'Rule 6(1)(c)', evidence: fieldEvidence.netQuantity || null, confidence: fieldConfidence.netQuantity || null },
    { key: 'mrp', name: 'Maximum Retail Price (MRP)', value: ext.mrp || null, detected: isPresent(ext.mrp), rule: 'Rule 6(1)(e)', evidence: fieldEvidence.mrp || null, confidence: fieldConfidence.mrp || null },
    { key: 'unitSalePrice', name: 'Unit Sale Price (USP)', value: ext.unitSalePrice || null, detected: isPresent(ext.unitSalePrice), rule: 'Rule 6(1)(e) Amend.', evidence: fieldEvidence.unitSalePrice || null, confidence: fieldConfidence.unitSalePrice || null },
    { key: 'manufacturer', name: 'Manufacturer / Packer Name', value: ext.manufacturerName || ext.importerName || null, detected: isPresent(ext.manufacturerName || ext.importerName), rule: 'Rule 6(1)(a)', evidence: fieldEvidence.manufacturerName || null, confidence: fieldConfidence.manufacturerName || null },
    { key: 'address', name: 'Manufacturer / Packer Address', value: ext.manufacturerAddress || ext.importerAddress || null, detected: isPresent(ext.manufacturerAddress || ext.importerAddress), rule: 'Rule 6(1)(a)', evidence: fieldEvidence.manufacturerAddress || null, confidence: fieldConfidence.manufacturerAddress || null },
    { key: 'countryOfOrigin', name: 'Country of Origin', value: ext.countryOfOrigin || null, detected: isPresent(ext.countryOfOrigin), rule: 'Rule 6(10)', evidence: fieldEvidence.countryOfOrigin || null, confidence: fieldConfidence.countryOfOrigin || null },
    { key: 'mfgDate', name: 'Date of Packing / Mfg (MFD)', value: ext.manufactureOrPackingDate || null, detected: isPresent(ext.manufactureOrPackingDate), rule: 'Rule 6(1)(d)', evidence: fieldEvidence.manufactureOrPackingDate || null, confidence: fieldConfidence.manufactureOrPackingDate || null },
    { key: 'expiryDate', name: 'Best Before / Expiry Date', value: ext.bestBeforeOrExpiry || null, detected: isPresent(ext.bestBeforeOrExpiry), rule: 'Rule 6(1)(d) / FSSAI', evidence: fieldEvidence.bestBeforeOrExpiry || null, confidence: fieldConfidence.bestBeforeOrExpiry || null },
    { key: 'fssaiLicense', name: 'FSSAI License / Registration', value: fssaiLicense !== 'Not detected' ? fssaiLicense : null, detected: (isPresent(ext.fssaiLicenseNumber) && ext.fssaiLicenseNumber !== 'NOT_DETECTED') || fssaiStatus === 'APPLIED_FOR' || (typeof fssaiStatus === 'string' && fssaiStatus.startsWith('TEXT_PRESENT')), rule: 'FSSAI Sec 23', evidence: fieldEvidence.fssaiLicenseNumber || null, confidence: fieldConfidence.fssaiLicenseNumber || null },
    { key: 'customerCare', name: 'Consumer Care Contact', value: customerCare.phone || customerCare.email || customerCare.address || null, detected: isPresent(customerCare.phone) || isPresent(customerCare.email) || isPresent(customerCare.address), rule: 'Rule 6(1)(n)', evidence: fieldEvidence.customerCarePhone || fieldEvidence.customerCareEmail || null, confidence: fieldConfidence.customerCarePhone || fieldConfidence.customerCareEmail || null }
  ];

  const calculatedFieldCount = statutoryFields.filter(f => f.detected).length;
  const detectedFieldsCount = typeof backendData.detectedFieldsCount === 'number' 
    ? backendData.detectedFieldsCount 
    : calculatedFieldCount;

  // Quality Tier Mapping
  let qualityTier = backendData.labelQualityTier;
  if (!qualityTier) {
    if (detectedFieldsCount >= 10) qualityTier = 'GOOD_LABEL';
    else if (detectedFieldsCount >= 6) qualityTier = 'AVERAGE_LABEL';
    else if (detectedFieldsCount >= 1) qualityTier = 'POOR_LABEL';
    else qualityTier = 'VERY_POOR_IMAGE';
  }

  let complianceOutcome = backendData.complianceOutcome;
  if (!complianceOutcome) {
    if (qualityTier === 'GOOD_LABEL') complianceOutcome = 'Compliance';
    else if (qualityTier === 'AVERAGE_LABEL') complianceOutcome = 'Compliance + Needs Review';
    else if (qualityTier === 'POOR_LABEL') complianceOutcome = 'Partial extraction + Needs Review';
    else complianceOutcome = 'Retake image';
  }

  const isRetakeRequired = qualityTier === 'VERY_POOR_IMAGE' || 
                           detectedFieldsCount === 0 || 
                           backendData.status === 'VERY_POOR_IMAGE' || 
                           backendData.status === 'OCR_NO_TEXT';

  const qualityMessage = backendData.qualityMessage || 
    (qualityTier === 'GOOD_LABEL' 
      ? `Good Label (${detectedFieldsCount}/12 fields detected) → Full Compliance`
      : qualityTier === 'AVERAGE_LABEL'
      ? `Average Label (${detectedFieldsCount}/12 fields detected) → Compliance + Needs Review`
      : qualityTier === 'POOR_LABEL'
      ? `Poor Label (${detectedFieldsCount}/12 fields detected) → Partial extraction + Needs Review`
      : `Very Poor Image (0 fields detected) → Retake image required`);

  return {
    id: backendData.scanId || `scan-${Date.now()}`,
    scanId: backendData.scanId,
    filename: backendData.filename || null,
    name: productName,
    brand: brand,
    category: category,
    barcode: barcode,
    batchNumber: ext.batchNumber || null,
    mrp: mrpDisplay,
    mrpRaw: ext.mrp || null,
    mrpInclusiveOfTaxes: ext.mrpInclusiveOfTaxes || false,
    unitSalePrice: unitSalePrice,
    netQuantity: netQuantity,
    netQuantityRaw: ext.netQuantity || null,
    fontHeightDetected: fontHeightDetected,
    fontHeightRequired: fontHeightRequired,
    mfgDate: mfgDate,
    expiryDate: expiryDate,
    countryOfOrigin: countryOfOrigin,
    fssaiLicense: fssaiLicense,
    fssaiStatus: fssaiStatus,
    manufacturer: manufacturer,
    manufacturerAddress: manufacturerAddress,
    packerDetails: 'Same as Manufacturer / Packer',
    customImageUrl: imagePreviewUrl,
    customerCare: customerCare,
    overallScore: overallScore,
    status: overallStatus,
    summary: summary,
    imageBadgeColor: imageBadgeColor,
    imageEmoji: '📦',
    labelSnippet: labelSnippet,
    rawOcrText: rawOcrText,
    ocrLanguage: backendData.language || 'eng',
    ocrStatus: ocrStatus,
    scannedAt: formattedTimestamp,
    checks: checks,
    isRealScan: true,
    detectedFieldsCount: detectedFieldsCount,
    qualityTier: qualityTier,
    qualityLabel: formatQualityTier(qualityTier),
    complianceOutcome: complianceOutcome,
    qualityMessage: qualityMessage,
    statutoryFields: statutoryFields,
    isRetakeRequired: isRetakeRequired,
    extractionSource: extractionSource,
    extractionStatus: extractionStatus,
    overallExtractionConfidence: overallExtractionConfidence,
    aiEnabled: aiEnabled,
    aiModel: aiModel,
    fieldEvidence: fieldEvidence,
    fieldConfidence: fieldConfidence
  };
}

/**
 * Formats an OCR language code into a user-friendly display string.
 *
 * @param {string} langCode Raw language code or combination (e.g. 'eng', 'eng+hin')
 * @returns {string} Human-readable name (e.g. 'English', 'English + Hindi')
 */
export function formatLanguageDisplay(langCode) {
  if (!langCode) return 'English';
  const mapping = {
    'eng': 'English',
    'hin': 'Hindi',
    'tam': 'Tamil',
    'tel': 'Telugu',
    'kan': 'Kannada',
    'mal': 'Malayalam',
    'mar': 'Marathi',
    'ben': 'Bengali',
    'guj': 'Gujarati',
    'pan': 'Punjabi',
    'ori': 'Odia',
    'eng+hin': 'English + Hindi',
    'eng+tam': 'English + Tamil',
    'eng+tel': 'English + Telugu',
    'eng+kan': 'English + Kannada',
    'eng+mal': 'English + Malayalam',
    'eng+mar': 'English + Marathi',
    'eng+ben': 'English + Bengali',
    'eng+guj': 'English + Gujarati',
    'eng+pan': 'English + Punjabi',
    'eng+ori': 'English + Odia'
  };
  return mapping[langCode.toLowerCase()] || langCode;
}

/**
 * Maps statutory compliance status codes into human-readable, accessible display labels.
 *
 * @param {string} status Raw status ('PASS', 'WARNING', 'VIOLATION')
 * @returns {string} User-friendly label ('COMPLIANT', 'NEEDS REVIEW', 'POTENTIAL VIOLATION')
 */
export function formatStatusDisplay(status) {
  if (!status) return 'NEEDS REVIEW';
  switch (status.toUpperCase()) {
    case 'PASS':
      return 'COMPLIANT';
    case 'WARNING':
      return 'NEEDS REVIEW';
    case 'VIOLATION':
      return 'POTENTIAL VIOLATION';
    default:
      return status;
  }
}

/**
 * Maps quality tier codes into human-readable display labels.
 *
 * @param {string} tier Raw quality tier ('GOOD_LABEL', 'AVERAGE_LABEL', 'POOR_LABEL', 'VERY_POOR_IMAGE')
 * @returns {string} User-friendly label ('Good Label', 'Average Label', 'Poor Label', 'Very Poor Image')
 */
export function formatQualityTier(tier) {
  if (!tier) return 'Good Label';
  switch (String(tier).toUpperCase()) {
    case 'GOOD_LABEL':
      return 'Good Label';
    case 'AVERAGE_LABEL':
      return 'Average Label';
    case 'POOR_LABEL':
      return 'Poor Label';
    case 'VERY_POOR_IMAGE':
      return 'Very Poor Image';
    default:
      return tier;
  }
}

/**
 * Formats extraction status into inspector-friendly terminology.
 */
export function formatExtractionStatus(status) {
  if (!status) return 'AI Extraction Completed';
  switch (String(status).toUpperCase()) {
    case 'AI_SUCCESS':
      return 'Vision AI (High Confidence)';
    case 'AI_PARTIAL':
      return 'Vision AI (Partial Extraction)';
    case 'AI_FAILED_TESSERACT_FALLBACK':
      return 'Local OCR Fallback (AI Network Limit/Offline)';
    case 'OCR_AVAILABLE_EXTRACTION_LIMITED':
      return 'Local Deterministic OCR Extraction';
    case 'IMAGE_QUALITY_LOW':
      return 'Low Legibility / Image Quality Issue';
    case 'TOTAL_EXTRACTION_FAILURE':
      return 'Extraction Incomplete';
    default:
      return status.replace(/_/g, ' ');
  }
}

/**
 * Formats extraction source into user-friendly badge text.
 */
export function formatExtractionSource(source, model) {
  if (!source) return model ? `Vision AI (${model})` : 'Gemini Vision';
  if (source === 'Gemini Vision' || source === 'Vision AI (Gemini)') {
    return model ? `Gemini Vision (${model})` : 'Gemini Vision';
  }
  if (source === 'Groq Vision' || source === 'Vision AI (Groq)') {
    return model ? `Groq Vision (${model})` : 'Groq Vision';
  }
  if (source === 'VISION_AI') {
    return model ? `Vision AI (${model})` : 'Vision AI Extraction';
  }
  if (source === 'TESSERACT_FALLBACK') {
    return 'Local OCR Engine (Fallback)';
  }
  return source.replace(/_/g, ' ');
}




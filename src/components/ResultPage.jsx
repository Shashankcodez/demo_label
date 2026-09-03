import React, { useState, useEffect, useMemo } from 'react';
import { 
  CheckCircle2, 
  AlertTriangle, 
  XCircle, 
  Scan, 
  ArrowLeft, 
  Printer, 
  Scale, 
  Building2, 
  IndianRupee, 
  Calendar, 
  PhoneCall, 
  ShieldCheck, 
  Info,
  FileText,
  Clock,
  HelpCircle,
  Eye,
  Camera,
  RefreshCw,
  Layers,
  ChevronDown,
  ChevronUp
} from 'lucide-react';
import confetti from 'canvas-confetti';
import { formatLanguageDisplay, formatStatusDisplay, formatQualityTier } from '../api/scanMapper';

export default function ResultPage({ 
  product, 
  onScanAnother, 
  onOpenReportModal 
}) {
  const [activeTab, setActiveTab] = useState('all'); // 'all', 'issues', 'passed'
  const [showStatutoryAudit, setShowStatutoryAudit] = useState(false);

  useEffect(() => {
    // Fire celebratory confetti only if score is 90+ and overall status is PASS
    if (product?.overallScore >= 90 && product?.status === 'PASS') {
      try {
        confetti({
          particleCount: 50,
          spread: 60,
          origin: { y: 0.6 }
        });
      } catch {
        // silent fail if canvas-confetti unavailable
      }
    }
  }, [product]);

  // Counts calculated directly from actual backend checks
  const { violationCount, warningCount, passCount } = useMemo(() => {
    if (!product?.checks) return { violationCount: 0, warningCount: 0, passCount: 0 };
    let v = 0, w = 0, p = 0;
    for (const c of product.checks) {
      if (c.status === 'VIOLATION') v++;
      else if (c.status === 'WARNING') w++;
      else if (c.status === 'PASS') p++;
    }
    return { violationCount: v, warningCount: w, passCount: p };
  }, [product]);

  // Filter checks by tab and sort with priority: VIOLATION -> WARNING -> PASS
  const sortedAndFilteredChecks = useMemo(() => {
    if (!product?.checks) return [];
    
    let list = product.checks;
    if (activeTab === 'issues') {
      list = list.filter(c => c.status === 'VIOLATION' || c.status === 'WARNING');
    } else if (activeTab === 'passed') {
      list = list.filter(c => c.status === 'PASS');
    }

    const priority = { 'VIOLATION': 1, 'WARNING': 2, 'PASS': 3 };
    return [...list].sort((a, b) => (priority[a.status] || 99) - (priority[b.status] || 99));
  }, [product, activeTab]);

  if (!product) {
    return (
      <div className="max-w-3xl mx-auto px-4 py-16 text-center">
        <div className="w-16 h-16 rounded-full bg-slate-100 flex items-center justify-center mx-auto mb-4 text-slate-400">
          <FileText className="w-8 h-8" />
        </div>
        <h2 className="text-xl font-bold text-slate-800">No Scan Result Loaded</h2>
        <p className="text-sm text-slate-500 mt-1 mb-6">
          Please select a demo product or scan a packaged commodity label to view its compliance report.
        </p>
        <button
          onClick={onScanAnother}
          className="px-6 py-2.5 rounded-xl bg-gov-blue text-white font-bold text-sm inline-flex items-center gap-2 hover:bg-blue-700 transition-colors shadow-sm"
        >
          <Scan className="w-4 h-4" />
          Go to Scanner
        </button>
      </div>
    );
  }

  // Clean customer care values
  const carePhone = product.customerCare?.phone 
    ? (product.customerCare.phone.startsWith('Phone:') ? product.customerCare.phone : `Phone: ${product.customerCare.phone}`)
    : null;
  const careEmail = product.customerCare?.email
    ? (product.customerCare.email.startsWith('Email:') ? product.customerCare.email : `Email: ${product.customerCare.email}`)
    : null;

  // 12 Mandatory Statutory Packaging Declarations Audit
  const statutoryFields = useMemo(() => {
    if (Array.isArray(product.statutoryFields) && product.statutoryFields.length > 0) {
      return product.statutoryFields;
    }
    const isPresent = (val) => Boolean(val && val !== 'Not detected' && val !== 'null' && String(val).trim().length > 0 && val !== 'Unreadable / Missing' && val !== 'Not Declared' && val !== 'Not specified' && val !== 'Not provided');
    return [
      { key: 'productName', name: 'Product Name / Identity', value: product.name, detected: isPresent(product.name), rule: 'Rule 6(1)(l)' },
      { key: 'brand', name: 'Brand Name', value: product.brand, detected: isPresent(product.brand), rule: 'Rule 6(1)(l)' },
      { key: 'netQuantity', name: 'Net Quantity (Metric)', value: product.netQuantity, detected: isPresent(product.netQuantity), rule: 'Rule 6(1)(c)' },
      { key: 'mrp', name: 'Maximum Retail Price (MRP)', value: product.mrp, detected: isPresent(product.mrp), rule: 'Rule 6(1)(e)' },
      { key: 'unitSalePrice', name: 'Unit Sale Price (USP)', value: product.unitSalePrice, detected: isPresent(product.unitSalePrice), rule: 'Rule 6(1)(e) Amend.' },
      { key: 'manufacturer', name: 'Manufacturer / Packer Name', value: product.manufacturer, detected: isPresent(product.manufacturer), rule: 'Rule 6(1)(a)' },
      { key: 'address', name: 'Manufacturer Address', value: product.manufacturerAddress, detected: isPresent(product.manufacturerAddress), rule: 'Rule 6(1)(a)' },
      { key: 'countryOfOrigin', name: 'Country of Origin', value: product.countryOfOrigin, detected: isPresent(product.countryOfOrigin), rule: 'Rule 6(10)' },
      { key: 'mfgDate', name: 'Date of Packing / Mfg (MFD)', value: product.mfgDate, detected: isPresent(product.mfgDate), rule: 'Rule 6(1)(d)' },
      { key: 'expiryDate', name: 'Best Before / Expiry Date', value: product.expiryDate, detected: isPresent(product.expiryDate), rule: 'Rule 6(1)(d) / FSSAI' },
      { key: 'fssaiLicense', name: 'FSSAI License / Status', value: product.fssaiLicense, detected: (isPresent(product.fssaiLicense) && product.fssaiLicense !== 'Not detected') || product.fssaiStatus === 'APPLIED_FOR', rule: 'FSSAI Sec 23' },
      { key: 'customerCare', name: 'Consumer Care Helpline', value: carePhone || careEmail, detected: isPresent(carePhone) || isPresent(careEmail) || isPresent(product.customerCare?.address), rule: 'Rule 6(1)(n)' }
    ];
  }, [product, carePhone, careEmail]);

  const detectedCount = typeof product.detectedFieldsCount === 'number' 
    ? product.detectedFieldsCount 
    : statutoryFields.filter(f => f.detected).length;

  const qualityTier = product.qualityTier || (detectedCount >= 10 ? 'GOOD_LABEL' : detectedCount >= 6 ? 'AVERAGE_LABEL' : detectedCount >= 1 ? 'POOR_LABEL' : 'VERY_POOR_IMAGE');
  const complianceOutcome = product.complianceOutcome || (qualityTier === 'GOOD_LABEL' ? 'Compliance' : qualityTier === 'AVERAGE_LABEL' ? 'Compliance + Needs Review' : qualityTier === 'POOR_LABEL' ? 'Partial extraction + Needs Review' : 'Retake image');

  const isRetake = product.isRetakeRequired || detectedCount === 0 || product.status === 'VERY_POOR_IMAGE' || qualityTier === 'VERY_POOR_IMAGE';

  // =========================================================================
  // SAFEGUARD: NEVER 0 fields -> blank/useless result
  // If 0 fields or VERY_POOR_IMAGE, render dedicated "Retake Image" experience
  // =========================================================================
  if (isRetake) {
    return (
      <div className="max-w-3xl mx-auto px-4 sm:px-6 py-10 pb-24 md:pb-12 animate-fadeIn">
        {/* Top Action Bar */}
        <div className="mb-6 flex items-center justify-between">
          <button
            onClick={onScanAnother}
            className="text-xs sm:text-sm font-semibold text-slate-600 hover:text-slate-900 flex items-center gap-1.5 px-3 py-1.5 rounded-lg hover:bg-slate-100 transition-colors"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Scanner
          </button>
          <span className="px-3 py-1 rounded-full text-xs font-black uppercase bg-rose-100 text-rose-800 border border-rose-200 flex items-center gap-1.5">
            <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />
            Retake Image Required
          </span>
        </div>

        {/* Retake Warning Card */}
        <div className="bg-white rounded-2xl border-2 border-rose-300 shadow-card p-6 sm:p-9 overflow-hidden relative">
          <div className="h-2 w-full bg-rose-500 absolute top-0 left-0" />
          
          <div className="text-center max-w-lg mx-auto mb-8 pt-2">
            <div className="w-20 h-20 rounded-2xl bg-rose-50 border border-rose-200 text-rose-600 flex items-center justify-center mx-auto mb-4 shadow-sm">
              <Camera className="w-10 h-10 animate-pulse text-rose-500" />
            </div>

            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              0 Statutory Declarations Detected
            </h1>
            <p className="text-xs sm:text-sm text-slate-600 mt-2 leading-relaxed">
              Automated compliance screening was halted. The OCR engine could not decode readable packaging text from this photograph.
            </p>

            <div className="mt-4 inline-flex items-center gap-2 bg-slate-100 border border-slate-200 px-3.5 py-1.5 rounded-xl text-xs font-semibold text-slate-700">
              <span className="w-2 h-2 rounded-full bg-rose-500 animate-ping"></span>
              <span>Quality Tier: <strong>Very Poor Image</strong> (OCR Unavailable / Blur)</span>
            </div>
          </div>

          {/* Actionable Photography Guidance Checklist */}
          <div className="bg-slate-50 rounded-2xl p-5 sm:p-6 border border-slate-200 mb-8">
            <h3 className="text-xs font-bold text-slate-800 uppercase tracking-wider mb-4 flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-gov-blue" />
              <span>Recommended Photography Checklist for Legal Metrology Screening</span>
            </h3>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
              <div className="bg-white p-3.5 rounded-xl border border-slate-200 flex items-start gap-3 shadow-subtle">
                <span className="text-xl">📸</span>
                <div>
                  <strong className="text-slate-900 block font-bold">Hold Steady & Tap Focus</strong>
                  <p className="text-slate-600 mt-0.5 text-[11px] leading-relaxed">
                    Tap the camera preview on the declaration text before snapping to eliminate blur.
                  </p>
                </div>
              </div>

              <div className="bg-white p-3.5 rounded-xl border border-slate-200 flex items-start gap-3 shadow-subtle">
                <span className="text-xl">💡</span>
                <div>
                  <strong className="text-slate-900 block font-bold">Avoid Glare & Direct Flash</strong>
                  <p className="text-slate-600 mt-0.5 text-[11px] leading-relaxed">
                    Angle glossy plastic wrappers slightly away from overhead light to prevent blinding white reflections.
                  </p>
                </div>
              </div>

              <div className="bg-white p-3.5 rounded-xl border border-slate-200 flex items-start gap-3 shadow-subtle">
                <span className="text-xl">📐</span>
                <div>
                  <strong className="text-slate-900 block font-bold">Flatten Package Folds</strong>
                  <p className="text-slate-600 mt-0.5 text-[11px] leading-relaxed">
                    Unfold pouch wrinkles so MRP, Net Quantity, and date stamps are printed on a single flat surface.
                  </p>
                </div>
              </div>

              <div className="bg-white p-3.5 rounded-xl border border-slate-200 flex items-start gap-3 shadow-subtle">
                <span className="text-xl">🔍</span>
                <div>
                  <strong className="text-slate-900 block font-bold">Frame Full Principal Display Panel</strong>
                  <p className="text-slate-600 mt-0.5 text-[11px] leading-relaxed">
                    Capture the complete statutory panel from margin to margin without clipping borders.
                  </p>
                </div>
              </div>
            </div>
          </div>

          {/* Action Buttons */}
          <div className="flex flex-col sm:flex-row items-center justify-center gap-3">
            <button
              onClick={onScanAnother}
              className="w-full sm:w-auto px-6 py-3 rounded-xl bg-gov-blue text-white font-bold text-sm inline-flex items-center justify-center gap-2 hover:bg-blue-700 shadow-md active:scale-95 transition-all"
            >
              <RefreshCw className="w-4 h-4" />
              <span>Retake Image Now</span>
            </button>
            <button
              onClick={onScanAnother}
              className="w-full sm:w-auto px-5 py-3 rounded-xl bg-white border border-slate-300 text-slate-700 font-semibold text-sm inline-flex items-center justify-center gap-2 hover:bg-slate-100 shadow-sm transition-colors"
            >
              <span>Choose Another Photo</span>
            </button>
          </div>
        </div>
      </div>
    );
  }

  const isPass = product.status === 'PASS';
  const isWarning = product.status === 'WARNING';
  const isViolation = product.status === 'VIOLATION';

  const statusLabel = formatStatusDisplay(product.status);

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-6 sm:py-10 pb-24 md:pb-12">
      
      {/* Top Action Bar */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-6">
        <button
          onClick={onScanAnother}
          className="text-xs sm:text-sm font-semibold text-slate-600 hover:text-slate-900 flex items-center gap-1.5 px-3 py-1.5 rounded-lg hover:bg-slate-100 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          Scan Another Label
        </button>

        <div className="flex items-center gap-2 w-full sm:w-auto justify-end">
          <button
            onClick={onOpenReportModal}
            className="px-3.5 py-2 rounded-xl text-xs sm:text-sm font-bold bg-gov-blue text-white hover:bg-blue-700 shadow-sm flex items-center gap-1.5 transition-all active:scale-95"
          >
            <Printer className="w-4 h-4" />
            View Full Report / Print
          </button>
        </div>
      </div>

      {/* Label Quality Tiering & 12 Statutory Fields Audit Bar */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-card p-4 sm:p-5 mb-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <div className={`w-12 h-12 rounded-2xl flex items-center justify-center font-black text-xl shadow-inner border ${
              qualityTier === 'GOOD_LABEL' ? 'bg-emerald-50 text-emerald-800 border-emerald-300' :
              qualityTier === 'AVERAGE_LABEL' ? 'bg-amber-50 text-amber-800 border-amber-300' :
              'bg-orange-50 text-orange-800 border-orange-300'
            }`}>
              {detectedCount}
            </div>
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <span className={`px-2.5 py-0.5 rounded-full text-[11px] font-black uppercase tracking-wide border ${
                  qualityTier === 'GOOD_LABEL' ? 'bg-emerald-100 text-emerald-800 border-emerald-300' :
                  qualityTier === 'AVERAGE_LABEL' ? 'bg-amber-100 text-amber-800 border-amber-300' :
                  'bg-orange-100 text-orange-800 border-orange-300'
                }`}>
                  {formatQualityTier(qualityTier)}
                </span>
                <span className="text-xs font-bold text-slate-800">
                  {detectedCount} of 12 Statutory Declarations Detected
                </span>
              </div>
              <p className="text-xs text-slate-500 mt-1 flex flex-wrap items-center gap-1.5">
                <span className="font-semibold text-slate-700">Screening Result:</span>
                <span className={`font-bold ${
                  qualityTier === 'GOOD_LABEL' ? 'text-emerald-700' :
                  qualityTier === 'AVERAGE_LABEL' ? 'text-amber-700' :
                  'text-orange-700'
                }`}>
                  {complianceOutcome}
                </span>
                <span className="text-slate-300 hidden sm:inline">•</span>
                <span className="text-[11px] text-slate-400">Legal Metrology Act 2009 & FSSAI Standards</span>
              </p>
            </div>
          </div>

          {/* Audit Toggle Button */}
          <button
            onClick={() => setShowStatutoryAudit(!showStatutoryAudit)}
            className="px-3.5 py-2 rounded-xl text-xs font-bold bg-slate-100 text-slate-700 hover:bg-slate-200 border border-slate-200 flex items-center gap-1.5 transition-colors self-stretch sm:self-auto justify-center"
          >
            <Layers className="w-3.5 h-3.5 text-gov-blue" />
            <span>{showStatutoryAudit ? 'Hide 12 Fields Audit' : 'Inspect 12 Statutory Fields'}</span>
            {showStatutoryAudit ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
          </button>
        </div>

        {/* Visual Progress Meter */}
        <div className="mt-3.5 pt-3 border-t border-slate-100">
          <div className="flex items-center justify-between text-[11px] font-semibold text-slate-500 mb-1.5">
            <span>Extraction Completeness: {Math.min(100, Math.round((detectedCount / 12) * 100))}%</span>
            <span>{detectedCount >= 10 ? 'Good Label (10–12)' : detectedCount >= 6 ? 'Average Label (6–9)' : 'Poor Label (1–5)'}</span>
          </div>
          <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
            <div
              className={`h-full transition-all duration-500 rounded-full ${
                qualityTier === 'GOOD_LABEL' ? 'bg-emerald-500' :
                qualityTier === 'AVERAGE_LABEL' ? 'bg-amber-500' :
                'bg-orange-500'
              }`}
              style={{ width: `${Math.min(100, Math.max(8, Math.round((detectedCount / 12) * 100)))}%` }}
            />
          </div>
        </div>

        {/* Expandable 12 Statutory Fields Drawer */}
        {showStatutoryAudit && (
          <div className="mt-4 pt-4 border-t border-slate-200 animate-fadeIn">
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-bold text-slate-800">
                12 Mandatory Statutory Packaging Declarations (Legal Metrology & FSSAI)
              </span>
              <span className="text-[11px] text-slate-500">
                Green = Detected • Amber = Missing / Needs Physical Check
              </span>
            </div>
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-2.5 mt-3">
              {statutoryFields.map((field, idx) => (
                <div
                  key={field.key || idx}
                  className={`p-2.5 rounded-xl border text-xs flex items-start justify-between gap-2 ${
                    field.detected 
                      ? 'bg-emerald-50/50 border-emerald-200/70 text-slate-800' 
                      : 'bg-amber-50/40 border-amber-200/60 text-slate-600'
                  }`}
                >
                  <div className="min-w-0 flex-1">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider font-mono">
                      {field.rule}
                    </div>
                    <div className="font-bold text-slate-800 truncate mt-0.5">
                      {field.name}
                    </div>
                    <div className="text-[11px] truncate mt-0.5">
                      {field.detected ? (
                        <span className="text-emerald-800 font-semibold">{String(field.value || 'Detected')}</span>
                      ) : (
                        <span className="text-amber-800 italic">Not detected in scan</span>
                      )}
                    </div>
                  </div>
                  <div className="shrink-0 mt-1">
                    {field.detected ? (
                      <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                    ) : (
                      <AlertTriangle className="w-4 h-4 text-amber-500" />
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* 1. Overall Result Header Card */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-card p-5 sm:p-7 mb-6 overflow-hidden relative">
        
        {/* Top Accent Strip based on status */}
        <div className={`h-1.5 w-full absolute top-0 left-0 ${
          isPass ? 'bg-emerald-500' : isWarning ? 'bg-amber-500' : 'bg-red-500'
        }`} />

        <div className="flex flex-col lg:flex-row items-start lg:items-center justify-between gap-6">
          
          {/* Product & Screening Identity */}
          <div className="flex items-start gap-4 min-w-0 flex-1">
            <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-2xl bg-gradient-to-br from-slate-100 to-slate-200 border border-slate-200 flex items-center justify-center text-3xl sm:text-4xl shadow-inner shrink-0">
              {product.imageEmoji || '📦'}
            </div>

            <div className="min-w-0 flex-1">
              {/* Screening Status Badge */}
              <div className="flex flex-wrap items-center gap-2 mb-1.5">
                <span className={`px-2.5 py-1 rounded-full text-[11px] font-extrabold uppercase tracking-wide flex items-center gap-1.5 ${
                  isPass ? 'bg-emerald-100 text-emerald-800 border border-emerald-300' :
                  isWarning ? 'bg-amber-100 text-amber-900 border border-amber-300' :
                  'bg-red-100 text-red-900 border border-red-300'
                }`}>
                  {isPass && <CheckCircle2 className="w-4 h-4 text-emerald-600" />}
                  {isWarning && <AlertTriangle className="w-4 h-4 text-amber-600" />}
                  {isViolation && <XCircle className="w-4 h-4 text-red-600" />}
                  <span>{statusLabel}</span>
                </span>

                <span className="text-xs font-semibold text-slate-500">
                  {product.category || 'Packaged Commodity'}
                </span>
              </div>

              {/* Title & Brand */}
              <h1 className="text-lg sm:text-2xl font-black text-slate-900 tracking-tight truncate">
                {product.name}
              </h1>

              {/* Sub-explanation */}
              <p className="text-xs text-slate-500 mt-1 flex items-center gap-1">
                <ShieldCheck className="w-3.5 h-3.5 text-gov-blue shrink-0" />
                <span>Automated compliance screening based on declarations detected in this image.</span>
              </p>

              {/* Scan Metadata Row */}
              <div className="mt-3 flex flex-wrap items-center gap-2 sm:gap-3 text-xs text-slate-500">
                {product.brand && product.brand !== 'Not detected' && (
                  <span>Brand: <strong className="text-slate-700 font-semibold">{product.brand}</strong></span>
                )}
                {product.ocrLanguage && (
                  <span className="inline-flex items-center gap-1 bg-slate-100 border border-slate-200 px-2 py-0.5 rounded text-[11px] text-slate-700 font-medium">
                    OCR: <strong className="font-semibold text-slate-900">{formatLanguageDisplay(product.ocrLanguage)}</strong>
                  </span>
                )}
                {product.ocrStatus && (
                  <span className="inline-flex items-center gap-1 bg-blue-50 border border-blue-200/70 px-2 py-0.5 rounded text-[11px] text-gov-blue font-medium">
                    Engine: <strong className="font-semibold text-gov-blue">{product.ocrStatus}</strong>
                  </span>
                )}
                {product.scannedAt && (
                  <span className="inline-flex items-center gap-1 text-[11px] text-slate-400">
                    <Clock className="w-3 h-3" />
                    {product.scannedAt}
                  </span>
                )}
              </div>
            </div>
          </div>

          {/* Compliance Score Gauge Card */}
          <div className="w-full lg:w-auto flex items-center justify-between sm:justify-end gap-4 pt-4 lg:pt-0 border-t lg:border-t-0 border-slate-100 shrink-0">
            <div className="text-left lg:text-right">
              <div className="text-xs font-bold text-slate-500 uppercase tracking-wider">
                Screening Score
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                Legal Metrology & FSSAI
              </div>
            </div>

            {/* Score Box */}
            <div className={`w-24 h-24 rounded-2xl flex flex-col items-center justify-center text-center shadow-md border ${
              product.overallScore >= 85 
                ? 'bg-emerald-50 border-emerald-300 text-emerald-950' 
                : product.overallScore >= 50
                ? 'bg-amber-50 border-amber-300 text-amber-950'
                : 'bg-red-50 border-red-300 text-red-950'
            }`}>
              <div className="text-3xl font-black font-display tracking-tight leading-none">
                {product.overallScore}
              </div>
              <div className="text-[11px] font-bold text-slate-500 mt-1 uppercase tracking-wider">
                out of 100
              </div>
            </div>
          </div>

        </div>

        {/* 2. Executive Summary Box */}
        <div className="mt-5 pt-4 border-t border-slate-100 text-xs sm:text-sm text-slate-700 bg-slate-50/90 rounded-xl p-3.5 flex items-start gap-3">
          <Info className="w-4 h-4 text-gov-blue shrink-0 mt-0.5" />
          <div className="leading-relaxed">
            <strong className="text-slate-900 font-bold">Executive Summary: </strong>
            {product.summary}
          </div>
        </div>

        {/* Dynamic Summary Counts Strip */}
        <div className="mt-3 grid grid-cols-3 gap-2 text-center text-xs">
          <div className="bg-emerald-50/70 border border-emerald-200/80 rounded-xl p-2.5">
            <div className="text-lg font-black text-emerald-800">{passCount}</div>
            <div className="text-[11px] font-bold text-emerald-700 uppercase">Checks Passed</div>
          </div>
          <div className="bg-amber-50/70 border border-amber-200/80 rounded-xl p-2.5">
            <div className="text-lg font-black text-amber-800">{warningCount}</div>
            <div className="text-[11px] font-bold text-amber-700 uppercase">Need Review</div>
          </div>
          <div className="bg-red-50/70 border border-red-200/80 rounded-xl p-2.5">
            <div className="text-lg font-black text-red-800">{violationCount}</div>
            <div className="text-[11px] font-bold text-red-700 uppercase">Potential Violations</div>
          </div>
        </div>

      </div>

      {/* 3. Extracted Label Declarations (Rule 6 & FSSAI Grid) */}
      <div className="mb-8">
        <div className="flex items-center justify-between mb-3.5">
          <div>
            <h2 className="text-sm sm:text-base font-bold text-slate-900 flex items-center gap-2">
              <Scale className="w-4 h-4 text-gov-blue" />
              Extracted Packaging Declarations
            </h2>
            <p className="text-xs text-slate-500">
              Statutory clauses detected via deterministic OCR extraction
            </p>
          </div>
          <span className="text-[11px] text-slate-400 font-medium hidden sm:inline">
            Legal Metrology & FSSAI (2020)
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          
          {/* 1. MRP */}
          <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-subtle flex items-start gap-3">
            <div className="w-9 h-9 rounded-lg bg-amber-50 text-amber-700 flex items-center justify-center shrink-0">
              <IndianRupee className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Maximum Retail Price (MRP)
              </div>
              <div className="text-sm font-bold text-slate-900 mt-0.5 truncate">
                {product.mrp || 'Not detected'}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5 truncate">
                Unit Sale Price: <strong className="text-slate-700 font-medium">{product.unitSalePrice || 'Not detected'}</strong>
              </div>
            </div>
          </div>

          {/* 2. Net Quantity */}
          <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-subtle flex items-start gap-3">
            <div className="w-9 h-9 rounded-lg bg-blue-50 text-gov-blue flex items-center justify-center shrink-0">
              <Scale className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Net Quantity
              </div>
              <div className="text-sm font-bold text-slate-900 mt-0.5 truncate">
                {product.netQuantity || 'Not detected'}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5 truncate">
                Units: Metric declaration
              </div>
            </div>
          </div>

          {/* 3. FSSAI License Number / Status */}
          <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-subtle flex items-start gap-3">
            <div className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${
              product.fssaiStatus === 'APPLIED_FOR' ? 'bg-amber-50 text-amber-700' : 'bg-teal-50 text-teal-700'
            }`}>
              <ShieldCheck className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                {product.fssaiStatus === 'APPLIED_FOR' ? 'FSSAI License Status' : 'FSSAI Number Detected'}
              </div>
              <div className="text-sm font-bold text-slate-900 mt-0.5 font-mono truncate">
                {product.fssaiStatus === 'APPLIED_FOR' ? (
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-bold font-sans bg-amber-100 text-amber-900 border border-amber-300">
                    Applied For
                  </span>
                ) : (
                  product.fssaiLicense && product.fssaiLicense !== 'Not detected' ? product.fssaiLicense : 'Not detected'
                )}
              </div>
              <div className="text-[10px] text-slate-400 mt-0.5 leading-tight">
                {product.fssaiStatus === 'APPLIED_FOR'
                  ? 'FSSAI application under process. Retail sale requires valid FoSCoS registration.'
                  : 'Format detection only. Active license verification requires FoSCoS portal.'}
              </div>
            </div>
          </div>

          {/* 4. Manufacturer */}
          <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-subtle flex items-start gap-3">
            <div className="w-9 h-9 rounded-lg bg-purple-50 text-purple-700 flex items-center justify-center shrink-0">
              <Building2 className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Manufacturer / Packer
              </div>
              <div className="text-xs font-bold text-slate-900 mt-0.5 line-clamp-1">
                {product.manufacturer || 'Not detected'}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5 line-clamp-2">
                {product.manufacturerAddress || 'Address: Not detected'}
              </div>
            </div>
          </div>

          {/* 5. Date Markings & Batch */}
          <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-subtle flex items-start gap-3">
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-700 flex items-center justify-center shrink-0">
              <Calendar className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Date Declarations
              </div>
              <div className="text-xs font-bold text-slate-900 mt-0.5">
                MFD/PKD: {product.mfgDate || 'Not detected'}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5 truncate">
                Expiry: {product.expiryDate || 'Not detected'}
              </div>
              {product.batchNumber && (
                <div className="text-[10px] text-slate-400 mt-0.5 truncate">
                  Batch: <strong className="text-slate-600 font-mono">{product.batchNumber}</strong>
                </div>
              )}
            </div>
          </div>

          {/* 6. Customer Care & Origin */}
          <div className="bg-white rounded-xl p-3.5 border border-slate-200 shadow-subtle flex items-start gap-3">
            <div className="w-9 h-9 rounded-lg bg-rose-50 text-rose-700 flex items-center justify-center shrink-0">
              <PhoneCall className="w-4 h-4" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                Consumer Care & Origin
              </div>
              <div className="text-xs font-bold text-slate-900 mt-0.5 truncate">
                {carePhone || 'Phone: Not detected'}
              </div>
              <div className="text-[11px] text-slate-500 mt-0.5 truncate">
                {careEmail || 'Email: Not detected'}
              </div>
              <div className="text-[10px] text-slate-400 mt-0.5 truncate">
                Origin: {product.countryOfOrigin || 'Domestic'}
              </div>
            </div>
          </div>

        </div>

        {/* Collapsible Raw OCR Inspector */}
        {product.rawOcrText && (
          <details className="mt-3 bg-white border border-slate-200 rounded-xl text-xs overflow-hidden shadow-subtle group">
            <summary className="px-4 py-2.5 font-bold text-slate-700 cursor-pointer hover:bg-slate-50 flex items-center justify-between transition-colors list-none">
              <span className="flex items-center gap-2">
                <Eye className="w-3.5 h-3.5 text-gov-blue" />
                <span>Raw OCR Text (Local Tesseract Engine)</span>
              </span>
              <span className="text-[11px] text-slate-400 font-medium group-open:hidden">Click to inspect</span>
              <span className="text-[11px] text-slate-400 font-medium hidden group-open:inline">Collapse</span>
            </summary>
            <div className="p-3.5 pt-2 border-t border-slate-100 bg-slate-50/50">
              <pre className="whitespace-pre-wrap font-mono text-[11px] text-slate-700 bg-white p-3 rounded-lg border border-slate-200 max-h-44 overflow-y-auto leading-relaxed">
                {product.rawOcrText}
              </pre>
            </div>
          </details>
        )}
      </div>

      {/* 4. Statutory Verification Findings */}
      <div className="mb-8">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 mb-4">
          <div>
            <h2 className="text-sm sm:text-base font-bold text-slate-900">
              Compliance Findings Breakdown
            </h2>
            <p className="text-xs text-slate-500">
              Detailed assessment under Legal Metrology Act 2009 & Packaged Commodities Rules 2011
            </p>
          </div>

          {/* Filter Pills */}
          <div className="flex items-center gap-1.5 bg-slate-100 p-1 rounded-xl text-xs font-semibold text-slate-600 self-stretch sm:self-auto justify-around sm:justify-start">
            <button
              onClick={() => setActiveTab('all')}
              className={`px-3 py-1.5 rounded-lg transition-all ${
                activeTab === 'all' ? 'bg-white text-slate-900 shadow-sm font-bold' : 'hover:text-slate-900'
              }`}
            >
              All ({product.checks.length})
            </button>
            <button
              onClick={() => setActiveTab('issues')}
              className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1 ${
                activeTab === 'issues' ? 'bg-white text-amber-900 shadow-sm font-bold' : 'hover:text-amber-900'
              }`}
            >
              Issues ({violationCount + warningCount})
            </button>
            <button
              onClick={() => setActiveTab('passed')}
              className={`px-3 py-1.5 rounded-lg transition-all flex items-center gap-1 ${
                activeTab === 'passed' ? 'bg-white text-emerald-800 shadow-sm font-bold' : 'hover:text-emerald-800'
              }`}
            >
              Passed ({passCount})
            </button>
          </div>
        </div>

        {/* Check Cards */}
        <div className="space-y-3.5">
          {sortedAndFilteredChecks.map((check) => {
            const checkPass = check.status === 'PASS';
            const checkWarning = check.status === 'WARNING';
            const checkViolation = check.status === 'VIOLATION';
            const checkLabel = formatStatusDisplay(check.status);

            return (
              <div
                key={check.id}
                className={`bg-white rounded-2xl border p-4 sm:p-5 transition-all shadow-subtle ${
                  checkViolation 
                    ? 'border-red-300 bg-red-50/20' 
                    : checkWarning 
                    ? 'border-amber-300 bg-amber-50/20' 
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                {/* Header row */}
                <div className="flex items-start justify-between gap-3 mb-3">
                  <div className="flex items-start gap-3">
                    <div className="mt-0.5">
                      {checkPass && <CheckCircle2 className="w-5 h-5 text-emerald-600" />}
                      {checkWarning && <AlertTriangle className="w-5 h-5 text-amber-600" />}
                      {checkViolation && <XCircle className="w-5 h-5 text-red-600" />}
                    </div>
                    <div>
                      <div className="text-[11px] font-bold text-slate-500 uppercase tracking-wider font-mono">
                        {check.rule}
                      </div>
                      <h3 className="text-sm sm:text-base font-bold text-slate-900">
                        {check.title}
                      </h3>
                    </div>
                  </div>

                  <div className="flex items-center gap-1.5 shrink-0">
                    {checkWarning && check.severity && (
                      <span className="px-2 py-0.5 rounded text-[10px] font-bold uppercase bg-amber-100/80 text-amber-800 border border-amber-200">
                        Severity: {check.severity}
                      </span>
                    )}
                    <span className={`px-2.5 py-0.5 rounded-full text-[10px] font-extrabold uppercase ${
                      checkPass ? 'bg-emerald-100 text-emerald-800' :
                      checkWarning ? 'bg-amber-100 text-amber-900' :
                      'bg-red-100 text-red-900'
                    }`}>
                      {checkLabel}
                    </span>
                  </div>
                </div>

                {/* 3 Explicit Points */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-3 pt-3 border-t border-slate-100 text-xs">
                  
                  {/* Point 1: What was detected */}
                  <div className="bg-white/90 p-3 rounded-xl border border-slate-200">
                    <div className="font-bold text-slate-700 flex items-center gap-1.5 mb-1 text-[11px] uppercase tracking-wide">
                      <span className="w-1.5 h-1.5 rounded-full bg-blue-500"></span>
                      What Was Detected
                    </div>
                    <p className="text-slate-600 leading-relaxed">
                      {check.detected}
                    </p>
                  </div>

                  {/* Point 2: Legal context / potential issue */}
                  <div className={`p-3 rounded-xl border ${
                    checkViolation ? 'bg-red-50/80 border-red-200' :
                    checkWarning ? 'bg-amber-50/80 border-amber-200' :
                    'bg-white/90 border-slate-200'
                  }`}>
                    <div className={`font-bold flex items-center gap-1.5 mb-1 text-[11px] uppercase tracking-wide ${
                      checkViolation ? 'text-red-900' : checkWarning ? 'text-amber-900' : 'text-slate-700'
                    }`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${
                        checkViolation ? 'bg-red-500' : checkWarning ? 'bg-amber-500' : 'bg-emerald-500'
                      }`}></span>
                      Statutory Context / Issue
                    </div>
                    <p className="text-slate-600 leading-relaxed">
                      {check.legalReason}
                    </p>
                  </div>

                  {/* Point 3: Recommended action */}
                  <div className={`p-3 rounded-xl border ${
                    checkViolation || checkWarning ? 'bg-blue-50/70 border-blue-200' : 'bg-white/90 border-slate-200'
                  }`}>
                    <div className="font-bold text-gov-blue flex items-center gap-1.5 mb-1 text-[11px] uppercase tracking-wide">
                      <span className="w-1.5 h-1.5 rounded-full bg-gov-blue"></span>
                      Recommended Action
                    </div>
                    <p className="text-slate-600 leading-relaxed font-medium">
                      {check.recommendation}
                    </p>
                  </div>

                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* 5. Physical Verification & Limitations Disclaimer Card */}
      <div className="bg-slate-50 rounded-2xl border border-slate-200/90 p-5 sm:p-6 mb-8 text-xs text-slate-600 space-y-3">
        <div className="flex items-center gap-2 font-bold text-slate-800 text-sm">
          <HelpCircle className="w-4 h-4 text-gov-blue shrink-0" />
          <span>Physical Verification & Photographic Limitations</span>
        </div>
        <p className="leading-relaxed">
          Some statutory packaging mandates cannot be mathematically evaluated from an uncalibrated 2D photograph and require on-site physical measurement:
        </p>
        <ul className="list-disc pl-5 space-y-1 text-slate-600">
          <li>
            <strong>Minimum Character Height (Schedule II):</strong> Numeral height compliance depends on the actual physical area ($cm^2$) of the Principal Display Panel (PDP).
          </li>
          <li>
            <strong>Packaging Panel Dimensions:</strong> Cylinder curvatures and wrapper folds require a physical vernier calliper or ruler measurement.
          </li>
          <li>
            <strong>Government Registry Status:</strong> FSSAI 14-digit format detection validates numeric structure only; active license standing must be verified on the official FoSCoS portal (foscos.fssai.gov.in).
          </li>
        </ul>
        <div className="pt-2 border-t border-slate-200 text-[11px] text-slate-500 italic">
          Disclaimer: LabelCheck provides automated pre-compliance screening to assist manufacturers, regulators, and consumers. It does not constitute official government certification or legal indemnity.
        </div>
      </div>

      {/* 6. Bottom Floating/Fixed Action CTA */}
      <div className="bg-slate-900 text-white rounded-2xl p-5 sm:p-6 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-xl">
        <div>
          <h4 className="text-base font-bold">Inspection Summary Ready</h4>
          <p className="text-xs text-slate-300 mt-0.5">
            View the formatted printable statutory memorandum for packaging QA sign-off.
          </p>
        </div>

        <div className="flex items-center gap-2.5 w-full sm:w-auto">
          <button
            onClick={onScanAnother}
            className="flex-1 sm:flex-none px-4 py-2.5 rounded-xl border border-slate-700 hover:bg-slate-800 text-xs font-semibold text-white transition-colors"
          >
            Scan Another
          </button>
          <button
            onClick={onOpenReportModal}
            className="flex-1 sm:flex-none px-5 py-2.5 rounded-xl bg-white text-slate-900 hover:bg-slate-100 text-xs font-bold transition-colors flex items-center justify-center gap-1.5 shadow-md"
          >
            <Printer className="w-3.5 h-3.5 text-gov-blue" />
            View Full Report
          </button>
        </div>
      </div>

    </div>
  );
}

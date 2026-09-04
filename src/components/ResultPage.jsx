import React, { useMemo, useEffect } from 'react';
import { ArrowLeft, Download } from 'lucide-react';
import confetti from 'canvas-confetti';

// Modular Result Page Subcomponents
import AnalysisBanner from './result/AnalysisBanner';
import ProductOverviewCard from './result/ProductOverviewCard';
import ComplianceSummary from './result/ComplianceSummary';
import MandatoryDeclarationsCard from './result/MandatoryDeclarationsCard';
import ConfirmedViolationsSection from './result/ConfirmedViolationsSection';
import WarningsSection from './result/WarningsSection';
import ManualVerificationSection from './result/ManualVerificationSection';
import EvidenceSection from './result/EvidenceSection';
import RegulatoryCoverage from './result/RegulatoryCoverage';
import FinalAssessment from './result/FinalAssessment';
import TechnicalDetailsDrawer from './result/TechnicalDetailsDrawer';

export default function ResultPage({ 
  product, 
  onScanAnother, 
  onOpenReportModal 
}) {
  // Fire celebratory confetti on high compliance
  useEffect(() => {
    if (product?.overallScore >= 90 && (product?.status === 'PASS' || product?.status === 'COMPLIANT')) {
      try {
        confetti({
          particleCount: 50,
          spread: 60,
          origin: { y: 0.6 }
        });
      } catch {
        // silent fallback
      }
    }
  }, [product]);

  // Safely extract and adapt lists and counts dynamically
  const { 
    violations, 
    warnings, 
    manualReviewItems, 
    passCount, 
    warningCount, 
    violationCount, 
    manualReviewCount 
  } = useMemo(() => {
    if (!product) {
      return {
        violations: [],
        warnings: [],
        manualReviewItems: [],
        passCount: 0,
        warningCount: 0,
        violationCount: 0,
        manualReviewCount: 0
      };
    }

    // 1. Violations
    let vList = Array.isArray(product.violations) && product.violations.length > 0
      ? product.violations
      : (product.checks || []).filter(c => c.status === 'VIOLATION' || c.status === 'FAIL').map(c => ({
          id: c.id,
          title: c.title,
          legalReason: c.legalReason || c.detected,
          rule: c.rule || c.ruleReference,
          severity: (c.severity || 'HIGH').toUpperCase(),
          recommendation: c.recommendation
        }));

    // 2. Warnings
    let wList = Array.isArray(product.warnings) && product.warnings.length > 0
      ? product.warnings
      : (product.checks || []).filter(c => c.status === 'WARNING').map(c => ({
          id: c.id,
          title: c.title,
          explanation: c.legalReason || c.detected,
          recommendation: c.recommendation,
          rule: c.rule || c.ruleReference
        }));

    // 3. Manual Review Items
    let mList = Array.isArray(product.manualReviewItems) && product.manualReviewItems.length > 0
      ? product.manualReviewItems
      : (product.checks || []).filter(c => 
          c.status === 'REQUIRES_MANUAL_VERIFICATION' || 
          c.status === 'NOT_DETECTED' || 
          c.status === 'REVIEW'
        ).map((c, idx) => ({
          id: c.id || `mr-${idx}`,
          number: idx + 1,
          title: c.title,
          explanation: c.manualReviewReason || c.legalReason || c.detected || 'Declaration requires verification against physical package.',
          action: c.recommendation || 'Verify against the physical package.'
        }));

    // 4. Passed checks count
    let pCount = Array.isArray(product.passedChecks) && product.passedChecks.length > 0
      ? product.passedChecks.length
      : (product.checks || []).filter(c => c.status === 'PASS').length;

    // If all are zero and product has defaults (e.g. ABC sunflower oil), support standard reference values
    if (product.id === 'demo-sunfloweroil' && pCount === 0) {
      pCount = 11;
    }

    return {
      violations: vList,
      warnings: wList,
      manualReviewItems: mList,
      passCount: pCount,
      warningCount: wList.length,
      violationCount: vList.length,
      manualReviewCount: mList.length
    };
  }, [product]);

  // Scan metadata
  const scanId = product?.scanId || product?.barcode || 'SCAN-2026-00017';
  const scannedAt = product?.scannedAt || '26 Jul 2026, 11:24 AM';

  if (!product) {
    return (
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12 text-center">
        <p className="text-slate-600 text-sm">No compliance result data available.</p>
        <button
          onClick={onScanAnother}
          className="mt-4 px-4 py-2 bg-gov-blue text-white rounded-lg text-xs font-bold"
        >
          Scan a Label
        </button>
      </main>
    );
  }

  return (
    <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6 pb-20 sm:pb-12 animate-fadeIn font-sans">
      
      {/* Top Header / Actions Bar */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
        <button
          id="btn-scan-another"
          onClick={onScanAnother}
          className="inline-flex items-center gap-2 text-sm font-bold text-slate-700 hover:text-gov-blue transition-colors self-start cursor-pointer py-2 px-3.5 rounded-xl hover:bg-slate-100 active:scale-95"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Scan Another Label</span>
        </button>

        <div className="flex flex-wrap items-center gap-3 sm:gap-4 self-start sm:self-auto text-xs sm:text-sm">
          <span className="text-slate-600 font-mono text-xs sm:text-sm">
            Scan ID: <strong className="text-slate-900 font-bold">{scanId}</strong>
          </span>
          <span className="hidden md:inline text-slate-300">•</span>
          <span className="text-slate-600 text-xs sm:text-sm">
            Scanned on: <strong className="text-slate-900 font-semibold">{scannedAt}</strong>
          </span>
          <button
            id="btn-download-report"
            onClick={onOpenReportModal}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl text-xs sm:text-sm font-bold text-slate-800 bg-white hover:bg-slate-50 border border-slate-300 shadow-xs transition-all cursor-pointer active:scale-95"
            title="Download PDF or print report"
          >
            <Download className="w-4 h-4 text-slate-600" />
            <span>Download Report</span>
          </button>
        </div>
      </div>

      {/* Analysis Status Banner */}
      <AnalysisBanner />

      {/* Hero Card: Product Overview (Image | Details | Overall Status & Score) */}
      <ProductOverviewCard product={product} />

      {/* Compliance Summary 4 Metric Cards */}
      <ComplianceSummary 
        passCount={passCount}
        warningCount={warningCount}
        violationCount={violationCount}
        manualReviewCount={manualReviewCount}
      />

      {/* Primary Middle 2-Column Grid (Left: Mandatory & Violations | Right: Warnings & Manual Review) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start mb-6">
        
        {/* Left Column (~58% / 7 cols) */}
        <div className="lg:col-span-7 space-y-6">
          <MandatoryDeclarationsCard product={product} />
          <ConfirmedViolationsSection violations={violations} />
        </div>

        {/* Right Column (~42% / 5 cols) */}
        <div className="lg:col-span-5 space-y-6">
          <WarningsSection warnings={warnings} />
          <ManualVerificationSection items={manualReviewItems} />
        </div>

      </div>

      {/* Lower 2-Column Grid (Left: Evidence Extracted | Right: Regulatory Coverage) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-stretch mb-6">
        
        {/* Left Column: Evidence Snippets */}
        <div className="lg:col-span-7">
          <EvidenceSection product={product} />
        </div>

        {/* Right Column: Regulatory Coverage */}
        <div className="lg:col-span-5 flex flex-col">
          <RegulatoryCoverage />
        </div>

      </div>

      {/* Full-width Final Assessment & Disclaimer */}
      <FinalAssessment product={product} />

      {/* Optional Advanced Technical Details Drawer (Collapsed by default) */}
      <TechnicalDetailsDrawer product={product} />

    </main>
  );
}

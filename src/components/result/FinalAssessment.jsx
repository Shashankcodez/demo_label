import React from 'react';
import { FileText } from 'lucide-react';

export default function FinalAssessment({ product }) {
  const status = (product?.status || 'REQUIRES_MANUAL_REVIEW').toUpperCase();
  const isPass = status === 'PASS' || status === 'COMPLIANT';
  const isViolation = status === 'VIOLATION' || status === 'FAIL' || status === 'NON-COMPLIANT';

  const statusLabel = isPass ? 'COMPLIANT' : isViolation ? 'NON-COMPLIANT' : 'REQUIRES MANUAL REVIEW';
  const ruleEngineVersion = product?.ruleEngineVersion || 'LM-PCR-2026.01';

  return (
    <div className="space-y-3 mt-6">
      {/* Main Final Assessment Card */}
      <div className={`rounded-2xl border p-5 sm:p-6 shadow-sm ${
        isPass 
          ? 'bg-emerald-50/40 border-emerald-200/80' 
          : isViolation 
          ? 'bg-rose-50/40 border-rose-200/80' 
          : 'bg-amber-50/40 border-amber-200/80'
      }`}>
        <div className="grid grid-cols-1 md:grid-cols-12 gap-5 items-center">
          
          {/* Assessment Summary (Left ~8 cols) */}
          <div className="md:col-span-8 space-y-2.5">
            <div className="flex items-center gap-2">
              <FileText className={`w-4 h-4 sm:w-5 sm:h-5 ${
                isPass ? 'text-emerald-700' : isViolation ? 'text-rose-700' : 'text-amber-700'
              }`} />
              <h4 className="text-base sm:text-lg font-bold text-slate-900 font-display">
                Final Assessment
              </h4>
            </div>

            <div className="text-sm sm:text-base font-bold flex items-center gap-2">
              <span className="text-slate-700">STATUS:</span>
              <span className={`${
                isPass ? 'text-emerald-700' : isViolation ? 'text-rose-700' : 'text-amber-700'
              } font-black tracking-tight`}>
                {statusLabel}
              </span>
            </div>

            <p className="text-xs sm:text-sm text-slate-700 leading-relaxed max-w-2xl font-normal">
              {isPass ? (
                'The package appears fully compliant based on the information that could be reliably assessed. All statutory declarations meet the mandatory rules under Legal Metrology.'
              ) : isViolation ? (
                'One or more confirmed statutory violations were established from the packaging label evidence. Rectification is required prior to commercial distribution.'
              ) : (
                'The package appears substantially compliant based on the information that could be reliably extracted. No confirmed high-confidence Legal Metrology violation was detected. Some requirements cannot be conclusively evaluated from a 2D image, including physical font dimensions and certain package-geometry requirements.'
              )}
            </p>
          </div>

          {/* Recommended Action (Right ~4 cols) */}
          <div className="md:col-span-4 bg-white/90 border border-slate-200/80 rounded-xl p-4">
            <h5 className="text-xs sm:text-sm font-bold text-slate-900 mb-1.5">
              Recommended Action:
            </h5>
            <p className="text-xs sm:text-sm text-slate-700 leading-relaxed font-normal">
              {isPass ? (
                'No regulatory enforcement action required. Retain screening record for compliance audit.'
              ) : isViolation ? (
                'Verify and rectify the non-compliant packaging declarations before release.'
              ) : (
                'Complete the highlighted manual inspections before issuing a final compliance determination.'
              )}
            </p>
          </div>

        </div>
      </div>

      {/* Footer Disclaimer & Rule Engine Version */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 px-1 pt-1 text-xs text-slate-500 font-medium">
        <p>
          This is an automated compliance screening result. It is not a statutory certificate, legal opinion, or final enforcement determination.
        </p>
        <span className="font-mono text-slate-500 shrink-0 font-semibold text-xs">
          Rule Engine Version: {ruleEngineVersion}
        </span>
      </div>
    </div>
  );
}

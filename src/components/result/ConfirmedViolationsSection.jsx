import React from 'react';
import { XCircle } from 'lucide-react';

export default function ConfirmedViolationsSection({ violations = [] }) {
  const count = violations.length;

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-4 sm:p-5">
      <div className="flex items-center gap-2 mb-3">
        <div className="w-5 h-5 rounded-full bg-rose-100 text-rose-700 flex items-center justify-center shrink-0">
          <XCircle className="w-3.5 h-3.5" />
        </div>
        <h4 className="text-base sm:text-lg font-bold text-slate-900 font-display">
          Confirmed Violations ({count})
        </h4>
      </div>

      {count === 0 ? (
        <div className="bg-slate-50/80 border border-slate-200/60 rounded-xl p-3.5">
          <strong className="block text-slate-900 font-bold text-sm sm:text-base mb-0.5">
            None
          </strong>
          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
            No confirmed Legal Metrology violation was established from the available evidence.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {violations.map((v, index) => (
            <div 
              key={v.id || index}
              className="bg-rose-50/50 border border-rose-200 rounded-xl p-4 space-y-2.5"
            >
              <div className="flex items-center justify-between gap-2">
                <div className="flex items-center gap-2">
                  <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-black bg-rose-600 text-white uppercase tracking-wider">
                    {v.severity || 'HIGH'}
                  </span>
                  <strong className="text-sm sm:text-base font-bold text-slate-900">
                    {v.title || 'Statutory Non-Compliance'}
                  </strong>
                </div>
              </div>

              <p className="text-xs sm:text-sm text-slate-800 leading-relaxed">
                {v.legalReason || v.explanation || v.detected || 'A statutory requirement was found to be in non-compliance.'}
              </p>

              <div className="text-xs sm:text-sm text-slate-600 font-medium pt-1 border-t border-rose-100">
                Rule: {v.rule || v.ruleReference || 'Legal Metrology (Packaged Commodities) Rules, 2011'}
              </div>

              {v.recommendation && (
                <div className="text-xs sm:text-sm font-semibold text-rose-800">
                  <span className="font-bold">Action:</span> {v.recommendation}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

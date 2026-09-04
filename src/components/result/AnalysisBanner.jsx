import React from 'react';

export default function AnalysisBanner() {
  return (
    <div className="bg-white rounded-xl border border-slate-200/90 shadow-2xs px-4 py-3 mb-5 flex flex-col sm:flex-row sm:items-center justify-between gap-2.5">
      <div className="flex items-center gap-3">
        <span className="w-2.5 h-2.5 rounded-full bg-blue-600 animate-pulse shrink-0"></span>
        <div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-bold text-slate-900 tracking-tight">
              Automated Label Analysis
            </span>
          </div>
          <p className="text-xs sm:text-sm text-slate-600 font-medium">
            Automated compliance screening — verify findings against the original packaging.
          </p>
        </div>
      </div>

      <div className="flex items-center gap-2 self-start sm:self-auto shrink-0">
        <span className="inline-flex items-center px-3 py-1 rounded-full text-xs font-bold bg-blue-50 text-gov-blue border border-blue-200/80">
          Analysis Ready
        </span>
        <span className="hidden md:inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold bg-slate-100 text-slate-700">
          Confidence: High
        </span>
      </div>
    </div>
  );
}

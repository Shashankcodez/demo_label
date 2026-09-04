import React from 'react';
import { CheckCircle2, AlertTriangle, XCircle, Eye } from 'lucide-react';

export default function ComplianceSummary({ 
  passCount = 0, 
  warningCount = 0, 
  violationCount = 0, 
  manualReviewCount = 0 
}) {
  return (
    <div className="mb-6">
      <h3 className="text-base sm:text-lg font-bold text-slate-900 mb-3.5 font-display">
        Compliance Summary
      </h3>

      <div className="grid grid-cols-2 md:grid-cols-4 gap-3.5 sm:gap-4">
        
        {/* Checks Passed Card */}
        <div className="bg-emerald-50/70 border border-emerald-200 rounded-2xl p-4 sm:p-5 flex items-center gap-3.5 shadow-xs">
          <div className="w-12 h-12 rounded-xl bg-emerald-600 text-white flex items-center justify-center shrink-0 shadow-sm">
            <CheckCircle2 className="w-7 h-7" />
          </div>
          <div>
            <span className="text-3xl sm:text-4xl font-black text-slate-900 font-display block leading-none">
              {passCount}
            </span>
            <span className="text-xs sm:text-sm font-bold text-slate-700 mt-1.5 block">
              Checks Passed
            </span>
          </div>
        </div>

        {/* Warnings Card */}
        <div className="bg-amber-50/70 border border-amber-200 rounded-2xl p-4 sm:p-5 flex items-center gap-3.5 shadow-xs">
          <div className="w-12 h-12 rounded-xl bg-amber-500 text-white flex items-center justify-center shrink-0 shadow-sm">
            <AlertTriangle className="w-6 h-6 fill-white" />
          </div>
          <div>
            <span className="text-3xl sm:text-4xl font-black text-slate-900 font-display block leading-none">
              {warningCount}
            </span>
            <span className="text-xs sm:text-sm font-bold text-slate-700 mt-1.5 block">
              Warnings
            </span>
          </div>
        </div>

        {/* Confirmed Violations Card */}
        <div className="bg-rose-50/70 border border-rose-200 rounded-2xl p-4 sm:p-5 flex items-center gap-3.5 shadow-xs">
          <div className="w-12 h-12 rounded-xl bg-rose-600 text-white flex items-center justify-center shrink-0 shadow-sm">
            <XCircle className="w-7 h-7" />
          </div>
          <div>
            <span className="text-3xl sm:text-4xl font-black text-slate-900 font-display block leading-none">
              {violationCount}
            </span>
            <span className="text-xs sm:text-sm font-bold text-slate-700 mt-1.5 block">
              Confirmed Violations
            </span>
          </div>
        </div>

        {/* Manual Verification Card */}
        <div className="bg-blue-50/70 border border-blue-200 rounded-2xl p-4 sm:p-5 flex items-center gap-3.5 shadow-xs">
          <div className="w-12 h-12 rounded-xl bg-blue-600 text-white flex items-center justify-center shrink-0 shadow-sm">
            <Eye className="w-6 h-6" />
          </div>
          <div>
            <span className="text-3xl sm:text-4xl font-black text-slate-900 font-display block leading-none">
              {manualReviewCount}
            </span>
            <span className="text-xs sm:text-sm font-bold text-slate-700 mt-1.5 block">
              Manual Verification
            </span>
          </div>
        </div>

      </div>
    </div>
  );
}

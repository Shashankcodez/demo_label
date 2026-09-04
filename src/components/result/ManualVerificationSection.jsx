import React from 'react';
import { Eye } from 'lucide-react';

export default function ManualVerificationSection({ items = [] }) {
  const count = items.length;

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-4 sm:p-5">
      <div className="flex items-center gap-2 mb-3">
        <div className="w-5 h-5 rounded-full bg-blue-100 text-gov-blue flex items-center justify-center shrink-0">
          <Eye className="w-3.5 h-3.5" />
        </div>
        <h4 className="text-base sm:text-lg font-bold text-slate-900 font-display">
          Manual Verification Required ({count})
        </h4>
      </div>

      {count === 0 ? (
        <div className="bg-slate-50/80 border border-slate-200/60 rounded-xl p-3.5">
          <strong className="block text-slate-900 font-bold text-sm sm:text-base mb-0.5">
            None
          </strong>
          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
            No physical declarations require manual verification from physical inspection.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {items.map((item, index) => {
            const num = item.number || index + 1;
            const title = item.title || `Check ${num}`;
            const text = item.explanation || item.manualReviewReason || item.detected || 'Requires verification against physical package.';
            const action = item.action || item.recommendation || 'Verify against the physical package.';

            return (
              <div key={item.id || index} className="flex items-start gap-3.5 text-xs sm:text-sm">
                <div className="w-6 h-6 rounded-lg bg-blue-50 text-gov-blue font-bold text-xs sm:text-sm flex items-center justify-center shrink-0 mt-0.5 border border-blue-200">
                  {num}
                </div>
                <div className="space-y-1.5 flex-1">
                  <strong className="block text-sm sm:text-base font-bold text-slate-900">
                    {num}. {title}
                  </strong>
                  <p className="text-xs sm:text-sm text-slate-700 leading-relaxed">
                    {text}
                  </p>
                  <div className="text-xs sm:text-sm text-slate-900 font-medium">
                    <span className="font-bold text-gov-blue">Action:</span> {action}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

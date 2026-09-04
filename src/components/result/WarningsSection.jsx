import React from 'react';
import { AlertTriangle } from 'lucide-react';

export default function WarningsSection({ warnings = [] }) {
  const count = warnings.length;

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-4 sm:p-5">
      <div className="flex items-center gap-2 mb-3">
        <div className="w-5 h-5 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
          <AlertTriangle className="w-3.5 h-3.5 fill-amber-700 text-amber-100" />
        </div>
        <h4 className="text-base sm:text-lg font-bold text-slate-900 font-display">
          Warnings ({count})
        </h4>
      </div>

      {count === 0 ? (
        <div className="bg-slate-50/80 border border-slate-200/60 rounded-xl p-3.5">
          <strong className="block text-slate-900 font-bold text-sm sm:text-base mb-0.5">
            None
          </strong>
          <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
            No statutory warnings identified from the available packaging label.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          {warnings.map((w, index) => (
            <div key={w.id || index} className="space-y-2 border-b border-slate-100 last:border-0 pb-3.5 last:pb-0">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-4 h-4 text-amber-500 fill-amber-500/20 shrink-0" />
                <h5 className="text-sm sm:text-base font-bold text-slate-900">
                  {w.title}
                </h5>
              </div>

              <p className="text-xs sm:text-sm text-slate-700 leading-relaxed pl-6">
                {w.explanation || w.legalReason || w.detected}
              </p>

              <div className="ml-6 bg-amber-50/90 border border-amber-200 rounded-xl p-3 text-xs sm:text-sm text-amber-950 flex items-start gap-2.5">
                <AlertTriangle className="w-4 h-4 text-amber-600 shrink-0 mt-0.5" />
                <div>
                  <span className="font-bold text-amber-900">Recommendation</span>
                  <p className="text-xs sm:text-sm text-amber-850 mt-0.5">
                    {w.recommendation || 'Inspect the physical package to confirm compliance.'}
                  </p>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

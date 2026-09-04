import React from 'react';
import { FileText, CheckCircle2, Info } from 'lucide-react';

export default function RegulatoryCoverage() {
  const metrologyRules = [
    'MRP',
    'Net Quantity',
    'Unit Sale Price',
    'Manufacturer/Packer information',
    'Consumer Care',
    'Applicable date declaration'
  ];

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-4 sm:p-5 flex flex-col justify-between">
      <div>
        <div className="flex items-center gap-2 mb-3">
          <FileText className="w-4 h-4 text-gov-blue" />
          <h4 className="text-base sm:text-lg font-bold text-slate-900 font-display">
            Regulatory Coverage
          </h4>
        </div>

        <div className="space-y-3">
          <div>
            <h5 className="text-sm font-bold text-slate-800 mb-2">
              Legal Metrology (Packaged Commodities) Rules, 2011
            </h5>
            <div className="grid grid-cols-2 gap-y-2 gap-x-2 text-xs sm:text-sm">
              {metrologyRules.map((rule, idx) => (
                <div key={idx} className="flex items-center gap-2 text-slate-800">
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 fill-emerald-100 shrink-0" />
                  <span className="text-xs sm:text-sm font-semibold truncate">{rule}</span>
                </div>
              ))}
            </div>
          </div>

          <div className="pt-3 border-t border-slate-100">
            <div className="flex items-center gap-1 text-sm font-bold text-slate-800 mb-1">
              <span>Other Regulations</span>
              <Info className="w-4 h-4 text-slate-400" />
            </div>
            <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
              Ingredients, nutrition information, allergen declarations and other food-specific requirements are not automatically classified as Legal Metrology violations.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}

import React from 'react';
import { 
  X, 
  Printer, 
  Scale, 
  FileText
} from 'lucide-react';

export default function ReportModal({ product, onClose }) {
  if (!product) return null;

  const handlePrint = () => {
    window.print();
  };

  const currentDate = new Date().toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'long',
    year: 'numeric'
  });

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/75 backdrop-blur-sm flex items-center justify-center p-3 sm:p-6 overflow-y-auto">
      <div className="bg-white rounded-2xl max-w-3xl w-full max-h-[90vh] flex flex-col shadow-2xl border border-slate-200 overflow-hidden animate-fadeIn my-auto">
        
        {/* Modal Header */}
        <div className="p-4 sm:p-5 border-b border-slate-200 flex items-center justify-between bg-slate-50 shrink-0">
          <div className="flex items-center gap-2">
            <FileText className="w-5 h-5 text-gov-blue" />
            <h3 className="text-base sm:text-lg font-bold text-slate-900">
              Statutory Compliance Inspection Report
            </h3>
          </div>

          <div className="flex items-center gap-2">
            <button
              onClick={handlePrint}
              className="px-3 py-1.5 rounded-lg text-xs font-bold bg-gov-blue text-white hover:bg-blue-700 flex items-center gap-1.5 transition-colors shadow-sm"
              title="Print official certificate"
            >
              <Printer className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Print / Save PDF</span>
            </button>
            <button
              onClick={onClose}
              className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200 transition-colors"
              aria-label="Close modal"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Scrollable Report Body */}
        <div className="p-5 sm:p-8 overflow-y-auto text-slate-800 space-y-6 text-xs sm:text-sm print:p-0">
          
          {/* Official Emblem & Header Slip */}
          <div className="border-b-2 border-slate-900 pb-5 text-center">
            <div className="flex justify-center mb-2">
              <div className="w-10 h-10 rounded-full bg-slate-900 text-amber-400 flex items-center justify-center font-bold">
                <Scale className="w-6 h-6" />
              </div>
            </div>
            <div className="text-[11px] font-bold tracking-widest text-slate-500 uppercase">
              Smart India Hackathon 2026 • SIH26034 Evaluation Report
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-950 font-display mt-0.5">
              PACKAGED COMMODITY COMPLIANCE MEMORANDUM
            </h2>
            <div className="text-xs text-slate-600 mt-1">
              Under Section 18 of The Legal Metrology Act, 2009 & Packaged Commodities Rules, 2011
            </div>
          </div>

          {/* Report Meta Info Table */}
          <div className="grid grid-cols-2 sm:grid-cols-5 gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200 text-xs">
            <div>
              <span className="text-slate-400 block font-medium">Inspection ID</span>
              <strong className="text-slate-800 font-mono">INSP-2026-{product.barcode?.slice(-6) || '99201'}</strong>
            </div>
            <div>
              <span className="text-slate-400 block font-medium">Date of Scan</span>
              <strong className="text-slate-800">{currentDate}</strong>
            </div>
            <div>
              <span className="text-slate-400 block font-medium">Label Quality</span>
              <strong className="text-slate-800">{product.qualityLabel || 'Good Label'} ({product.detectedFieldsCount ?? 12}/12)</strong>
            </div>
            <div>
              <span className="text-slate-400 block font-medium">Overall Score</span>
              <strong className="text-slate-800 font-display text-sm">{product.overallScore} / 100</strong>
            </div>
            <div>
              <span className="text-slate-400 block font-medium">Statutory Verdict</span>
              <span className={`inline-block px-2 py-0.5 rounded text-[10px] font-extrabold uppercase ${
                product.status === 'PASS' ? 'bg-emerald-100 text-emerald-800' :
                product.status === 'WARNING' ? 'bg-amber-100 text-amber-900' :
                'bg-red-100 text-red-900'
              }`}>
                {product.complianceOutcome || (product.status === 'PASS' ? 'COMPLIANT' : product.status === 'WARNING' ? 'NEEDS REVIEW' : 'POTENTIAL VIOLATION')}
              </span>
            </div>
          </div>

          {/* Product Profile Table */}
          <div>
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2 border-l-2 border-gov-blue pl-2">
              Product & Packaging Profile
            </h4>
            <div className="border border-slate-200 rounded-xl overflow-hidden text-xs">
              <table className="w-full divide-y divide-slate-200">
                <tbody className="divide-y divide-slate-100">
                  <tr className="bg-slate-50/50">
                    <td className="py-2 px-3 font-semibold text-slate-600 w-1/3">Commodity Name</td>
                    <td className="py-2 px-3 text-slate-900 font-bold">{product.name}</td>
                  </tr>
                  <tr>
                    <td className="py-2 px-3 font-semibold text-slate-600">Category & Brand</td>
                    <td className="py-2 px-3 text-slate-800">{product.category} ({product.brand})</td>
                  </tr>
                  <tr className="bg-slate-50/50">
                    <td className="py-2 px-3 font-semibold text-slate-600">Maximum Retail Price (MRP)</td>
                    <td className="py-2 px-3 text-slate-900 font-semibold">{product.mrp} • USP: {product.unitSalePrice || 'N/A'}</td>
                  </tr>
                  <tr>
                    <td className="py-2 px-3 font-semibold text-slate-600">Net Quantity & Numeral Height</td>
                    <td className="py-2 px-3 text-slate-800">{product.netQuantity} (Detected: {product.fontHeightDetected}, Required: {product.fontHeightRequired})</td>
                  </tr>
                  <tr className="bg-slate-50/50">
                    <td className="py-2 px-3 font-semibold text-slate-600">Manufacturer & Address</td>
                    <td className="py-2 px-3 text-slate-800">{product.manufacturerAddress || product.manufacturer || 'Not detected'}</td>
                  </tr>
                  <tr>
                    <td className="py-2 px-3 font-semibold text-slate-600">Consumer Care Helpline</td>
                    <td className="py-2 px-3 text-slate-800">
                      Phone: {product.customerCare?.phone || 'Not detected'} • Email: {product.customerCare?.email || 'Not detected'}
                    </td>
                  </tr>
                  <tr className="bg-slate-50/50">
                    <td className="py-2 px-3 font-semibold text-slate-600">Country of Origin & FSSAI</td>
                    <td className="py-2 px-3 text-slate-800">Origin: {product.countryOfOrigin || 'Domestic'} • FSSAI: {product.fssaiLicense || 'Not detected'}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* Clause-by-Clause Findings Table */}
          <div>
            <h4 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2 border-l-2 border-gov-blue pl-2">
              Statutory Findings & Rule-by-Rule Compliance
            </h4>
            <div className="border border-slate-200 rounded-xl overflow-hidden text-xs">
              <table className="w-full divide-y divide-slate-200">
                <thead className="bg-slate-100 text-slate-700">
                  <tr>
                    <th className="py-2 px-3 text-left font-bold">Rule Reference</th>
                    <th className="py-2 px-3 text-left font-bold">Status</th>
                    <th className="py-2 px-3 text-left font-bold">Observation & Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {product.checks.map(c => (
                    <tr key={c.id} className={c.status === 'VIOLATION' ? 'bg-red-50/40' : c.status === 'WARNING' ? 'bg-amber-50/40' : ''}>
                      <td className="py-2.5 px-3 font-mono text-[11px] font-bold text-slate-800 align-top">
                        {c.rule}
                      </td>
                      <td className="py-2.5 px-3 align-top whitespace-nowrap">
                        <span className={`px-2 py-0.5 rounded text-[9px] font-bold uppercase ${
                          c.status === 'PASS' ? 'bg-emerald-100 text-emerald-800' :
                          c.status === 'WARNING' ? 'bg-amber-100 text-amber-900' :
                          'bg-red-100 text-red-900'
                        }`}>
                          {c.status === 'PASS' ? 'COMPLIANT' : c.status === 'WARNING' ? 'NEEDS REVIEW' : 'POTENTIAL VIOLATION'}
                        </span>
                      </td>
                      <td className="py-2.5 px-3 align-top text-slate-700">
                        <div className="font-semibold text-slate-900">{c.title}</div>
                        <div className="text-[11px] text-slate-600 mt-0.5">{c.detected}</div>
                        {c.status !== 'PASS' && (
                          <div className="text-[11px] font-medium text-amber-900 mt-1">
                            Action: {c.recommendation}
                          </div>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Legal Notice Disclaimer */}
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 text-[10px] text-slate-500 leading-relaxed">
            <strong>Notice / Disclaimer: </strong>
            This document is an automated screening memorandum generated by LabelCheck (SIH26034 Prototype). The optical evaluation relies on image analysis against the Standards of Weights and Measures Enforcement Act & Legal Metrology Rules, 2011. Pre-packaged items with potential non-compliance should undergo physical inspection before formal regulatory action. This memorandum does not constitute official legal certification.
          </div>

        </div>

        {/* Modal Footer */}
        <div className="p-4 border-t border-slate-200 bg-slate-50 flex items-center justify-between shrink-0">
          <span className="text-xs text-slate-500">
            Automated screening result generated by LabelCheck (SIH26034)
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 rounded-xl text-xs font-semibold bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition-colors"
          >
            Close Report
          </button>

        </div>

      </div>
    </div>
  );
}

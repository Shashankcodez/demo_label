import React from 'react';
import { 
  X, 
  Printer, 
  Scale, 
  FileText
} from 'lucide-react';
import { formatExtractionSource } from '../api/scanMapper';

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
              <div className="w-11 h-11 rounded-full bg-slate-900 text-amber-400 flex items-center justify-center font-bold shadow-sm">
                <Scale className="w-6 h-6" />
              </div>
            </div>
            <div className="text-xs font-bold tracking-widest text-slate-500 uppercase">
              Smart India Hackathon 2026 • SIH26034 Evaluation Report
            </div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-950 font-display mt-0.5">
              PACKAGED COMMODITY COMPLIANCE MEMORANDUM
            </h2>
            <div className="text-xs sm:text-sm text-slate-600 mt-1 flex flex-wrap items-center justify-between gap-2 font-medium">
              <span>Under Section 18 of The Legal Metrology Act, 2009 & Packaged Commodities Rules, 2011</span>
              <span className="font-semibold text-slate-700">Inspection Date: {currentDate}</span>
            </div>
          </div>

          {/* Report Meta Info Table */}
          <div className="grid grid-cols-2 sm:grid-cols-6 gap-3 bg-slate-50 p-4 rounded-xl border border-slate-200 text-xs sm:text-sm">
            <div>
              <span className="text-slate-500 block font-semibold text-xs">Inspection ID</span>
              <strong className="text-slate-800 font-mono text-xs sm:text-sm font-bold">INSP-2026-{product.barcode?.slice(-6) || '99201'}</strong>
            </div>
            <div>
              <span className="text-slate-500 block font-semibold text-xs">Extraction Engine</span>
              <strong className="text-slate-800 truncate block text-xs sm:text-sm font-bold" title={product.extractionSource}>
                {formatExtractionSource(product.extractionSource, product.aiModel)}
              </strong>
            </div>
            <div>
              <span className="text-slate-500 block font-semibold text-xs">Label Quality</span>
              <strong className="text-slate-800 text-xs sm:text-sm font-bold">{product.qualityLabel || 'Good Label'} ({product.detectedFieldsCount ?? 12}/12)</strong>
            </div>
            <div>
              <span className="text-slate-500 block font-semibold text-xs">Extraction Conf.</span>
              <strong className="text-slate-800 text-xs sm:text-sm font-bold">{Math.round((product.overallExtractionConfidence || 0.85) * 100)}%</strong>
            </div>
            <div>
              <span className="text-slate-500 block font-semibold text-xs">Compliance Score</span>
              <strong className="text-slate-800 font-display text-sm sm:text-base font-bold">{product.overallScore} / 100</strong>
            </div>
            <div>
              <span className="text-slate-500 block font-semibold text-xs">Statutory Verdict</span>
              <span className={`inline-block px-2.5 py-0.5 rounded text-xs font-extrabold uppercase ${
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
            <h4 className="text-xs sm:text-sm font-bold text-slate-900 uppercase tracking-wider mb-2 border-l-3 border-gov-blue pl-2.5">
              Product & Packaging Profile
            </h4>
            <div className="border border-slate-200 rounded-xl overflow-hidden text-xs sm:text-sm">
              <table className="w-full divide-y divide-slate-200">
                <tbody className="divide-y divide-slate-100">
                  <tr className="bg-slate-50/50">
                    <td className="py-2.5 px-3.5 font-semibold text-slate-600 w-1/3">Commodity Name</td>
                    <td className="py-2.5 px-3.5 text-slate-900 font-bold">{product.name}</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3.5 font-semibold text-slate-600">Category & Brand</td>
                    <td className="py-2.5 px-3.5 text-slate-800 font-medium">{product.category} ({product.brand})</td>
                  </tr>
                  <tr className="bg-slate-50/50">
                    <td className="py-2.5 px-3.5 font-semibold text-slate-600">Maximum Retail Price (MRP)</td>
                    <td className="py-2.5 px-3.5 text-slate-900 font-bold">{product.mrp} • USP: {product.unitSalePrice || 'N/A'}</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3.5 font-semibold text-slate-600">Net Quantity & Numeral Height</td>
                    <td className="py-2.5 px-3.5 text-slate-800 font-medium">{product.netQuantity} (Detected: {product.fontHeightDetected}, Required: {product.fontHeightRequired})</td>
                  </tr>
                  <tr className="bg-slate-50/50">
                    <td className="py-2.5 px-3.5 font-semibold text-slate-600">Manufacturer & Address</td>
                    <td className="py-2.5 px-3.5 text-slate-800 font-medium">{product.manufacturerAddress || product.manufacturer || 'Not detected'}</td>
                  </tr>
                  <tr>
                    <td className="py-2.5 px-3.5 font-semibold text-slate-600">Consumer Care Helpline</td>
                    <td className="py-2.5 px-3.5 text-slate-800 font-medium">
                      Phone: {product.customerCare?.phone || 'Not detected'} • Email: {product.customerCare?.email || 'Not detected'}
                    </td>
                  </tr>
                  <tr className="bg-slate-50/50">
                    <td className="py-2.5 px-3.5 font-semibold text-slate-600">Country of Origin & FSSAI</td>
                    <td className="py-2.5 px-3.5 text-slate-800 font-medium">Origin: {product.countryOfOrigin || 'Domestic'} • FSSAI: {product.fssaiLicense || 'Not detected'}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          {/* Clause-by-Clause Findings Table */}
          <div>
            <h4 className="text-xs sm:text-sm font-bold text-slate-900 uppercase tracking-wider mb-2 border-l-3 border-gov-blue pl-2.5">
              Statutory Findings & Rule-by-Rule Compliance
            </h4>
            <div className="border border-slate-200 rounded-xl overflow-hidden text-xs sm:text-sm">
              <table className="w-full divide-y divide-slate-200">
                <thead className="bg-slate-100 text-slate-800">
                  <tr>
                    <th className="py-2.5 px-3.5 text-left font-bold">Rule Reference</th>
                    <th className="py-2.5 px-3.5 text-left font-bold">Status</th>
                    <th className="py-2.5 px-3.5 text-left font-bold">Observation & Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {product.checks.map(c => (
                    <tr key={c.id} className={c.status === 'VIOLATION' ? 'bg-red-50/40' : c.status === 'WARNING' ? 'bg-amber-50/40' : ''}>
                      <td className="py-3 px-3.5 font-mono text-xs font-bold text-slate-800 align-top">
                        {c.rule}
                      </td>
                      <td className="py-3 px-3.5 align-top whitespace-nowrap">
                        <span className={`px-2.5 py-0.5 rounded text-xs font-bold uppercase ${
                          c.status === 'PASS' ? 'bg-emerald-100 text-emerald-800' :
                          c.status === 'WARNING' ? 'bg-amber-100 text-amber-900' :
                          'bg-red-100 text-red-900'
                        }`}>
                          {c.status === 'PASS' ? 'COMPLIANT' : c.status === 'WARNING' ? 'NEEDS REVIEW' : 'POTENTIAL VIOLATION'}
                        </span>
                      </td>
                      <td className="py-3 px-3.5 align-top text-slate-700">
                        <div className="font-bold text-slate-900 text-xs sm:text-sm">{c.title}</div>
                        <div className="text-xs text-slate-600 mt-1">{c.detected}</div>
                        {c.status !== 'PASS' && (
                          <div className="text-xs font-semibold text-amber-950 mt-1.5 bg-amber-50/80 p-2 rounded-lg border border-amber-200">
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
          <div className="p-3.5 bg-slate-50 rounded-xl border border-slate-200 text-xs text-slate-600 leading-relaxed font-medium">
            <strong className="text-slate-800 font-bold">Statutory Disclaimer: </strong>
            Automated pre-compliance screening based on visible image evidence. Not an official certification or legal determination. Pre-packaged items with potential non-compliance should undergo physical inspection before formal regulatory action under The Legal Metrology Act, 2009 and Food Safety and Standards Act, 2006.
          </div>

        </div>

        {/* Modal Footer */}
        <div className="p-4 sm:p-5 border-t border-slate-200 bg-slate-50 flex items-center justify-between shrink-0">
          <span className="text-xs sm:text-sm text-slate-600 font-medium">
            Automated screening result generated by LabelCheck (SIH26034)
          </span>
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl text-xs sm:text-sm font-bold bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 transition-colors cursor-pointer"
          >
            Close Report
          </button>
        </div>

      </div>
    </div>
  );
}

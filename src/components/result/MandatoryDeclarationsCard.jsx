import React from 'react';
import { FileText, ChevronRight, Check, Eye, X } from 'lucide-react';

export default function MandatoryDeclarationsCard({ product }) {
  if (!product) return null;

  // Build rows dynamically from product.statutoryFields if available
  const statutoryFields = product.statutoryFields || [];

  const dynamicRows = statutoryFields.length > 0
    ? statutoryFields.map((f) => {
        let status = f.detected ? 'PASS' : 'REVIEW';
        if (f.key === 'unitSalePrice' && product.unitSalePrice === 'Missing') {
          status = 'FAIL';
        }
        return {
          id: f.key,
          requirement: f.name,
          detectedText: f.value || f.evidence || 'Not clearly detected',
          status: status,
          confidence: f.confidence,
          rule: f.rule,
          hasBox: Array.isArray(f.boundingBox) && f.boundingBox.length === 4
        };
      })
    : [];

  // Default rows matching standard statutory declarations
  const defaultRows = [
    {
      id: 'generic-name',
      requirement: 'Generic Commodity Name',
      detectedText: product.name || 'Packaged Commodity',
      status: product.name ? 'PASS' : 'REVIEW',
      hasBox: false
    },
    {
      id: 'net-quantity',
      requirement: 'Net Quantity',
      detectedText: product.netQuantity || product.netQuantityRaw || 'Not detected',
      status: product.netQuantity ? 'PASS' : 'REVIEW',
      hasBox: false
    },
    {
      id: 'mrp',
      requirement: 'Maximum Retail Price (MRP)',
      detectedText: product.mrp || 'Not clearly detected',
      status: product.mrp ? 'PASS' : 'REVIEW',
      hasBox: false
    },
    {
      id: 'unit-sale-price',
      requirement: 'Unit Sale Price',
      detectedText: product.unitSalePrice || 'Not clearly detected',
      status: product.unitSalePrice === 'Missing' ? 'FAIL' : (product.unitSalePrice ? 'PASS' : 'REVIEW'),
      hasBox: false
    },
    {
      id: 'mfg-details',
      requirement: 'Manufacturer / Packer Details',
      detectedText: product.manufacturer || 'Not clearly detected',
      status: product.manufacturer ? 'PASS' : 'REVIEW',
      hasBox: false
    },
    {
      id: 'mfg-date',
      requirement: 'Manufacture / Packing Date',
      detectedText: product.mfgDate || 'Not clearly detected',
      status: product.mfgDate ? 'PASS' : 'REVIEW',
      hasBox: false
    },
    {
      id: 'care-phone',
      requirement: 'Consumer Care Telephone',
      detectedText: product.customerCare?.phone || 'Not provided',
      status: product.customerCare?.phone && product.customerCare.phone !== 'Not provided' ? 'PASS' : 'REVIEW',
      hasBox: false
    },
    {
      id: 'care-email',
      requirement: 'Consumer Care Email',
      detectedText: product.customerCare?.email || 'Not provided',
      status: product.customerCare?.email ? 'PASS' : 'REVIEW',
      hasBox: false
    },
    {
      id: 'origin',
      requirement: 'Country of Origin',
      detectedText: product.countryOfOrigin || 'Not clearly detected',
      status: product.countryOfOrigin && !product.countryOfOrigin.toLowerCase().includes('not') ? 'PASS' : 'REVIEW',
      hasBox: false
    }
  ];

  const baseRows = dynamicRows.length > 0 ? dynamicRows : defaultRows;

  // Add standard verification checks (Font size & PDP)
  const rows = [
    ...baseRows,
    {
      id: 'font-size',
      requirement: 'Font Size (as per rules)',
      detectedText: product.fontHeightDetected?.includes('Text detected') 
        ? product.fontHeightDetected 
        : 'Text detected, size not verifiable from single photo',
      status: 'REVIEW',
      hasBox: false
    },
    {
      id: 'pdp',
      requirement: 'Principal Display Panel (PDP)',
      detectedText: 'Verified front/display panel coverage',
      status: 'REVIEW',
      hasBox: false
    }
  ];

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-4 sm:p-5">
      <div className="flex items-center gap-2 mb-4">
        <FileText className="w-4 h-4 text-gov-blue" />
        <h4 className="text-base sm:text-lg font-bold text-slate-900 font-display">
          Mandatory Declarations
        </h4>
      </div>

      <div className="overflow-x-auto -mx-4 sm:mx-0">
        <table className="w-full text-left border-collapse text-xs sm:text-sm">
          <thead>
            <tr className="border-b border-slate-200 bg-slate-50/80 text-slate-600 font-bold uppercase tracking-wider text-xs">
              <th className="py-3 px-3.5">Requirement</th>
              <th className="py-3 px-3.5">Detected Text</th>
              <th className="py-3 px-3.5 text-right">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {rows.map((row) => (
              <tr key={row.id} className="hover:bg-slate-50/70 transition-colors">
                <td className="py-3 px-3.5 text-slate-800 font-semibold whitespace-nowrap">
                  <span className="inline-flex items-center gap-2">
                    <ChevronRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                    {row.requirement}
                  </span>
                </td>
                <td className="py-3 px-3.5 text-slate-700 font-normal max-w-[240px]" title={row.detectedText}>
                  <div className="flex items-center gap-2">
                    {row.hasBox && (
                      <span className="shrink-0 w-2.5 h-2.5 rounded-full bg-emerald-500 ring-2 ring-emerald-100" title="Spatial region located on packaging image" />
                    )}
                    <span className="truncate font-medium">{row.detectedText}</span>
                  </div>
                </td>
                <td className="py-3 px-3.5 text-right whitespace-nowrap">
                  {row.status === 'PASS' ? (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-emerald-50 text-emerald-700 border border-emerald-200">
                      <Check className="w-3.5 h-3.5 stroke-[3]" /> Pass
                    </span>
                  ) : row.status === 'FAIL' ? (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                      <X className="w-3.5 h-3.5 stroke-[3]" /> Fail
                    </span>
                  ) : (
                    <span className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-blue-50 text-blue-700 border border-blue-200">
                      <Eye className="w-3.5 h-3.5 stroke-[2.5]" /> Review
                    </span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

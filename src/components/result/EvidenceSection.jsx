import React, { useState } from 'react';
import { Image as ImageIcon } from 'lucide-react';
import EvidenceModal from './EvidenceModal';

export default function EvidenceSection({ product }) {
  const [selectedEvidence, setSelectedEvidence] = useState(null);

  // Default 4 evidence items matching reference dashboard
  const defaultEvidence = [
    {
      id: 'ev-mrp',
      title: 'MRP Evidence',
      imageUrl: '/evidence_mrp.png',
      caption: 'MRP ₹145 Incl. of All Taxes'
    },
    {
      id: 'ev-qty',
      title: 'Net Quantity Evidence',
      imageUrl: '/evidence_qty.png',
      caption: '1L Net Quantity'
    },
    {
      id: 'ev-mfg',
      title: 'Manufacturer Evidence',
      imageUrl: '/evidence_mfg.png',
      caption: 'Manufactured & Marketed by: ABC Foods Pvt Ltd'
    },
    {
      id: 'ev-care',
      title: 'Consumer Care Evidence',
      imageUrl: '/evidence_care.png',
      caption: 'Customer Care: 1800-123-4567, care@abcfoods.in'
    }
  ];

  const evidenceItems = product?.evidenceList || defaultEvidence;

  return (
    <>
      <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-4 sm:p-5">
        <div className="flex items-center gap-2 mb-4">
          <ImageIcon className="w-4 h-4 text-gov-blue" />
          <h4 className="text-base sm:text-lg font-bold text-slate-900 font-display">
            Evidence (Extracted from Image)
          </h4>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {evidenceItems.slice(0, 4).map((item) => (
            <button
              key={item.id}
              onClick={() => setSelectedEvidence(item)}
              className="flex flex-col items-center group cursor-pointer focus:outline-hidden text-left"
            >
              <div className="w-full aspect-4/3 rounded-xl border border-slate-200/80 bg-slate-50 overflow-hidden flex flex-col justify-between p-2 group-hover:border-blue-500 group-hover:shadow-sm transition-all relative">
                <div className="w-full h-full flex items-center justify-center overflow-hidden">
                  <img
                    src={item.imageUrl}
                    alt={item.title}
                    className="max-h-full max-w-full object-contain group-hover:scale-105 transition-transform"
                  />
                </div>
                {item.caption && (
                  <div className="absolute inset-x-1.5 bottom-1.5 bg-slate-900/90 backdrop-blur-xs text-white rounded-md px-2 py-1 text-xs font-mono truncate text-center shadow-xs">
                    "{item.caption}"
                  </div>
                )}
              </div>
              <div className="flex items-center gap-1.5 mt-2">
                <span className="text-xs sm:text-sm font-bold text-slate-700 text-center group-hover:text-gov-blue transition-colors truncate max-w-[130px]">
                  {item.title}
                </span>
                {typeof item.confidence === 'number' && (
                  <span className="text-xs font-bold text-emerald-700 bg-emerald-50 px-1.5 py-0.5 rounded border border-emerald-200">
                    {Math.round(item.confidence * 100)}%
                  </span>
                )}
              </div>
            </button>
          ))}
        </div>
      </div>

      {selectedEvidence && (
        <EvidenceModal
          evidence={selectedEvidence}
          onClose={() => setSelectedEvidence(null)}
        />
      )}
    </>
  );
}

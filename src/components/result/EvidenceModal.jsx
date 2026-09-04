import React from 'react';
import { X, ZoomIn } from 'lucide-react';

export default function EvidenceModal({ evidence, onClose }) {
  if (!evidence) return null;

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-xs flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-lg w-full shadow-2xl border border-slate-200 overflow-hidden animate-fadeIn">
        <div className="p-4 border-b border-slate-200 flex items-center justify-between bg-slate-50">
          <div className="flex items-center gap-2">
            <ZoomIn className="w-4 h-4 text-gov-blue" />
            <h4 className="text-sm font-bold text-slate-900">
              {evidence.title}
            </h4>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-200 transition-colors"
            aria-label="Close modal"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 flex flex-col items-center">
          <div className="rounded-xl border border-slate-200 overflow-hidden bg-slate-100 p-2 max-h-72 flex items-center justify-center">
            <img
              src={evidence.imageUrl}
              alt={evidence.title}
              className="max-h-64 object-contain rounded-lg shadow-sm"
            />
          </div>
          {evidence.caption && (
            <p className="text-xs text-slate-600 mt-4 text-center font-medium bg-slate-50 px-3 py-1.5 rounded-lg border border-slate-200/80">
              {evidence.caption}
            </p>
          )}
        </div>
      </div>
    </div>
  );
}

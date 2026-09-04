import React, { useState } from 'react';
import { ChevronDown, ChevronUp, Cpu } from 'lucide-react';

export default function TechnicalDetailsDrawer({ product }) {
  const [isOpen, setIsOpen] = useState(false);

  if (!product) return null;

  return (
    <div className="mt-8 border-t border-slate-200/80 pt-4">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between px-4 py-3 rounded-xl bg-slate-100 hover:bg-slate-200/70 text-slate-700 text-xs sm:text-sm font-bold transition-colors"
      >
        <div className="flex items-center gap-2">
          <Cpu className="w-4 h-4 text-slate-500" />
          <span>Technical Diagnostics & Audit Details (Advanced)</span>
        </div>
        {isOpen ? <ChevronUp className="w-4 h-4" /> : <ChevronDown className="w-4 h-4" />}
      </button>

      {isOpen && (
        <div className="mt-3 p-4 sm:p-5 bg-slate-50 rounded-xl border border-slate-200 text-xs sm:text-sm space-y-4 animate-fadeIn">
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
            <div>
              <span className="text-xs uppercase font-bold text-slate-500 block mb-0.5">Scan Identifier</span>
              <strong className="text-slate-800 font-mono text-xs sm:text-sm font-bold">{product.scanId || product.id}</strong>
            </div>
            <div>
              <span className="text-xs uppercase font-bold text-slate-500 block mb-0.5">Rule Engine</span>
              <strong className="text-slate-800 font-mono text-xs sm:text-sm font-bold">{product.ruleEngineVersion || 'LM-PCR-2026.01'}</strong>
            </div>
            <div>
              <span className="text-xs uppercase font-bold text-slate-500 block mb-0.5">Confidence Score</span>
              <strong className="text-slate-800 text-xs sm:text-sm font-bold">
                {Math.round((product.overallExtractionConfidence || 0.85) * 100)}%
              </strong>
            </div>
            <div>
              <span className="text-xs uppercase font-bold text-slate-500 block mb-0.5">Processing Timestamp</span>
              <strong className="text-slate-800 text-xs sm:text-sm font-bold">{product.scannedAt || 'Recent'}</strong>
            </div>
          </div>

          {product.rawOcrText && (
            <div className="pt-3 border-t border-slate-200">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center gap-2">
                  <span className="text-xs uppercase font-bold text-slate-600 block">
                    Full Extracted Text Stream
                  </span>
                  <span className="text-xs font-semibold text-slate-600 bg-slate-200/80 px-2 py-0.5 rounded-md">
                    {product.rawOcrText.length} chars • {product.rawOcrText.split('\n').filter(Boolean).length} lines
                  </span>
                </div>
                <CopyButton text={product.rawOcrText} />
              </div>
              <div className="bg-white p-3.5 rounded-lg border border-slate-200 font-mono text-xs sm:text-sm text-slate-800 max-h-56 overflow-y-auto whitespace-pre-wrap leading-relaxed select-text shadow-2xs">
                {product.rawOcrText}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function CopyButton({ text }) {
  const [copied, setCopied] = useState(false);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(text);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Fallback
    }
  };

  return (
    <button
      type="button"
      onClick={handleCopy}
      className="inline-flex items-center gap-1 text-xs font-bold text-slate-700 hover:text-slate-900 bg-white hover:bg-slate-50 border border-slate-300 px-2.5 py-1 rounded-md shadow-2xs transition-all cursor-pointer"
    >
      <span>{copied ? '✓ Copied' : 'Copy Text'}</span>
    </button>
  );
}

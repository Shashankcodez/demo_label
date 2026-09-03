import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { X, Smartphone, Wifi, Copy, Check } from 'lucide-react';

export default function MobileQrModal({ isOpen, onClose }) {
  const [copied, setCopied] = useState(false);

  if (!isOpen) return null;
  
  // Local network IP detected from the system
  const networkIp = '192.168.0.122';
  const networkUrl = `http://${networkIp}:5173/`;

  const handleCopy = () => {
    navigator.clipboard.writeText(networkUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl max-w-sm w-full p-6 shadow-2xl border border-slate-200 text-center animate-fadeIn relative">
        
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 p-1.5 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100 transition-colors"
          aria-label="Close modal"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Modal Header */}
        <div className="w-12 h-12 rounded-2xl bg-blue-50 text-gov-blue flex items-center justify-center mx-auto mb-3">
          <Smartphone className="w-6 h-6" />
        </div>

        <h3 className="text-lg font-bold text-slate-900">
          Run Demo on Your Phone
        </h3>
        <p className="text-xs text-slate-500 mt-1 mb-5">
          Scan this QR code with your phone camera to test live mobile photo capture & responsive layout.
        </p>

        {/* QR Code Container */}
        <div className="bg-slate-50 p-4 rounded-2xl border border-slate-200 inline-block mx-auto mb-4 shadow-inner">
          <QRCodeSVG
            value={networkUrl}
            size={180}
            level="M"
            includeMargin={true}
            bgColor="#f8fafc"
            fgColor="#0f172a"
          />
        </div>

        {/* URL Box with Copy */}
        <div className="flex items-center justify-between bg-slate-100 px-3 py-2 rounded-xl text-xs font-mono text-slate-700 mb-4 border border-slate-200">
          <span className="truncate font-semibold">{networkUrl}</span>
          <button
            onClick={handleCopy}
            className="ml-2 text-gov-blue hover:text-blue-700 flex items-center gap-1 font-sans text-[11px] font-bold shrink-0"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-600" /> : <Copy className="w-3.5 h-3.5" />}
            {copied ? 'Copied' : 'Copy'}
          </button>
        </div>

        {/* Requirements notice */}
        <div className="bg-amber-50 rounded-xl p-3 text-left border border-amber-200/80 text-[11px] text-amber-900 flex items-start gap-2">
          <Wifi className="w-4 h-4 text-amber-700 shrink-0 mt-0.5" />
          <div>
            <strong>Requirement: </strong>
            Make sure your phone is connected to the same Wi-Fi network as this PC (<code className="font-semibold">192.168.0.xxx</code>).
          </div>
        </div>

        <button
          onClick={onClose}
          className="mt-5 w-full py-2.5 rounded-xl bg-slate-900 hover:bg-slate-800 text-white font-bold text-xs transition-colors"
        >
          Got It
        </button>

      </div>
    </div>
  );
}

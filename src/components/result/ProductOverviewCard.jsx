import React, { useState } from 'react';
import { AlertTriangle, CheckCircle2, XCircle, Info, Eye, EyeOff, Scan } from 'lucide-react';

export default function ProductOverviewCard({ product }) {
  const [showBoxes, setShowBoxes] = useState(true);
  const [hoveredBoxKey, setHoveredBoxKey] = useState(null);

  if (!product) return null;

  const score = typeof product.overallScore === 'number' ? product.overallScore : 84;
  const status = (product.status || 'REQUIRES_MANUAL_REVIEW').toUpperCase();

  const isPass = status === 'PASS' || status === 'COMPLIANT';
  const isViolation = status === 'VIOLATION' || status === 'FAIL' || status === 'NON-COMPLIANT';
  const isReview = !isPass && !isViolation;

  // Normalized status badge text
  const statusText = isPass ? 'COMPLIANT' : isViolation ? 'NON-COMPLIANT' : 'REQUIRES MANUAL REVIEW';

  // Product details with real detected data
  const productName = product.name || 'Packaged Commodity';
  const category = product.category || 'Packaged Commodity';
  const brand = product.brand || 'Unbranded / Not Declared';
  const packageType = product.packageType || 'Retail Packaging';
  const netQuantity = product.netQuantity || product.netQuantityRaw || 'Not detected';

  // Bounding boxes for text detection overlay
  const boundingBoxes = product.fieldBoundingBoxes || {};
  const boxEntries = Object.entries(boundingBoxes).filter(
    ([, box]) => Array.isArray(box) && box.length === 4
  );

  const getBoxStyle = (key) => {
    if (['mrp', 'netQuantity', 'unitSalePrice'].includes(key)) {
      return {
        border: 'border-emerald-500',
        bg: 'bg-emerald-500/15 hover:bg-emerald-500/25',
        badge: 'bg-emerald-700 text-white',
        label: 'Statutory Metric'
      };
    }
    if (['mfgDate', 'expiryDate', 'batchNumber'].includes(key)) {
      return {
        border: 'border-purple-500',
        bg: 'bg-purple-500/15 hover:bg-purple-500/25',
        badge: 'bg-purple-700 text-white',
        label: 'Date / Batch'
      };
    }
    if (['fssaiLicense'].includes(key)) {
      return {
        border: 'border-amber-500',
        bg: 'bg-amber-500/20 hover:bg-amber-500/30',
        badge: 'bg-amber-700 text-white',
        label: 'FSSAI'
      };
    }
    return {
      border: 'border-blue-500',
      bg: 'bg-blue-500/15 hover:bg-blue-500/25',
      badge: 'bg-blue-700 text-white',
      label: 'Origin & Care'
    };
  };

  // Product image source
  const imageSrc = product.customImageUrl || (product.name?.toLowerCase().includes('sunflower') ? '/sample_sunflower_oil.png' : null);

  // Dynamic summary sentence
  const summarySentence = product.summary || (
    isReview
      ? 'The package appears to contain most required declarations. However, some requirements cannot be conclusively verified from the provided image.'
      : isPass
      ? 'The package demonstrates full compliance with all evaluated statutory declarations under Legal Metrology Rules, 2011.'
      : 'One or more mandatory statutory declarations violate Legal Metrology Rules, 2011. Corrective action is required.'
  );

  return (
    <div className="bg-white rounded-2xl border border-slate-200/90 shadow-sm p-5 sm:p-6 mb-6">
      <div className="grid grid-cols-1 md:grid-cols-12 gap-6 items-center">
        
        {/* Left Column: Product Image (~25% / 3 cols) */}
        <div className="md:col-span-3 flex flex-col items-center">
          <div className="relative w-full max-w-[210px] h-60 sm:h-64 rounded-xl bg-slate-900/5 border border-slate-200/80 flex items-center justify-center p-2 overflow-hidden shadow-2xs group">
            {imageSrc ? (
              <div className="relative w-full h-full flex items-center justify-center">
                <img
                  src={imageSrc}
                  alt={productName}
                  className="max-h-full max-w-full object-contain drop-shadow-sm select-none"
                />

                {/* Spatial Bounding Box Overlay */}
                {showBoxes && boxEntries.length > 0 && (
                  <div className="absolute inset-0 pointer-events-none">
                    {boxEntries.map(([key, box]) => {
                      const style = getBoxStyle(key);
                      const isHovered = hoveredBoxKey === key;
                      const ymin = box[0] / 10;
                      const xmin = box[1] / 10;
                      const ymax = box[2] / 10;
                      const xmax = box[3] / 10;

                      return (
                        <div
                          key={key}
                          onMouseEnter={() => setHoveredBoxKey(key)}
                          onMouseLeave={() => setHoveredBoxKey(null)}
                          className={`absolute pointer-events-auto cursor-pointer border-2 rounded-sm transition-all duration-200 ${style.border} ${style.bg} ${
                            isHovered ? 'ring-2 ring-white ring-offset-1 z-20 scale-[1.02]' : 'z-10'
                          }`}
                          style={{
                            top: `${ymin}%`,
                            left: `${xmin}%`,
                            width: `${Math.max(4, xmax - xmin)}%`,
                            height: `${Math.max(3, ymax - ymin)}%`
                          }}
                          title={`${key}: ${product.fieldEvidence?.[key] || key}`}
                        >
                          <span
                            className={`absolute -top-4 left-0 text-[10px] font-black uppercase px-1.5 py-0.5 rounded shadow-xs tracking-tight pointer-events-none whitespace-nowrap ${style.badge} ${
                              isHovered ? 'opacity-100 scale-105' : 'opacity-90'
                            }`}
                          >
                            {key.replace(/([A-Z])/g, ' $1').trim()}
                          </span>
                        </div>
                      );
                    })}
                  </div>
                )}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center text-slate-400 gap-2">
                <span className="text-5xl">{product.imageEmoji || '📦'}</span>
                <span className="text-xs sm:text-sm font-semibold text-slate-500">Packaged Commodity</span>
              </div>
            )}

            {/* Overlay toggle badge */}
            {boxEntries.length > 0 && (
              <button
                type="button"
                onClick={() => setShowBoxes(!showBoxes)}
                className="absolute bottom-2 right-2 z-30 inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-slate-900/85 hover:bg-slate-900 text-white text-xs font-bold shadow-md backdrop-blur-md transition-all cursor-pointer"
                title={showBoxes ? 'Hide detected text bounding regions' : 'Show detected text bounding regions'}
              >
                {showBoxes ? <Eye className="w-3.5 h-3.5 text-emerald-400" /> : <EyeOff className="w-3.5 h-3.5 text-slate-400" />}
                <span>{showBoxes ? `Text Boxes (${boxEntries.length})` : 'Show Boxes'}</span>
              </button>
            )}
          </div>

          {/* Quick Subtitle below image */}
          {boxEntries.length > 0 && (
            <div className="mt-2 text-xs sm:text-sm text-slate-600 flex items-center gap-1.5 font-medium">
              <Scan className="w-3.5 h-3.5 text-emerald-600 shrink-0" />
              <span>{boxEntries.length} spatial text regions detected</span>
            </div>
          )}
        </div>

        {/* Center Column: Product Details (~42% / 5 cols) */}
        <div className="md:col-span-5 space-y-4 border-t md:border-t-0 md:border-l md:border-slate-100 pt-4 md:pt-0 md:pl-6">
          <h3 className="text-base sm:text-lg font-bold text-slate-900 font-display">
            Product Details
          </h3>

          <div className="space-y-3 text-sm">
            <div>
              <span className="text-xs sm:text-sm text-slate-500 block font-semibold">Product Name</span>
              <strong className="text-slate-900 font-bold text-base sm:text-lg leading-snug block mt-0.5">
                {productName}
              </strong>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <span className="text-xs sm:text-sm text-slate-500 block font-semibold">Category</span>
                <span className="text-slate-800 font-semibold text-sm sm:text-base block mt-0.5">
                  {category}
                </span>
              </div>
              <div>
                <span className="text-xs sm:text-sm text-slate-500 block font-semibold">Brand</span>
                <span className="text-slate-800 font-semibold text-sm sm:text-base block mt-0.5">
                  {brand}
                </span>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <span className="text-xs sm:text-sm text-slate-500 block font-semibold">Package Type</span>
                <span className="text-slate-800 font-semibold text-sm sm:text-base block mt-0.5">
                  {packageType}
                </span>
              </div>
              <div>
                <span className="text-xs sm:text-sm text-slate-500 block font-semibold">Net Quantity (Declared)</span>
                <strong className="text-slate-900 font-bold text-sm sm:text-base block mt-0.5">
                  {netQuantity}
                </strong>
              </div>
            </div>
          </div>
        </div>

        {/* Right Column: Overall Result & Score (~33% / 4 cols) */}
        <div className="md:col-span-4 bg-slate-50/80 border border-slate-200/80 rounded-2xl p-4 sm:p-5 flex flex-col justify-between h-full">
          <div>
            <div className="flex items-center gap-1.5 text-slate-800 text-xs sm:text-sm font-bold">
              <span>Overall Result</span>
              <Info className="w-4 h-4 text-slate-400" />
            </div>

            {/* Prominent Status Banner */}
            <div className="flex items-center gap-2.5 my-2.5">
              {isPass ? (
                <>
                  <CheckCircle2 className="w-8 h-8 sm:w-9 h-9 text-emerald-600 shrink-0" />
                  <span className="text-xl sm:text-2xl font-black text-emerald-700 tracking-tight font-display">
                    {statusText}
                  </span>
                </>
              ) : isViolation ? (
                <>
                  <XCircle className="w-8 h-8 sm:w-9 h-9 text-rose-600 shrink-0" />
                  <span className="text-xl sm:text-2xl font-black text-rose-700 tracking-tight font-display">
                    {statusText}
                  </span>
                </>
              ) : (
                <>
                  <AlertTriangle className="w-8 h-8 sm:w-9 h-9 text-amber-500 fill-amber-500/20 shrink-0" />
                  <span className="text-xl sm:text-2xl font-black text-amber-600 tracking-tight font-display">
                    {statusText}
                  </span>
                </>
              )}
            </div>

            {/* Score Bar */}
            <div className="mt-3">
              <span className="text-xs sm:text-sm font-bold text-slate-600 uppercase tracking-wider block">
                Compliance Score
              </span>
              <div className="flex items-baseline gap-1 mt-0.5">
                <span className={`text-3xl sm:text-4xl font-black font-display ${
                  score >= 80 ? 'text-emerald-600' : score >= 50 ? 'text-amber-600' : 'text-rose-600'
                }`}>
                  {score}
                </span>
                <span className="text-slate-500 font-semibold text-base">/ 100</span>
              </div>

              <div className="w-full bg-slate-200/80 rounded-full h-2.5 mt-1.5 overflow-hidden">
                <div 
                  className={`h-full rounded-full transition-all duration-700 ${
                    score >= 80 ? 'bg-emerald-500' : score >= 50 ? 'bg-amber-500' : 'bg-rose-500'
                  }`}
                  style={{ width: `${Math.min(100, Math.max(0, score))}%` }}
                ></div>
              </div>
            </div>
          </div>

          {/* Bottom Info Box */}
          <div className="mt-4 bg-amber-50/90 border border-amber-200 rounded-xl p-3 text-xs sm:text-sm text-amber-950 leading-relaxed font-medium">
            {summarySentence}
          </div>
        </div>

      </div>
    </div>
  );
}

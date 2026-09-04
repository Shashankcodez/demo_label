import React, { useState, useRef, useEffect } from 'react';
import { 
  UploadCloud, 
  Camera, 
  Image as ImageIcon, 
  Sparkles, 
  AlertCircle, 
  CheckCircle2, 
  ArrowRight, 
  Scan,
  X,
  Cpu
} from 'lucide-react';

const scanDebug = (...args) => {

  const msg = ['[SCAN_DEBUG]', ...args];
  console.log(...msg);
  if (typeof window !== 'undefined') {
    window.__SCAN_DEBUG_LOGS = window.__SCAN_DEBUG_LOGS || [];
    window.__SCAN_DEBUG_LOGS.push({
      time: new Date().toISOString(),
      message: args.map(a => (typeof a === 'object' ? JSON.stringify(a) : String(a))).join(' ')
    });
  }
};

function detectImageMimeType(buffer, fallbackMime = '', filename = '') {
  if (!buffer || buffer.byteLength < 3) {
    if (fallbackMime && fallbackMime.startsWith('image/')) return fallbackMime;
    return 'image/jpeg';
  }
  const bytes = new Uint8Array(buffer);
  if (bytes.length >= 3 && bytes[0] === 0xFF && bytes[1] === 0xD8 && bytes[2] === 0xFF) {
    return 'image/jpeg';
  }
  if (bytes.length >= 8 && bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4E && bytes[3] === 0x47) {
    return 'image/png';
  }
  if (bytes.length >= 12 && bytes[0] === 0x52 && bytes[1] === 0x49 && bytes[2] === 0x46 && bytes[3] === 0x46 &&
      bytes[8] === 0x57 && bytes[9] === 0x45 && bytes[10] === 0x42 && bytes[11] === 0x50) {
    return 'image/webp';
  }
  if (fallbackMime && fallbackMime.startsWith('image/')) return fallbackMime;
  if (/\.png$/i.test(filename)) return 'image/png';
  if (/\.webp$/i.test(filename)) return 'image/webp';
  return 'image/jpeg';
}

export default function ScannerPage({ 
  sampleProducts, 
  onAnalyze, 
  isAnalyzing, 
  analysisStep, 
  analysisProgress,
  analysisError,
  onClearError,
  scannerKey
}) {
  const [selectedImage, setSelectedImage] = useState(null);
  const [selectedFile, setSelectedFile] = useState(null);
  const [selectedImageName, setSelectedImageName] = useState('');
  const [activeDemoPreset, setActiveDemoPreset] = useState(null);
  const [dragActive, setDragActive] = useState(false);

  const fileInputRef = useRef(null);
  const cameraInputRef = useRef(null);
  const previewUrlRef = useRef(null);

  useEffect(() => {
    scanDebug('ScannerPage MOUNTED', { scannerKey });
    return () => {
      scanDebug('ScannerPage UNMOUNTED', { scannerKey });
    };
  }, [scannerKey]);

  // Safe file processing with in-memory buffering (guards against Android Chrome cache invalidation)
  const processFile = async (file) => {
    if (!file) {
      scanDebug('processFile called with empty file');
      return;
    }

    scanDebug('processFile started', {
      name: file.name,
      size: file.size,
      type: file.type
    });

    // On Android, some camera captures return empty file.type or generic octet-stream.
    // Validate by MIME type OR by file extension.
    const isImage = (file.type && file.type.startsWith('image/')) ||
                    /\.(jpe?g|png|webp|gif|bmp|heic|heif)$/i.test(file.name || '');

    if (!isImage) {
      scanDebug('processFile rejected non-image', { name: file.name, type: file.type });
      alert('Please select a valid image file (JPEG, PNG, or WebP).');
      return;
    }

    if (onClearError) onClearError();

    // Read bytes immediately into memory to guard against Android Chrome
    // temporary camera cache invalidation or DOM input cleanup.
    let safeFile = file;
    try {
      const arrayBuffer = await file.arrayBuffer();
      const mime = detectImageMimeType(arrayBuffer, file.type, file.name);
      const safeBlob = new Blob([arrayBuffer], { type: mime });
      safeFile = new File([safeBlob], file.name || 'camera_label.jpg', { type: mime });
      scanDebug('File safely buffered into memory', { size: safeFile.size, type: safeFile.type });
    } catch (err) {
      scanDebug('Could not buffer file in memory, using original File reference:', err?.message);
    }

    // Revoke previous blob URL if any
    if (previewUrlRef.current && previewUrlRef.current.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrlRef.current);
      previewUrlRef.current = null;
    }

    const objectUrl = URL.createObjectURL(safeFile);
    previewUrlRef.current = objectUrl;

    scanDebug('Preview URL created', { url: objectUrl });

    setSelectedFile(safeFile);
    setSelectedImage(objectUrl);
    setSelectedImageName(safeFile.name || 'Camera Photograph');
    setActiveDemoPreset(null);
  };

  // Handle file select from either camera or gallery input
  const handleFileChange = (e) => {
    scanDebug('file input onChange event fired', { filesCount: e.target.files?.length });
    const file = e.target.files?.[0];
    if (file) {
      scanDebug('File extracted from event', { name: file.name, size: file.size, type: file.type });
      processFile(file);
    }
  };

  // Drag & drop handlers
  const handleDrag = (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === "dragenter" || e.type === "dragover") {
      setDragActive(true);
    } else if (e.type === "dragleave") {
      setDragActive(false);
    }
  };

  const handleDrop = (e) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      processFile(e.dataTransfer.files[0]);
    }
  };

  // Select demo preset
  const handleSelectDemo = (preset) => {
    scanDebug('handleSelectDemo chosen', { id: preset.id, name: preset.name });
    if (previewUrlRef.current && previewUrlRef.current.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrlRef.current);
      previewUrlRef.current = null;
    }
    setSelectedFile(null);
    if (onClearError) onClearError();
    setActiveDemoPreset(preset);
    setSelectedImageName(preset.name);
    setSelectedImage('PRESET:' + preset.id);
    if (fileInputRef.current) fileInputRef.current.value = '';
    if (cameraInputRef.current) cameraInputRef.current.value = '';
  };

  // Clear current image
  const handleClear = () => {
    scanDebug('handleClear called');
    if (previewUrlRef.current && previewUrlRef.current.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrlRef.current);
      previewUrlRef.current = null;
    }
    setSelectedImage(null);
    setSelectedFile(null);
    setSelectedImageName('');
    setActiveDemoPreset(null);
    if (onClearError) onClearError();
    if (fileInputRef.current) fileInputRef.current.value = '';
    if (cameraInputRef.current) cameraInputRef.current.value = '';
  };

  // Trigger analysis
  // Language is always 'eng' — OCR is disabled in production; Gemini Vision handles extraction.
  const handleStartAnalysis = () => {
    scanDebug('Analyze button clicked', {
      isAnalyzing,
      hasSelectedImage: !!selectedImage,
      hasActiveDemoPreset: !!activeDemoPreset,
      hasSelectedFile: !!selectedFile,
      selectedFileName: selectedFile?.name,
      selectedFileSize: selectedFile?.size
    });

    if (isAnalyzing) {
      scanDebug('Analyze button clicked while isAnalyzing is true; ignoring');
      return;
    }
    if (onClearError) onClearError();

    if (!selectedImage && !activeDemoPreset) {
      scanDebug('No image or demo selected. Defaulting to sample product 0');
      onAnalyze(sampleProducts[0]);
      return;
    }

    if (activeDemoPreset) {
      scanDebug('Analyzing demo preset', { id: activeDemoPreset.id, name: activeDemoPreset.name });
      onAnalyze(activeDemoPreset);
    } else if (selectedFile) {
      scanDebug('Analyzing real image file', { name: selectedFile.name, size: selectedFile.size });
      // Pass 'eng' as language — backend API expects the param but OCR is disabled (APP_OCR_ENABLED=false).
      // Gemini Vision performs the actual label extraction regardless of this value.
      onAnalyze(null, selectedImage, selectedImageName, selectedFile, 'eng');
    } else {

      scanDebug('ERROR: selectedImage present but selectedFile is null!');
      alert('Image file data is missing. Please re-select or take the photo again.');
    }
  };

  return (
    <div className="max-w-4xl mx-auto px-4 sm:px-6 py-6 sm:py-10 pb-24 md:pb-12">
      
      {/* Page Header */}
      <div className="text-center max-w-xl mx-auto mb-8">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-100 text-gov-blue text-xs font-bold mb-3">
          <Scan className="w-3.5 h-3.5" />
          <span>Legal Metrology Scanner</span>
        </div>
        <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
          Product Label Scanner
        </h1>
        <p className="mt-2 text-xs sm:text-sm text-slate-600">
          Capture or upload the declaration panel of any packaged commodity. Our engine checks Rule 6 declarations in seconds.
        </p>
      </div>

      {/* Analysis Error Alert / Retake Guidance */}
      {analysisError && (
        <div className="mb-6 p-4 sm:p-5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-900 text-xs sm:text-sm animate-fadeIn">
          <div className="flex items-start justify-between gap-3">
            <div className="flex items-start gap-2.5">
              <AlertCircle className="w-5 h-5 text-rose-600 shrink-0 mt-0.5" />
              <div>
                <strong className="font-bold block text-rose-950 text-sm mb-0.5">Scan Analysis Stopped • Retake Recommended</strong>
                <p className="text-rose-800 leading-relaxed">{analysisError}</p>
              </div>
            </div>
            {onClearError && (
              <button
                type="button"
                onClick={onClearError}
                className="p-1 rounded-lg text-rose-500 hover:text-rose-700 hover:bg-rose-100 transition-colors shrink-0"
                title="Dismiss error"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          {/* Quick Retake Photography Checklist */}
          <div className="mt-3.5 pt-3 border-t border-rose-200/80 grid grid-cols-1 sm:grid-cols-2 gap-2.5 text-xs sm:text-sm text-rose-950 font-medium">
            <div className="flex items-center gap-2">
              <span className="text-base">📸</span>
              <span>Hold camera steady &amp; tap text to focus</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-base">💡</span>
              <span>Angle package slightly away from light to prevent glare</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-base">📐</span>
              <span>Smooth out wrapper folds flat before snapping</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-base">🔍</span>
              <span>Capture the entire Principal Display Panel</span>
            </div>
          </div>
        </div>
      )}

      {/* Main Upload / Preview Container */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-card p-4 sm:p-7 relative overflow-hidden">
        
        {/* Hidden inputs */}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="hidden"
          onClick={(e) => {
            scanDebug('file input onClick: clearing previous value');
            e.target.value = '';
          }}
          onChange={handleFileChange}
        />
        {/* Camera input with capture="environment" for smartphone rear camera */}
        <input
          ref={cameraInputRef}
          type="file"
          accept="image/*"
          capture="environment"
          className="hidden"
          onClick={(e) => {
            scanDebug('camera input onClick: clearing previous value');
            e.target.value = '';
          }}
          onChange={handleFileChange}
        />

        {/* AI Vision Analysis Info Banner */}
        <div className="mb-4 bg-slate-50 border border-slate-200 rounded-xl p-3 sm:p-3.5 flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-lg bg-blue-100/70 text-gov-blue flex items-center justify-center shrink-0 border border-blue-200">
            <Cpu className="w-4 h-4" />
          </div>
          <div>
            <p className="text-sm font-bold text-slate-800">AI Vision Analysis</p>
            <p className="text-xs text-slate-500 font-medium">
              Gemini Vision analyzes the uploaded label image and extracts the required declarations for compliance checking.
            </p>
          </div>
        </div>

        {!selectedImage ? (

          /* Empty State / Upload Area */
          <div
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-xl p-8 sm:p-12 text-center transition-all cursor-pointer ${
              dragActive 
                ? 'border-gov-blue bg-blue-50/60 scale-[0.99]' 
                : 'border-slate-300 hover:border-slate-400 bg-slate-50/60 hover:bg-slate-50'
            }`}
            onClick={() => fileInputRef.current?.click()}
          >
            <div className="w-16 h-16 rounded-2xl bg-blue-50 text-gov-blue flex items-center justify-center mx-auto mb-4 shadow-sm border border-blue-100">
              <UploadCloud className="w-8 h-8" />
            </div>

            <h3 className="text-lg sm:text-xl font-bold text-slate-800">
              Upload Label Photograph
            </h3>
            <p className="text-sm sm:text-base text-slate-600 mt-1 max-w-md mx-auto">
              Drag and drop your packaging image here, or browse from your device
            </p>

            {/* Mobile Camera and File Browse Buttons */}
            <div className="mt-6 flex flex-wrap items-center justify-center gap-3" onClick={(e) => e.stopPropagation()}>
              <button
                type="button"
                id="camera-capture-btn"
                onClick={() => cameraInputRef.current?.click()}
                className="px-5 py-3 rounded-xl text-sm font-bold bg-gov-blue text-white hover:bg-blue-700 shadow-sm flex items-center gap-2 active:scale-95 transition-all cursor-pointer"
              >
                <Camera className="w-4 h-4" />
                Use Phone Camera
              </button>

              <button
                type="button"
                id="browse-file-btn"
                onClick={() => fileInputRef.current?.click()}
                className="px-5 py-3 rounded-xl text-sm font-semibold bg-white border border-slate-300 text-slate-700 hover:bg-slate-100 shadow-sm flex items-center gap-2 active:scale-95 transition-all cursor-pointer"
              >
                <ImageIcon className="w-4 h-4 text-slate-500" />
                Browse Gallery
              </button>
            </div>

            <p className="text-xs text-slate-500 mt-5 font-medium">
              Supports JPEG, PNG, WEBP • Ensure label text is sharp &amp; glare-free for best results
            </p>
          </div>
        ) : (
          /* Preview State with Simulated Scanner Reticle */
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-slate-100">
              <div className="flex items-center gap-2">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse"></span>
                <span className="text-sm sm:text-base font-bold text-slate-800 truncate max-w-[200px] sm:max-w-md">
                  {selectedImageName}
                </span>
                {activeDemoPreset && (
                  <span className="px-2.5 py-0.5 rounded text-xs font-bold bg-blue-100 text-gov-blue">
                    Demo Preset
                  </span>
                )}
              </div>

              <button
                onClick={handleClear}
                className="text-xs font-semibold text-slate-500 hover:text-red-600 flex items-center gap-1 p-1 hover:bg-slate-100 rounded-lg transition-colors"
                title="Remove image"
              >
                <X className="w-4 h-4" />
                <span className="hidden sm:inline">Change</span>
              </button>
            </div>

            {/* Visual Preview Box */}
            <div className="relative rounded-xl overflow-hidden bg-slate-900 aspect-video sm:aspect-[21/9] flex items-center justify-center border border-slate-800 shadow-inner">
              
              {/* Scanline Animation */}
              <div className="absolute inset-0 pointer-events-none z-10">
                <div className="w-full h-1 bg-gradient-to-r from-transparent via-cyan-400 to-transparent shadow-[0_0_15px_#22d3ee] animate-scanline absolute"></div>
                {/* HUD Corner Reticles */}
                <div className="absolute top-3 left-3 w-4 h-4 border-t-2 border-l-2 border-cyan-400"></div>
                <div className="absolute top-3 right-3 w-4 h-4 border-t-2 border-r-2 border-cyan-400"></div>
                <div className="absolute bottom-3 left-3 w-4 h-4 border-b-2 border-l-2 border-cyan-400"></div>
                <div className="absolute bottom-3 right-3 w-4 h-4 border-b-2 border-r-2 border-cyan-400"></div>
                
                {/* Live Detection Indicators */}
                <div className="absolute bottom-3 left-4 text-[10px] font-mono text-cyan-300/80 tracking-wider">
                  AI_VISION: READY • REGION_LOCKED
                </div>
              </div>

              {/* Render Image or Demo Preset Graphic */}
              {activeDemoPreset ? (
                <div className="p-6 text-center text-white max-w-md">
                  <span className="text-4xl sm:text-5xl block mb-2">{activeDemoPreset.imageEmoji}</span>
                  <h4 className="text-lg sm:text-xl font-bold font-display">{activeDemoPreset.name}</h4>
                  <p className="text-xs sm:text-sm text-slate-300 mt-1 font-mono line-clamp-2">
                    {activeDemoPreset.labelSnippet}
                  </p>
                  <div className="mt-3 flex flex-wrap items-center justify-center gap-2">
                    {activeDemoPreset.detectedFieldsCount !== undefined && (
                      <span className={`text-xs font-black px-3 py-1 rounded-full uppercase tracking-wider ${
                        activeDemoPreset.qualityTier === 'GOOD_LABEL' ? 'bg-emerald-500 text-white' :
                        activeDemoPreset.qualityTier === 'AVERAGE_LABEL' ? 'bg-amber-500 text-white' :
                        activeDemoPreset.qualityTier === 'POOR_LABEL' ? 'bg-orange-500 text-white' :
                        'bg-rose-500 text-white'
                      }`}>
                        {activeDemoPreset.qualityTier === 'GOOD_LABEL' ? 'Good Label (12 Fields)' :
                         activeDemoPreset.qualityTier === 'AVERAGE_LABEL' ? 'Average Label (7 Fields)' :
                         activeDemoPreset.qualityTier === 'POOR_LABEL' ? 'Poor Label (3 Fields)' :
                         'Very Poor Image (0 Fields • Retake)'}
                      </span>
                    )}
                    <span className="text-xs font-bold px-2.5 py-1 rounded bg-white/20">
                      Outcome: {activeDemoPreset.complianceOutcome || activeDemoPreset.status}
                    </span>
                  </div>
                </div>
              ) : (
                <img
                  src={selectedImage}
                  alt="Product Label Preview"
                  className="w-full h-full object-contain"
                />
              )}
            </div>

            {/* Action Bar */}
            <div className="pt-2 flex flex-col sm:flex-row items-center justify-between gap-3">
              <div className="text-xs sm:text-sm text-slate-600 text-center sm:text-left">
                Ready for extraction. Click <strong>Analyze Label</strong> to run Legal Metrology checks.
              </div>

              <div className="flex items-center gap-2 w-full sm:w-auto">
                <button
                  type="button"
                  onClick={handleClear}
                  className="flex-1 sm:flex-none px-4 py-2.5 rounded-xl border border-slate-300 text-xs sm:text-sm font-semibold text-slate-700 hover:bg-slate-50 cursor-pointer"
                >
                  Clear
                </button>
                <button
                  type="button"
                  id="analyze-label-btn"
                  disabled={isAnalyzing}
                  onClick={handleStartAnalysis}
                  className={`flex-1 sm:flex-none px-6 py-2.5 rounded-xl bg-gov-blue hover:bg-blue-700 text-white font-bold text-sm flex items-center justify-center gap-2 shadow-md shadow-blue-900/20 active:scale-95 transition-all cursor-pointer ${
                    isAnalyzing ? 'opacity-60 cursor-not-allowed' : ''
                  }`}
                >
                  <Scan className="w-4 h-4" />
                  <span>{isAnalyzing ? 'Analyzing...' : 'Analyze Label'}</span>
                </button>
              </div>
            </div>
          </div>
        )}

      </div>

      {/* "Try Demo Products by Quality Tier" Section */}
      <div className="mt-8">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-4">
          <div className="flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-amber-500" />
            <h3 className="text-sm sm:text-base font-bold text-slate-800 uppercase tracking-wider">
              Test Quality Tiers &amp; Field Yields (Instant Load)
            </h3>
          </div>
          <span className="text-xs sm:text-sm text-slate-600 font-medium">
            Good (12 fields) • Average (7 fields) • Poor (3 fields) • Very Poor (0 fields / Retake)
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
          {sampleProducts.slice(0, 6).map((preset) => {
            const isSelected = activeDemoPreset?.id === preset.id;
            const isGood = preset.qualityTier === 'GOOD_LABEL' || preset.detectedFieldsCount >= 10;
            const isAverage = preset.qualityTier === 'AVERAGE_LABEL' || (preset.detectedFieldsCount >= 6 && preset.detectedFieldsCount < 10);
            const isPoor = preset.qualityTier === 'POOR_LABEL' || (preset.detectedFieldsCount >= 1 && preset.detectedFieldsCount < 6);

            return (
              <div
                key={preset.id}
                onClick={() => handleSelectDemo(preset)}
                className={`p-4 rounded-xl border cursor-pointer transition-all flex flex-col justify-between ${
                  isSelected
                    ? 'border-gov-blue bg-blue-50/70 ring-2 ring-blue-500/20 shadow-sm'
                    : 'border-slate-200 bg-white hover:border-slate-300 hover:bg-slate-50/80 shadow-subtle'
                }`}
              >
                <div>
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-2xl">{preset.imageEmoji}</span>
                    <span className={`px-2.5 py-0.5 rounded text-xs font-black uppercase tracking-wider border ${
                      isGood ? 'bg-emerald-50 text-emerald-800 border-emerald-200' :
                      isAverage ? 'bg-amber-50 text-amber-800 border-amber-200' :
                      isPoor ? 'bg-orange-50 text-orange-800 border-orange-200' :
                      'bg-rose-50 text-rose-800 border-rose-200'
                    }`}>
                      {isGood ? '12 Fields' :
                       isAverage ? '7 Fields' :
                       isPoor ? '3 Fields' :
                       '0 Fields (Retake)'}
                    </span>
                  </div>

                  <h4 className="text-sm sm:text-base font-bold text-slate-900 line-clamp-1">
                    {preset.name}
                  </h4>
                  <p className="text-xs text-slate-500 mt-0.5 line-clamp-1">
                    {preset.category}
                  </p>

                  <div className="mt-2 text-xs font-semibold text-slate-600">
                    Outcome: <span className="font-bold text-slate-900">{preset.complianceOutcome || preset.status}</span>
                  </div>
                </div>

                <div className="mt-3.5 pt-2.5 border-t border-slate-100 flex items-center justify-between text-xs sm:text-sm font-bold text-gov-blue">
                  <span>{isSelected ? 'Loaded in Preview' : 'Test Tier'}</span>
                  <ArrowRight className="w-4 h-4" />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Quick Tips for Inspection */}
      <div className="mt-8 bg-blue-50/60 rounded-xl p-4 border border-blue-100 text-xs text-slate-600 flex items-start gap-3">
        <AlertCircle className="w-4 h-4 text-gov-blue shrink-0 mt-0.5" />
        <div>
          <span className="font-bold text-gov-blue">Evaluation Tip for Judges: </span>
          You can test the <strong>Haldiram's</strong> sample to see a 100% compliant pass, or test the <strong>PureDrop Water</strong> sample to see how the system detects missing grievance numbers, absent tax clauses, and non-compliant font sizes.
        </div>
      </div>

      {/* Realistic Scanning / Processing Modal State */}
      {isAnalyzing && (
        <div className="fixed inset-0 z-50 bg-slate-950/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 sm:p-7 shadow-2xl border border-slate-200 text-center animate-fadeIn">
            
            {/* Animated Radar Icon */}
            <div className="relative w-20 h-20 mx-auto mb-5">
              <div className="absolute inset-0 rounded-full bg-blue-100 animate-ping opacity-60"></div>
              <div className="relative w-20 h-20 rounded-full bg-gradient-to-tr from-gov-navy to-gov-blue text-white flex items-center justify-center shadow-lg">
                <Scan className="w-10 h-10 animate-pulse text-amber-400" />
              </div>
            </div>

            <h3 className="text-xl font-bold text-slate-900 mb-1">
              Analyzing Commodity Label
            </h3>
            <p className="text-xs sm:text-sm text-slate-500 mb-6 font-medium">
              Checking Legal Metrology (Packaged Commodities) Rules 2011
            </p>

            {/* Stepped Progress Checklist */}
            <div className="space-y-3 text-left mb-6 text-xs sm:text-sm bg-slate-50 p-4 rounded-xl border border-slate-200/80">
              <div className={`flex items-center gap-2.5 transition-colors ${
                analysisProgress >= 25 ? 'text-emerald-700 font-semibold' : 'text-slate-500'
              }`}>
                {analysisProgress >= 25 ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                ) : (
                  <div className="w-4 h-4 rounded-full border-2 border-slate-300 shrink-0 animate-spin border-t-transparent"></div>
                )}
                <span>1. Uploading label image to backend</span>
              </div>

              <div className={`flex items-center gap-2.5 transition-colors ${
                analysisProgress >= 50 ? 'text-emerald-700 font-semibold' : 
                analysisProgress >= 25 ? 'text-blue-700 font-semibold' : 'text-slate-500'
              }`}>
                {analysisProgress >= 50 ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                ) : analysisProgress >= 25 ? (
                  <div className="w-4 h-4 rounded-full border-2 border-blue-600 shrink-0 animate-spin border-t-transparent"></div>
                ) : (
                  <div className="w-4 h-4 rounded-full border-2 border-slate-200 shrink-0"></div>
                )}
                <span>2. AI Vision Analysis — extracting declarations</span>
              </div>

              <div className={`flex items-center gap-2.5 transition-colors ${
                analysisProgress >= 75 ? 'text-emerald-700 font-semibold' : 
                analysisProgress >= 50 ? 'text-blue-700 font-semibold' : 'text-slate-500'
              }`}>
                {analysisProgress >= 75 ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                ) : analysisProgress >= 50 ? (
                  <div className="w-4 h-4 rounded-full border-2 border-blue-600 shrink-0 animate-spin border-t-transparent"></div>
                ) : (
                  <div className="w-4 h-4 rounded-full border-2 border-slate-200 shrink-0"></div>
                )}
                <span>3. Parsing statutory entities &amp; rule evaluation</span>
              </div>

              <div className={`flex items-center gap-2.5 transition-colors ${
                analysisProgress >= 100 ? 'text-emerald-700 font-semibold' : 
                analysisProgress >= 75 ? 'text-blue-700 font-semibold' : 'text-slate-500'
              }`}>
                {analysisProgress >= 100 ? (
                  <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
                ) : analysisProgress >= 75 ? (
                  <div className="w-4 h-4 rounded-full border-2 border-blue-600 shrink-0 animate-spin border-t-transparent"></div>
                ) : (
                  <div className="w-4 h-4 rounded-full border-2 border-slate-200 shrink-0"></div>
                )}
                <span>4. Saving analysis &amp; generating report</span>
              </div>
            </div>

            {/* Progress Bar */}
            <div className="w-full bg-slate-100 rounded-full h-2.5 overflow-hidden mb-2">
              <div 
                className="bg-gov-blue h-2.5 rounded-full transition-all duration-300"
                style={{ width: `${analysisProgress}%` }}
              ></div>
            </div>
            <div className="flex justify-between text-xs text-slate-500 font-mono font-medium">
              <span>Status: {analysisStep}</span>
              <span>{analysisProgress}%</span>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}

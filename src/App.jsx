import React, { useState, useEffect, useRef } from 'react';
import Navbar from './components/Navbar';
import LandingPage from './components/LandingPage';
import ScannerPage from './components/ScannerPage';
import ResultPage from './components/ResultPage';
import HistoryPage from './components/HistoryPage';
import ReportModal from './components/ReportModal';
import MobileQrModal from './components/MobileQrModal';
import { SAMPLE_PRODUCTS, DEFAULT_HISTORY } from './data/mockProducts';
import { scanLabel, getScanById } from './api/labelCheckApi';
import { mapBackendScanToProduct, dataUrlToFile } from './api/scanMapper';

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

export default function App() {
  const [activePage, setActivePage] = useState('home'); // 'home' | 'scanner' | 'result' | 'history'
  const [activeProduct, setActiveProduct] = useState(SAMPLE_PRODUCTS[0]); // Haldiram's by default
  const [isReportModalOpen, setIsReportModalOpen] = useState(false);
  const [isQrModalOpen, setIsQrModalOpen] = useState(false);

  // Scan analysis state
  const [isAnalyzing, setIsAnalyzing] = useState(false);
  const [analysisStep, setAnalysisStep] = useState('Initializing...');
  const [analysisProgress, setAnalysisProgress] = useState(0);
  const [analysisError, setAnalysisError] = useState(null);
  const [scannerKey, setScannerKey] = useState(0);

  const timersRef = useRef([]);

  const clearActiveTimers = () => {
    timersRef.current.forEach(t => clearTimeout(t));
    timersRef.current = [];
  };

  // Centralized scanner navigation that guarantees full reset of scanner state and DOM inputs
  const handleNavigateToScanner = () => {
    scanDebug('handleNavigateToScanner called. Incrementing scannerKey from', scannerKey, 'to', scannerKey + 1);
    clearActiveTimers();
    setIsAnalyzing(false);
    setAnalysisProgress(0);
    setAnalysisStep('Initializing...');
    setAnalysisError(null);
    setScannerKey(prev => prev + 1);
    setActivePage('scanner');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleNavPage = (page) => {
    scanDebug('handleNavPage requested page:', page, 'current activePage:', activePage);
    if (page === 'scanner') {
      // If already on scanner page, do NOT remount and destroy user's selected file!
      if (activePage !== 'scanner') {
        handleNavigateToScanner();
      }
    } else {
      setActivePage(page);
    }
  };

  // History state persisted in localStorage
  const [historyList, setHistoryList] = useState(() => {
    try {
      const saved = localStorage.getItem('labelcheck_history_v1');
      return saved ? JSON.parse(saved) : DEFAULT_HISTORY;
    } catch {
      return DEFAULT_HISTORY;
    }
  });

  useEffect(() => {
    try {
      localStorage.setItem('labelcheck_history_v1', JSON.stringify(historyList));
    } catch (e) {
      console.warn('Failed to persist demo history to localStorage (quota exceeded). Resetting demo cache:', e);
      try {
        localStorage.removeItem('labelcheck_history_v1');
      } catch {
        // ignore
      }
    }
  }, [historyList]);

  // Clean up timers on unmount
  useEffect(() => {
    return () => clearActiveTimers();
  }, []);

  const addProductToHistory = (resultProd) => {
    // Real scans are permanently persisted in the backend H2 database (GET /api/v1/scans).
    // Do NOT store real scans or large camera image strings in localStorage to prevent mobile QuotaExceededError.
    if (resultProd.isRealScan) {
      return;
    }

    const now = new Date();
    const dateString = now.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }) + ' • ' + now.toLocaleTimeString('en-IN', {
      hour: '2-digit',
      minute: '2-digit'
    });

    const issuesCount = Array.isArray(resultProd.checks)
      ? resultProd.checks.filter(c => c.status !== 'PASS').length
      : 0;

    const newHistoryItem = {
      id: 'hist-' + Date.now(),
      productId: resultProd.id,
      scanId: resultProd.scanId || null,
      name: resultProd.name,
      brand: resultProd.brand,
      category: resultProd.category,
      scannedAt: dateString,
      score: resultProd.overallScore,
      status: resultProd.status,
      issuesCount: issuesCount,
      thumbnailEmoji: resultProd.imageEmoji || '📦',
      productData: resultProd
    };

    setHistoryList(prev => [newHistoryItem, ...prev]);
  };

  /**
   * Primary handler for analyzing a product:
   * 1. Preset demo products use existing mock data (Preserves demo mode!).
   * 2. Real uploaded/captured images send a REAL multipart POST request to the backend.
   */
  const handleAnalyze = async (presetProduct, customImage = null, customName = '', customFile = null, language = 'eng') => {
    scanDebug('handleAnalyze invoked', {
      isAnalyzing,
      isDemo: !!presetProduct,
      hasCustomFile: !!customFile,
      hasCustomImage: !!customImage,
      fileName: customFile?.name,
      fileSize: customFile?.size,
      language
    });

    if (isAnalyzing) {
      scanDebug('handleAnalyze aborted: isAnalyzing is already true');
      return;
    }
    clearActiveTimers();
    setAnalysisError(null);

    // ==========================================
    // CASE A: DEMO PRESET (Preserve Demo Mode)
    // ==========================================
    if (presetProduct) {
      scanDebug('Starting Demo Preset analysis for:', presetProduct.name);
      setIsAnalyzing(true);
      setAnalysisProgress(20);
      setAnalysisStep('Loading demo product profile...');

      const t1 = setTimeout(() => {
        setAnalysisProgress(60);
        setAnalysisStep('Evaluating statutory declarations against Rule 6...');
      }, 350);

      const t2 = setTimeout(() => {
        setAnalysisProgress(100);
        setAnalysisStep('Inspection Complete!');
        setActiveProduct(presetProduct);
        setIsAnalyzing(false);
        setActivePage('result');
        window.scrollTo({ top: 0, behavior: 'smooth' });
        addProductToHistory(presetProduct);
        scanDebug('Demo Preset analysis finished, navigated to result');
      }, 750);

      timersRef.current = [t1, t2];
      return;
    }

    // ==========================================
    // CASE B: REAL IMAGE UPLOAD (Real Backend)
    // ==========================================
    let fileToUpload = customFile;
    if (!fileToUpload && customImage && typeof customImage === 'string' && customImage.startsWith('data:')) {
      fileToUpload = dataUrlToFile(customImage, customName || 'uploaded_label.jpg');
    }

    if (!fileToUpload) {
      scanDebug('ERROR: No valid fileToUpload found for real scan!');
      setAnalysisError('No valid image file found for scanning. Please select an image.');
      return;
    }

    scanDebug('Initiating REAL scan backend upload', {
      fileName: fileToUpload.name,
      fileSize: fileToUpload.size,
      fileType: fileToUpload.type,
      language
    });

    setIsAnalyzing(true);
    setAnalysisProgress(25);
    setAnalysisStep('Uploading label photograph...');

    const stepTimer1 = setTimeout(() => {
      setAnalysisProgress(60);
      setAnalysisStep('Inspecting packaging declarations with Vision AI...');
    }, 1200);

    const stepTimer2 = setTimeout(() => {
      setAnalysisProgress(85);
      setAnalysisStep('Evaluating deterministic Legal Metrology rules...');
    }, 3000);

    try {
      scanDebug('Calling scanLabel API with language:', language);
      const backendResponse = await scanLabel(fileToUpload, 'uploaded_label.jpg', language);
      clearTimeout(stepTimer1);
      clearTimeout(stepTimer2);

      scanDebug('scanLabel response received successfully', {
        scanId: backendResponse?.scanId,
        status: backendResponse?.status,
        language: backendResponse?.language,
        overallScore: backendResponse?.compliance?.overallScore
      });

      setAnalysisProgress(100);
      setAnalysisStep('Finalizing Inspection Memorandum...');

      scanDebug('Mapping backend response to product model...');
      const mappedProduct = mapBackendScanToProduct(backendResponse, customImage);
      scanDebug('Product mapped successfully', {
        name: mappedProduct.name,
        overallScore: mappedProduct.overallScore,
        checksCount: mappedProduct.checks?.length
      });

      scanDebug('Setting activeProduct and transitioning to result page');
      setActiveProduct(mappedProduct);
      setIsAnalyzing(false);
      setActivePage('result');
      window.scrollTo({ top: 0, behavior: 'smooth' });

      // Save to history (real scans stay in H2)
      addProductToHistory(mappedProduct);
      scanDebug('Real scan fully completed and displayed!');
    } catch (err) {
      scanDebug('REAL scan failed with error:', err?.message || err);
      console.error('Scan analysis failed:', err);
      setIsAnalyzing(false);
      setAnalysisProgress(0);
      setAnalysisStep('Initializing...');
      // Retain user on scanner page and show clear error message
      setAnalysisError(err.message || 'Unable to analyze image. Please ensure the backend is running.');
    }
  };

  // Quick Demo Trigger
  const handleQuickDemo = () => {
    const demo = SAMPLE_PRODUCTS[1]; // PureDrop Water (Violation)
    handleAnalyze(demo);
  };

  // View Demo from Hero
  const handleViewDemo = () => {
    setActiveProduct(SAMPLE_PRODUCTS[0]); // Haldiram's
    setActivePage('result');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Select a product directly
  const handleSelectProduct = (product) => {
    setActiveProduct(product);
    setActivePage('result');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Select an item from History
  const handleSelectHistoryItem = async (historyItem) => {
    if (!historyItem) return;

    // 1. If it is already a fully mapped product (e.g. from HistoryPage getScanById)
    if (historyItem.checks && historyItem.mrp !== undefined) {
      setActiveProduct(historyItem);
      setActivePage('result');
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    // 2. If it is a real backend scan with scanId, retrieve complete scan record
    if (historyItem.scanId) {
      try {
        const fullScan = await getScanById(historyItem.scanId);
        const product = mapBackendScanToProduct(fullScan);
        setActiveProduct(product);
        setActivePage('result');
        window.scrollTo({ top: 0, behavior: 'smooth' });
      } catch (err) {
        console.error('Failed to retrieve full scan record from backend:', err);
      }
      return;
    }

    // 3. If it has productData from demo storage
    if (historyItem.productData) {
      setActiveProduct(historyItem.productData);
      setActivePage('result');
      window.scrollTo({ top: 0, behavior: 'smooth' });
      return;
    }

    // 4. Fallback lookup in sample demo products
    const found = SAMPLE_PRODUCTS.find(p => p.id === historyItem.productId);
    setActiveProduct(found || SAMPLE_PRODUCTS[0]);
    setActivePage('result');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  // Reset & Clear History
  const handleClearHistory = () => {
    setHistoryList([]);
  };

  const handleResetHistory = () => {
    setHistoryList(DEFAULT_HISTORY);
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col selection:bg-blue-100 selection:text-blue-900">
      
      {/* Navigation Bar */}
      <Navbar
        activePage={activePage}
        setActivePage={handleNavPage}
        activeProduct={activeProduct}
        onQuickDemo={handleQuickDemo}
        onOpenMobileQr={() => setIsQrModalOpen(true)}
      />

      {/* Main Page Routing */}
      <main className="flex-1">
        {activePage === 'home' && (
          <LandingPage
            onScanClick={handleNavigateToScanner}
            onViewDemo={handleViewDemo}
            onSelectProduct={handleSelectProduct}
            sampleProducts={SAMPLE_PRODUCTS}
          />
        )}

        {activePage === 'scanner' && (
          <ScannerPage
            key={scannerKey}
            scannerKey={scannerKey}
            sampleProducts={SAMPLE_PRODUCTS}
            onAnalyze={handleAnalyze}
            isAnalyzing={isAnalyzing}
            analysisStep={analysisStep}
            analysisProgress={analysisProgress}
            analysisError={analysisError}
            onClearError={() => setAnalysisError(null)}
          />
        )}

        {activePage === 'result' && (
          <ResultPage
            product={activeProduct}
            onScanAnother={handleNavigateToScanner}
            onOpenReportModal={() => setIsReportModalOpen(true)}
          />
        )}

        {activePage === 'history' && (
          <HistoryPage
            historyList={historyList}
            onSelectHistoryItem={handleSelectHistoryItem}
            onClearHistory={handleClearHistory}
            onResetHistory={handleResetHistory}
            onScanNew={handleNavigateToScanner}
          />
        )}
      </main>

      {/* Official Inspection Report Modal */}
      {isReportModalOpen && (
        <ReportModal
          product={activeProduct}
          onClose={() => setIsReportModalOpen(false)}
        />
      )}

      {/* Mobile QR Modal */}
      {isQrModalOpen && (
        <MobileQrModal
          isOpen={isQrModalOpen}
          onClose={() => setIsQrModalOpen(false)}
        />
      )}

      {/* Minimal Footer */}
      <footer className="border-t border-slate-200 bg-white py-6 text-center text-xs text-slate-500">
        <div className="max-w-5xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-2">
          <span>Smart India Hackathon 2026 • Problem Statement SIH26034</span>
          <span>Department of Consumer Affairs • Legal Metrology Packaged Commodities Compliance</span>
        </div>
      </footer>

    </div>
  );
}

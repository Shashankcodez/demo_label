import React, { useState, useEffect, useCallback } from 'react';
import { 
  History, 
  Search, 
  CheckCircle2, 
  AlertTriangle, 
  XCircle, 
  ArrowRight, 
  RotateCcw, 
  Scan,
  Calendar,
  Database,
  Layers,
  ChevronLeft,
  ChevronRight,
  Loader2
} from 'lucide-react';
import { getScanHistory, getScanById } from '../api/labelCheckApi';
import { mapBackendScanToProduct, formatLanguageDisplay, formatStatusDisplay } from '../api/scanMapper';

/**
 * Formats an ISO createdAt date string into a local Indian English timestamp.
 *
 * @param {string} isoString
 * @returns {string}
 */
function formatCreatedAt(isoString) {
  if (!isoString) return 'Date not available';
  try {
    const d = new Date(isoString);
    if (isNaN(d.getTime())) return String(isoString);
    return d.toLocaleDateString('en-IN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric'
    }) + ' • ' + d.toLocaleTimeString('en-IN', {
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return String(isoString);
  }
}

export default function HistoryPage({ 
  historyList: demoHistoryList, 
  onSelectHistoryItem, 
  onResetHistory, 
  onScanNew 
}) {
  // Source selector: 'backend' (Real H2 Database) | 'demo' (Demo Presentation Presets)
  const [activeSource, setActiveSource] = useState('backend');

  // Backend scan records state
  const [realScans, setRealScans] = useState([]);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState(null);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loadingScanId, setLoadingScanId] = useState(null);

  // Search and status filters
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('ALL'); // ALL, PASS, WARNING, VIOLATION

  /**
   * Fetches persistent scan history from backend Spring Boot / H2 service for pagination or refresh.
   */
  const fetchBackendHistory = useCallback(async (page = 0) => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getScanHistory(page, 20);
      const content = Array.isArray(data?.content) ? data.content : [];
      
      const mappedItems = content.map(item => ({
        id: item.scanId,
        scanId: item.scanId,
        name: item.productName || (item.filename ? `Packaged Commodity (${item.filename})` : 'Scanned Product'),
        brand: item.brand || 'Not detected',
        category: 'Packaged Commodity (SIH26034)',
        status: item.overallStatus || 'WARNING',
        score: typeof item.overallScore === 'number' ? item.overallScore : 0,
        summary: item.summary || 'Statutory packaging compliance analysis',
        scannedAt: formatCreatedAt(item.createdAt),
        language: item.language || 'eng',
        thumbnailEmoji: '📦',
        isRealScan: true
      }));

      setRealScans(mappedItems);
      setCurrentPage(data?.page || page);
      setTotalPages(data?.totalPages || 1);
      setTotalElements(typeof data?.totalElements === 'number' ? data.totalElements : mappedItems.length);
    } catch (err) {
      console.error('Failed to load scan history from backend:', err);
      setError('Unable to load scan history. Make sure the LabelCheck backend is running.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  // Fetch backend records on component mount
  useEffect(() => {
    let ignore = false;
    async function loadInitial() {
      setIsLoading(true);
      setError(null);
      try {
        const data = await getScanHistory(0, 20);
        if (ignore) return;
        const content = Array.isArray(data?.content) ? data.content : [];
        const mappedItems = content.map(item => ({
          id: item.scanId,
          scanId: item.scanId,
          name: item.productName || (item.filename ? `Packaged Commodity (${item.filename})` : 'Scanned Product'),
          brand: item.brand || 'Not detected',
          category: 'Packaged Commodity (SIH26034)',
          status: item.overallStatus || 'WARNING',
          score: typeof item.overallScore === 'number' ? item.overallScore : 0,
          summary: item.summary || 'Statutory packaging compliance analysis',
          scannedAt: formatCreatedAt(item.createdAt),
          language: item.language || 'eng',
          thumbnailEmoji: '📦',
          isRealScan: true
        }));
        setRealScans(mappedItems);
        setCurrentPage(data?.page || 0);
        setTotalPages(data?.totalPages || 1);
        setTotalElements(typeof data?.totalElements === 'number' ? data.totalElements : mappedItems.length);
      } catch (err) {
        if (ignore) return;
        console.error('Failed to load scan history from backend:', err);
        setError('Unable to load scan history. Make sure the LabelCheck backend is running.');
      } finally {
        if (!ignore) setIsLoading(false);
      }
    }
    loadInitial();
    return () => {
      ignore = true;
    };
  }, []);

  // Determine active dataset based on selected source tab
  const activeDataset = activeSource === 'backend' ? realScans : (demoHistoryList || []);

  // Filter list by keyword and statutory status
  const filteredList = activeDataset.filter(item => {
    const q = searchQuery.toLowerCase().trim();
    const matchesSearch = !q || 
      (item.name && item.name.toLowerCase().includes(q)) ||
      (item.brand && item.brand.toLowerCase().includes(q)) ||
      (item.category && item.category.toLowerCase().includes(q)) ||
      (item.summary && item.summary.toLowerCase().includes(q));
      
    const matchesStatus = statusFilter === 'ALL' || item.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  /**
   * Handles user click on a history record card:
   * - If real backend scan: calls GET /api/v1/scans/{scanId}, maps complete result, and navigates.
   * - If demo item: delegates directly to existing demo handler.
   */
  const handleItemClick = async (item) => {
    if (item.scanId && item.isRealScan) {
      setLoadingScanId(item.scanId);
      try {
        const fullBackendScan = await getScanById(item.scanId);
        const fullProduct = mapBackendScanToProduct(fullBackendScan);
        onSelectHistoryItem(fullProduct);
      } catch (err) {
        console.error('Failed to retrieve full scan detail:', err);
        setError(`Failed to retrieve complete scan record: ${err.message}`);
      } finally {
        setLoadingScanId(null);
      }
    } else {
      onSelectHistoryItem(item);
    }
  };

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-6 sm:py-10 pb-24 md:pb-12">
      
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6">
        <div>
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-blue-100 text-gov-blue text-xs font-bold mb-2">
            <History className="w-3.5 h-3.5" />
            <span>Inspection Log</span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
            Scan History & Audit Records
          </h1>
          <p className="mt-1 text-xs sm:text-sm text-slate-500">
            Persistent inspection records stored in the embedded database with Legal Metrology audit trails.
          </p>
        </div>

        {/* Action Controls */}
        <div className="flex items-center gap-2 w-full sm:w-auto">
          {activeSource === 'backend' ? (
            <button
              onClick={() => fetchBackendHistory(currentPage)}
              disabled={isLoading}
              className="px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold text-slate-700 bg-white border border-slate-200 hover:bg-slate-50 flex items-center gap-2 transition-colors shadow-subtle disabled:opacity-60 cursor-pointer"
              title="Refresh database records"
            >
              <RotateCcw className={`w-4 h-4 ${isLoading ? 'animate-spin' : ''}`} />
              <span>Refresh</span>
            </button>
          ) : (
            <button
              onClick={onResetHistory}
              className="px-4 py-2.5 rounded-xl text-xs sm:text-sm font-semibold text-slate-600 bg-white border border-slate-200 hover:bg-slate-50 flex items-center gap-2 transition-colors shadow-subtle cursor-pointer"
              title="Reset demo samples"
            >
              <RotateCcw className="w-4 h-4" />
              Reset Demo
            </button>
          )}

          <button
            onClick={onScanNew}
            className="px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold text-white bg-gov-blue hover:bg-blue-700 flex items-center gap-2 transition-colors shadow-sm cursor-pointer"
          >
            <Scan className="w-4 h-4" />
            Scan New
          </button>
        </div>
      </div>

      {/* Real Database vs Demo Presets Source Switcher */}
      <div className="flex items-center gap-2.5 mb-6">
        <button
          onClick={() => setActiveSource('backend')}
          className={`px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
            activeSource === 'backend'
              ? 'bg-gov-blue text-white shadow-sm'
              : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
          }`}
        >
          <Database className="w-4 h-4" />
          <span>Real Database Scans</span>
          <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${
            activeSource === 'backend' ? 'bg-white/25 text-white' : 'bg-slate-100 text-slate-700'
          }`}>
            {totalElements}
          </span>
        </button>

        <button
          onClick={() => setActiveSource('demo')}
          className={`px-4 py-2.5 rounded-xl text-xs sm:text-sm font-bold flex items-center gap-2 transition-all cursor-pointer ${
            activeSource === 'demo'
              ? 'bg-gov-blue text-white shadow-sm'
              : 'bg-white border border-slate-200 text-slate-600 hover:bg-slate-50'
          }`}
        >
          <Layers className="w-4 h-4" />
          <span>Demo Samples</span>
          <span className={`px-2 py-0.5 rounded-full text-xs font-bold ${
            activeSource === 'demo' ? 'bg-white/25 text-white' : 'bg-slate-100 text-slate-700'
          }`}>
            {demoHistoryList?.length || 4}
          </span>
        </button>
      </div>

      {/* Error Alert if backend unreachable */}
      {error && activeSource === 'backend' && (
        <div className="mb-6 p-4 rounded-xl bg-red-50 border border-red-200 text-red-800 text-xs sm:text-sm flex items-start justify-between gap-3 animate-fadeIn">
          <div className="flex items-start gap-2.5">
            <AlertTriangle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
            <div>
              <strong className="font-bold block text-red-900 mb-0.5">Connection Error</strong>
              <p className="text-red-700">{error}</p>
            </div>
          </div>
          <button
            onClick={() => fetchBackendHistory(0)}
            className="px-3 py-1 rounded-lg text-xs font-bold bg-white text-red-700 border border-red-300 hover:bg-red-100 shrink-0"
          >
            Retry
          </button>
        </div>
      )}

      {/* Search and Status Filters */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-subtle mb-6 flex flex-col sm:flex-row items-center gap-3">
        
        {/* Search Input */}
        <div className="relative flex-1 w-full">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            placeholder={activeSource === 'backend' ? "Search by product, brand, or filename..." : "Search demo records..."}
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-9 pr-4 py-2 rounded-xl border border-slate-200 text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-gov-blue/20 focus:border-gov-blue transition-all"
          />
        </div>

        {/* Filter Badges */}
        <div className="flex items-center gap-2 w-full sm:w-auto overflow-x-auto pb-1 sm:pb-0">
          <button
            onClick={() => setStatusFilter('ALL')}
            className={`px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold whitespace-nowrap transition-all cursor-pointer ${
              statusFilter === 'ALL'
                ? 'bg-slate-900 text-white'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            All ({activeDataset.length})
          </button>

          <button
            onClick={() => setStatusFilter('PASS')}
            className={`px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold whitespace-nowrap transition-all flex items-center gap-1.5 cursor-pointer ${
              statusFilter === 'PASS'
                ? 'bg-emerald-600 text-white'
                : 'bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
            }`}
          >
            <CheckCircle2 className="w-4 h-4" />
            Pass
          </button>

          <button
            onClick={() => setStatusFilter('WARNING')}
            className={`px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold whitespace-nowrap transition-all flex items-center gap-1.5 cursor-pointer ${
              statusFilter === 'WARNING'
                ? 'bg-amber-600 text-white'
                : 'bg-amber-50 text-amber-700 hover:bg-amber-100'
            }`}
          >
            <AlertTriangle className="w-4 h-4" />
            Warning
          </button>

          <button
            onClick={() => setStatusFilter('VIOLATION')}
            className={`px-3.5 py-2 rounded-xl text-xs sm:text-sm font-semibold whitespace-nowrap transition-all flex items-center gap-1.5 cursor-pointer ${
              statusFilter === 'VIOLATION'
                ? 'bg-red-600 text-white'
                : 'bg-red-50 text-red-700 hover:bg-red-100'
            }`}
          >
            <XCircle className="w-4 h-4" />
            Violation
          </button>
        </div>

      </div>

      {/* Loading Skeleton / Spinner State */}
      {isLoading ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center shadow-subtle animate-fadeIn">
          <div className="w-10 h-10 border-3 border-gov-blue border-t-transparent rounded-full animate-spin mx-auto mb-3"></div>
          <h3 className="text-sm font-bold text-slate-800">Loading Scan History</h3>
          <p className="text-xs text-slate-500 mt-1">Retrieving persistent audit records from backend database...</p>
        </div>
      ) : filteredList.length === 0 ? (
        /* Empty State */
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center shadow-subtle animate-fadeIn">
          <div className="w-12 h-12 rounded-full bg-slate-100 text-slate-400 flex items-center justify-center mx-auto mb-3">
            {activeSource === 'backend' ? <Database className="w-6 h-6 text-slate-400" /> : <Search className="w-6 h-6" />}
          </div>
          <h3 className="text-base font-bold text-slate-800">
            {searchQuery ? 'No matching scan records' : activeSource === 'backend' ? 'No Scans in Database Yet' : 'Demo history is empty'}
          </h3>
          <p className="text-xs text-slate-500 mt-1 mb-5 max-w-sm mx-auto">
            {searchQuery
              ? 'Try adjusting your search keywords or status filter.'
              : activeSource === 'backend'
              ? 'Upload or capture a food label photograph in the Scanner to create persistent compliance records.'
              : 'Reset demo samples to restore demonstration products.'}
          </p>
          <div className="flex items-center justify-center gap-2">
            <button
              onClick={onScanNew}
              className="px-4 py-2 rounded-xl text-xs font-bold text-white bg-gov-blue hover:bg-blue-700 shadow-sm transition-colors"
            >
              Scan a Label
            </button>
            {activeSource === 'backend' ? (
              <button
                onClick={() => setActiveSource('demo')}
                className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
              >
                View Demo Samples
              </button>
            ) : (
              <button
                onClick={onResetHistory}
                className="px-4 py-2 rounded-xl text-xs font-semibold bg-slate-100 hover:bg-slate-200 text-slate-700 transition-colors"
              >
                Restore Demo Data
              </button>
            )}
          </div>
        </div>
      ) : (
        /* History Items List */
        <div className="space-y-3">
          {filteredList.map((item) => {
            const isPass = item.status === 'PASS';
            const isWarning = item.status === 'WARNING';
            const isCardLoading = loadingScanId === item.scanId;

            return (
              <div
                key={item.id || item.scanId}
                onClick={() => !isCardLoading && handleItemClick(item)}
                className={`group bg-white rounded-xl border border-slate-200 hover:border-gov-blue/50 p-4 sm:p-5 transition-all shadow-subtle hover:shadow-card cursor-pointer flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 ${
                  isCardLoading ? 'opacity-70 pointer-events-none' : ''
                }`}
              >
                {/* Left product details */}
                <div className="flex items-center gap-3.5 min-w-0">
                  <div className="w-12 h-12 rounded-xl bg-slate-100 border border-slate-200 flex items-center justify-center text-2xl shrink-0 group-hover:scale-105 transition-transform">
                    {item.thumbnailEmoji || '📦'}
                  </div>

                  <div className="min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={`px-2.5 py-0.5 rounded text-xs font-extrabold uppercase ${
                        isPass ? 'bg-emerald-100 text-emerald-800' :
                        isWarning ? 'bg-amber-100 text-amber-900' :
                        'bg-red-100 text-red-900'
                      }`}>
                        {formatStatusDisplay(item.status)}
                      </span>
                      <span className="text-xs sm:text-sm text-slate-600 font-medium">
                        {item.category}
                      </span>
                      {item.language && (
                        <span className="px-2 py-0.5 rounded text-xs font-medium bg-slate-100 text-slate-700 border border-slate-200">
                          {formatLanguageDisplay(item.language)}
                        </span>
                      )}
                      {item.isRealScan && (
                        <span className="px-2 py-0.5 rounded text-xs font-mono font-semibold bg-blue-50 text-gov-blue border border-blue-100">
                          H2 Database
                        </span>
                      )}
                    </div>

                    <h3 className="text-sm sm:text-base font-bold text-slate-900 group-hover:text-gov-blue transition-colors truncate">
                      {item.name}
                    </h3>

                    <div className="flex items-center gap-3 text-xs sm:text-sm text-slate-500 mt-1 font-medium">
                      <span className="flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        {item.scannedAt}
                      </span>
                      <span>•</span>
                      <span>Brand: {item.brand}</span>
                    </div>
                  </div>
                </div>

                {/* Right score and view button */}
                <div className="flex items-center justify-between sm:justify-end gap-4 w-full sm:w-auto pt-3 sm:pt-0 border-t sm:border-t-0 border-slate-100">
                  <div className="text-left sm:text-right">
                    <div className="text-xs uppercase font-bold text-slate-500">
                      Screening Score
                    </div>
                    <div className={`text-xl sm:text-2xl font-black font-display ${
                      item.score >= 85 ? 'text-emerald-700' :
                      item.score >= 50 ? 'text-amber-700' : 'text-red-700'
                    }`}>
                      {item.score} <span className="text-xs sm:text-sm font-normal text-slate-400">/ 100</span>
                    </div>
                  </div>

                  <div className="w-9 h-9 rounded-xl bg-slate-50 group-hover:bg-blue-50 text-slate-400 group-hover:text-gov-blue border border-slate-200 group-hover:border-blue-200 flex items-center justify-center transition-colors">
                    {isCardLoading ? (
                      <Loader2 className="w-4 h-4 text-gov-blue animate-spin" />
                    ) : (
                      <ArrowRight className="w-4 h-4 group-hover:translate-x-0.5 transition-transform" />
                    )}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Pagination Controls (for real database scans) */}
      {activeSource === 'backend' && totalPages > 1 && (
        <div className="mt-6 pt-4 border-t border-slate-200 flex items-center justify-between text-xs sm:text-sm text-slate-600 font-medium">
          <div>
            Page <strong className="text-slate-800">{currentPage + 1}</strong> of <strong className="text-slate-800">{totalPages}</strong> ({totalElements} total scans)
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => fetchBackendHistory(currentPage - 1)}
              disabled={currentPage <= 0 || isLoading}
              className="px-3.5 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-semibold cursor-pointer"
            >
              <ChevronLeft className="w-4 h-4" />
              Previous
            </button>
            <button
              onClick={() => fetchBackendHistory(currentPage + 1)}
              disabled={currentPage >= totalPages - 1 || isLoading}
              className="px-3.5 py-2 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 disabled:opacity-40 disabled:cursor-not-allowed flex items-center gap-1 font-semibold cursor-pointer"
            >
              Next
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}

    </div>
  );
}

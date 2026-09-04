import React, { useState } from 'react';
import { 
  ShieldCheck, 
  Scan, 
  History, 
  FileText, 
  Home, 
  Menu, 
  X, 
  Sparkles,
  Smartphone
} from 'lucide-react';

export default function Navbar({ activePage, setActivePage, activeProduct, onQuickDemo, onOpenMobileQr }) {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleNav = (page) => {
    setActivePage(page);
    setMobileMenuOpen(false);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <>
      {/* Indian Tricolor Top Accent Stripe */}
      <div className="tricolor-bar w-full fixed top-0 left-0 z-50"></div>

      {/* Main Navigation Header */}
      <header className="sticky top-[3.5px] z-40 bg-white/95 backdrop-blur-md border-b border-slate-200/90 shadow-sm transition-all">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            
            {/* Logo & Hackathon Identity */}
            <div 
              onClick={() => handleNav('home')}
              className="flex items-center gap-3 cursor-pointer group"
            >
              <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-gov-navy to-gov-blue flex items-center justify-center text-white shadow-md shadow-blue-900/20 group-hover:scale-105 transition-transform">
                <ShieldCheck className="w-6 h-6 text-amber-400" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-display font-extrabold text-xl sm:text-2xl tracking-tight text-gov-navy">
                    Label<span className="text-gov-blue">Check</span>
                  </span>
                  <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-bold bg-amber-100 text-amber-900 border border-amber-300">
                    SIH26034
                  </span>
                </div>
                <p className="text-xs text-slate-500 hidden sm:block font-medium">
                  Packaged Product Compliance Scanner
                </p>
              </div>
            </div>

            {/* Desktop Navigation Links */}
            <nav className="hidden md:flex items-center gap-1 lg:gap-2">
              <button
                id="nav-home"
                onClick={() => handleNav('home')}
                className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-1.5 ${
                  activePage === 'home'
                    ? 'bg-blue-50 text-gov-blue font-bold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Home className="w-4 h-4" />
                Home
              </button>

              <button
                id="nav-scanner"
                onClick={() => handleNav('scanner')}
                className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-1.5 ${
                  activePage === 'scanner'
                    ? 'bg-blue-50 text-gov-blue font-bold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Scan className="w-4 h-4" />
                Label Scanner
              </button>

              <button
                id="nav-result"
                onClick={() => handleNav('result')}
                className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-1.5 relative ${
                  activePage === 'result'
                    ? 'bg-blue-50 text-gov-blue font-bold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <FileText className="w-4 h-4" />
                Compliance Result
                {activeProduct && (
                  <span className={`w-2 h-2 rounded-full ${
                    activeProduct.status === 'PASS' ? 'bg-emerald-500' :
                    activeProduct.status === 'WARNING' ? 'bg-amber-500' : 'bg-red-500'
                  } animate-pulse`}></span>
                )}
              </button>

              <button
                id="nav-history"
                onClick={() => handleNav('history')}
                className={`px-3.5 py-2 rounded-lg text-sm font-semibold transition-colors flex items-center gap-1.5 ${
                  activePage === 'history'
                    ? 'bg-blue-50 text-gov-blue font-bold'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <History className="w-4 h-4" />
                Scan History
              </button>
            </nav>

            {/* Quick Action Button */}
            <div className="hidden sm:flex items-center gap-2">
              <button
                id="nav-mobile-qr"
                onClick={onOpenMobileQr}
                className="px-3.5 py-2 rounded-lg text-xs sm:text-sm font-semibold text-slate-700 bg-slate-100 hover:bg-slate-200 border border-slate-200 transition-all flex items-center gap-1.5 shadow-sm active:scale-95"
                title="Scan QR code to open on phone"
              >
                <Smartphone className="w-4 h-4 text-gov-blue" />
                <span className="hidden lg:inline">Open on</span> Phone
              </button>

              <button
                id="nav-quick-demo"
                onClick={onQuickDemo}
                className="px-3.5 py-2 rounded-lg text-xs sm:text-sm font-semibold text-amber-900 bg-amber-50 hover:bg-amber-100 border border-amber-200 transition-all flex items-center gap-1.5 shadow-sm active:scale-95"
                title="Instant test with pre-loaded product"
              >
                <Sparkles className="w-4 h-4 text-amber-600" />
                Try Live Demo
              </button>

              <button
                onClick={() => handleNav('scanner')}
                className="px-4 py-2 rounded-lg text-xs sm:text-sm font-bold text-white bg-gov-blue hover:bg-blue-700 transition-all flex items-center gap-1.5 shadow-sm shadow-blue-500/20 active:scale-95"
              >
                <Scan className="w-4 h-4" />
                Scan Product
              </button>
            </div>

            {/* Mobile Menu Button */}
            <div className="flex md:hidden items-center gap-2">
              <button
                onClick={onQuickDemo}
                className="px-3 py-1.5 rounded-lg text-xs sm:text-sm font-bold text-amber-900 bg-amber-100 border border-amber-300 flex items-center gap-1"
              >
                <Sparkles className="w-3.5 h-3.5 text-amber-700" />
                Demo
              </button>

              <button
                id="mobile-menu-toggle"
                onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                className="p-2 rounded-lg text-slate-600 hover:text-slate-900 hover:bg-slate-100 focus:outline-none"
                aria-label="Toggle navigation menu"
              >
                {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
              </button>
            </div>

          </div>
        </div>

        {/* Mobile Dropdown Navigation */}
        {mobileMenuOpen && (
          <div className="md:hidden bg-white border-b border-slate-200 px-4 pt-2 pb-4 space-y-2 shadow-lg animate-fadeIn">
            <button
              onClick={() => handleNav('home')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold ${
                activePage === 'home' ? 'bg-blue-50 text-gov-blue' : 'text-slate-700 hover:bg-slate-50'
              }`}
            >
              <Home className="w-4 h-4 text-gov-blue" />
              Home Overview
            </button>

            <button
              onClick={() => handleNav('scanner')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold ${
                activePage === 'scanner' ? 'bg-blue-50 text-gov-blue' : 'text-slate-700 hover:bg-slate-50'
              }`}
            >
              <Scan className="w-4 h-4 text-gov-blue" />
              Product Label Scanner
            </button>

            <button
              onClick={() => handleNav('result')}
              className={`w-full flex items-center justify-between px-3 py-2.5 rounded-lg text-sm font-semibold ${
                activePage === 'result' ? 'bg-blue-50 text-gov-blue' : 'text-slate-700 hover:bg-slate-50'
              }`}
            >
              <div className="flex items-center gap-3">
                <FileText className="w-4 h-4 text-gov-blue" />
                Compliance Report
              </div>
              {activeProduct && (
                <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                  activeProduct.status === 'PASS' ? 'bg-emerald-100 text-emerald-800' :
                  activeProduct.status === 'WARNING' ? 'bg-amber-100 text-amber-800' : 'bg-red-100 text-red-800'
                }`}>
                  {activeProduct.status}
                </span>
              )}
            </button>

            <button
              onClick={() => handleNav('history')}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold ${
                activePage === 'history' ? 'bg-blue-50 text-gov-blue' : 'text-slate-700 hover:bg-slate-50'
              }`}
            >
              <History className="w-4 h-4 text-gov-blue" />
              Scan History
            </button>

            <button
              onClick={() => {
                onOpenMobileQr();
                setMobileMenuOpen(false);
              }}
              className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-semibold text-slate-700 hover:bg-slate-50"
            >
              <Smartphone className="w-4 h-4 text-gov-blue" />
              Connect Another Phone (QR Code)
            </button>

            <div className="pt-2 border-t border-slate-100 flex gap-2">
              <button
                onClick={() => {
                  onQuickDemo();
                  setMobileMenuOpen(false);
                }}
                className="flex-1 py-2.5 rounded-lg text-xs font-bold text-amber-900 bg-amber-50 border border-amber-200 flex items-center justify-center gap-1.5"
              >
                <Sparkles className="w-3.5 h-3.5 text-amber-600" />
                Instant Demo
              </button>
              <button
                onClick={() => handleNav('scanner')}
                className="flex-1 py-2.5 rounded-lg text-xs font-bold text-white bg-gov-blue flex items-center justify-center gap-1.5"
              >
                <Scan className="w-3.5 h-3.5" />
                Scan Now
              </button>
            </div>
          </div>
        )}
      </header>

      {/* Mobile Bottom Navigation Bar for rapid thumb access on phones */}
      <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 px-3 py-2 flex items-center justify-around shadow-lg">
        <button
          onClick={() => handleNav('home')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-lg transition-colors ${
            activePage === 'home' ? 'text-gov-blue font-bold' : 'text-slate-600'
          }`}
        >
          <Home className="w-5 h-5" />
          <span className="text-xs font-medium">Home</span>
        </button>

        <button
          onClick={() => handleNav('scanner')}
          className={`flex flex-col items-center gap-1 py-1 px-3.5 rounded-lg transition-colors ${
            activePage === 'scanner' ? 'text-gov-blue font-bold' : 'text-slate-600'
          }`}
        >
          <div className="relative">
            <Scan className="w-5 h-5" />
            <span className="absolute -top-1 -right-1 flex h-2 w-2">
              <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-blue-400 opacity-75"></span>
              <span className="relative inline-flex rounded-full h-2 w-2 bg-blue-500"></span>
            </span>
          </div>
          <span className="text-xs font-medium">Scan</span>
        </button>

        <button
          onClick={() => handleNav('result')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-lg transition-colors ${
            activePage === 'result' ? 'text-gov-blue font-bold' : 'text-slate-600'
          }`}
        >
          <FileText className="w-5 h-5" />
          <span className="text-xs font-medium">Result</span>
        </button>

        <button
          onClick={() => handleNav('history')}
          className={`flex flex-col items-center gap-1 py-1 px-2.5 rounded-lg transition-colors ${
            activePage === 'history' ? 'text-gov-blue font-bold' : 'text-slate-600'
          }`}
        >
          <History className="w-5 h-5" />
          <span className="text-xs font-medium">History</span>
        </button>
      </div>
    </>
  );
}

import React from 'react';
import { 
  Scan, 
  Sparkles, 
  Cpu, 
  FileCheck2, 
  CheckCircle2, 
  Scale, 
  ArrowRight, 
  FileSearch,
  CheckCircle,
  Building2,
  PhoneCall,
  Calendar,
  IndianRupee,
  ShieldCheck
} from 'lucide-react';

export default function LandingPage({ onScanClick, onViewDemo, onSelectProduct, sampleProducts }) {
  return (
    <div className="min-h-screen pb-20 md:pb-12">
      {/* Hero Section */}
      <section className="relative overflow-hidden pt-8 pb-16 md:pt-14 md:pb-24 bg-gradient-to-b from-slate-100 via-white to-slate-50 border-b border-slate-200">
        {/* Subtle background decorative shapes */}
        <div className="absolute top-0 left-1/2 -translate-x-1/2 w-full max-w-7xl h-full overflow-hidden pointer-events-none opacity-40">
          <div className="absolute -top-32 -left-32 w-96 h-96 rounded-full bg-blue-200/50 blur-3xl"></div>
          <div className="absolute top-1/3 -right-24 w-96 h-96 rounded-full bg-amber-200/40 blur-3xl"></div>
          <div className="absolute bottom-10 left-1/4 w-80 h-80 rounded-full bg-emerald-200/40 blur-3xl"></div>
        </div>

        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 relative z-10">
          
          {/* Government / SIH Banner Pill */}
          <div className="flex justify-center mb-6">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-white shadow-sm border border-slate-300/80 text-xs sm:text-sm font-semibold text-slate-700">
              <span className="flex h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></span>
              <span className="font-bold text-gov-blue">Smart India Hackathon 2026</span>
              <span className="text-slate-300">|</span>
              <span className="text-slate-600">Problem Statement SIH26034</span>
            </div>
          </div>

          {/* Hero Content */}
          <div className="text-center max-w-3xl mx-auto">
            <div className="flex items-center justify-center gap-2 mb-3 text-gov-blue font-bold text-sm tracking-wide uppercase">
              <Scale className="w-4 h-4 text-gov-blue" />
              <span>Ministry of Consumer Affairs & Legal Metrology Benchmark</span>
            </div>

            <h1 className="text-3xl sm:text-4xl md:text-5xl lg:text-6xl font-extrabold tracking-tight text-slate-900 leading-tight">
              AI-Powered Packaged Product <br className="hidden sm:inline" />
              <span className="bg-clip-text text-transparent bg-gradient-to-r from-gov-navy via-gov-blue to-blue-600">
                Compliance Scanner
              </span>
            </h1>

            <p className="mt-5 text-base sm:text-lg md:text-xl text-slate-600 max-w-2xl mx-auto leading-relaxed">
              Scan a product label and instantly identify missing or potentially non-compliant declarations under the 
              <strong className="text-slate-800 font-semibold"> Legal Metrology (Packaged Commodities) Rules, 2011</strong>.
            </p>

            {/* Primary & Secondary Call to Action */}
            <div className="mt-8 sm:mt-10 flex flex-col sm:flex-row items-center justify-center gap-3 sm:gap-4 max-w-md mx-auto">
              <button
                id="hero-scan-btn"
                onClick={onScanClick}
                className="w-full sm:w-auto px-8 py-3.5 rounded-xl font-bold text-white bg-gov-blue hover:bg-blue-700 active:scale-95 transition-all shadow-lg shadow-blue-900/20 flex items-center justify-center gap-2.5 text-base"
              >
                <Scan className="w-5 h-5" />
                Scan Product
              </button>

              <button
                id="hero-demo-btn"
                onClick={onViewDemo}
                className="w-full sm:w-auto px-7 py-3.5 rounded-xl font-semibold text-slate-700 bg-white hover:bg-slate-50 active:scale-95 border border-slate-300 hover:border-slate-400 transition-all shadow-sm flex items-center justify-center gap-2 text-base"
              >
                <Sparkles className="w-5 h-5 text-amber-500" />
                View Demo
              </button>
            </div>

            {/* Key Trust Signals */}
            <div className="mt-8 pt-6 border-t border-slate-200/80 flex flex-wrap items-center justify-center gap-6 sm:gap-10 text-xs sm:text-sm font-semibold text-slate-600">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>Legal Metrology Rule 6 Compliant</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>Unit Sale Price (USP) Check</span>
              </div>
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span>FSSAI & Packer Norms</span>
              </div>
            </div>

          </div>

          {/* Quick Demo Selector Cards - One Click to Test */}
          <div className="mt-12 sm:mt-16 max-w-4xl mx-auto bg-white/80 backdrop-blur rounded-2xl p-5 sm:p-6 border border-slate-200 shadow-gov">
            <div className="flex items-center justify-between mb-4">
              <div>
                <h3 className="text-base sm:text-lg font-bold text-slate-900 flex items-center gap-2">
                  <Sparkles className="w-4 h-4 text-amber-500" />
                  Try a Pre-Configured Test Product
                </h3>
                <p className="text-xs sm:text-sm text-slate-600 font-medium">
                  Instant analysis without taking a photo — test compliant and non-compliant samples:
                </p>
              </div>
              <span className="hidden sm:inline-block text-xs font-bold text-gov-blue uppercase tracking-wider">
                Live Test Cases
              </span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              {sampleProducts.slice(0, 3).map((prod) => (
                <div
                  key={prod.id}
                  onClick={() => onSelectProduct(prod)}
                  className="group p-4 rounded-xl border border-slate-200 hover:border-gov-blue/50 hover:bg-blue-50/40 cursor-pointer transition-all duration-200 flex flex-col justify-between bg-white shadow-subtle hover:shadow-card"
                >
                  <div>
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-2xl">{prod.imageEmoji}</span>
                      <span className={`px-2.5 py-0.5 rounded text-xs font-extrabold uppercase tracking-wide border ${
                        prod.status === 'PASS' ? 'bg-emerald-100 text-emerald-800 border-emerald-200' :
                        prod.status === 'WARNING' ? 'bg-amber-100 text-amber-800 border-amber-200' :
                        'bg-red-100 text-red-800 border-red-200'
                      }`}>
                        {prod.status} • {prod.overallScore}%
                      </span>
                    </div>
                    <h4 className="text-sm sm:text-base font-bold text-slate-800 group-hover:text-gov-blue transition-colors line-clamp-1">
                      {prod.name}
                    </h4>
                    <p className="text-xs text-slate-500 mt-0.5">
                      {prod.category}
                    </p>
                  </div>
                  
                  <div className="mt-3.5 pt-2 border-t border-slate-100 flex items-center justify-between text-xs sm:text-sm font-bold text-gov-blue">
                    <span>Inspect Verdict</span>
                    <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
                  </div>
                </div>
              ))}
            </div>
          </div>

        </div>
      </section>

      {/* Feature Cards Section */}
      <section className="py-14 sm:py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <h2 className="text-xs font-bold text-gov-blue uppercase tracking-widest mb-2">
              Core Capabilities
            </h2>
            <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              Intelligent Automated Label Inspection
            </p>
            <p className="mt-3 text-sm sm:text-base text-slate-600">
              Designed for packaging manufacturers, e-commerce compliance teams, and Legal Metrology field inspectors.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 lg:gap-8">
            
            {/* Feature 1: AI-assisted label reading */}
            <div className="bg-slate-50 rounded-2xl p-6 sm:p-7 border border-slate-200/80 shadow-subtle hover:shadow-card transition-all group">
              <div className="w-12 h-12 rounded-xl bg-blue-100 text-gov-blue flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <Cpu className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-2">
                AI-Assisted Label Reading
              </h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Extracts printed text, tabular nutrition grids, barcodes, and manufacturing codes even from curved bottles, reflective pouches, and low-contrast labels.
              </p>
              <ul className="mt-4 space-y-2 text-xs text-slate-500">
                <li className="flex items-center gap-2">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Optical Character Recognition (OCR)</span>
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Numeral font height estimation</span>
                </li>
              </ul>
            </div>

            {/* Feature 2: Compliance checking */}
            <div className="bg-slate-50 rounded-2xl p-6 sm:p-7 border border-slate-200/80 shadow-subtle hover:shadow-card transition-all group">
              <div className="w-12 h-12 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <Scale className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-2">
                Statutory Compliance Checking
              </h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Rules engine verifies declarations against Rule 6 of Legal Metrology 2011: MRP, Net Quantity units, complete physical address, and grievance phone numbers.
              </p>
              <ul className="mt-4 space-y-2 text-xs text-slate-500">
                <li className="flex items-center gap-2">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Legal Metrology Act 2009 checks</span>
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Unit Sale Price (USP) validation</span>
                </li>
              </ul>
            </div>

            {/* Feature 3: Instant compliance report */}
            <div className="bg-slate-50 rounded-2xl p-6 sm:p-7 border border-slate-200/80 shadow-subtle hover:shadow-card transition-all group">
              <div className="w-12 h-12 rounded-xl bg-emerald-100 text-emerald-800 flex items-center justify-center mb-5 group-hover:scale-110 transition-transform">
                <FileCheck2 className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-bold text-slate-900 mb-2">
                Instant Compliance Report
              </h3>
              <p className="text-sm text-slate-600 leading-relaxed">
                Clear PASS / WARNING / VIOLATION summary score with citations to legal clauses and actionable instructions for packaging designers and QA teams.
              </p>
              <ul className="mt-4 space-y-2 text-xs text-slate-500">
                <li className="flex items-center gap-2">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Defect severity highlighting</span>
                </li>
                <li className="flex items-center gap-2">
                  <CheckCircle className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Printable inspection summary report</span>
                </li>
              </ul>
            </div>

          </div>
        </div>
      </section>

      {/* "How It Works" Section */}
      <section className="py-14 sm:py-20 bg-slate-100/70 border-y border-slate-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center max-w-2xl mx-auto mb-12">
            <h2 className="text-xs font-bold text-gov-blue uppercase tracking-widest mb-2">
              Workflow
            </h2>
            <p className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight">
              How LabelCheck Works
            </p>
            <p className="mt-3 text-sm sm:text-base text-slate-600">
              Four streamlined steps from label photograph to legal verification.
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-5">
            
            {/* Step 1 */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-subtle relative flex flex-col justify-between">
              <div>
                <span className="text-3xl font-black text-blue-100 block mb-3 font-display">01</span>
                <div className="w-10 h-10 rounded-lg bg-blue-50 text-gov-blue flex items-center justify-center mb-3">
                  <Scan className="w-5 h-5" />
                </div>
                <h4 className="text-base font-bold text-slate-900 mb-1">Upload / Capture</h4>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                  Snap a picture of the back-of-pack or front-of-pack label using your smartphone camera or upload existing packaging artwork.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 text-xs text-slate-500 font-semibold">
                Step 1: Input Acquisition
              </div>
            </div>

            {/* Step 2 */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-subtle relative flex flex-col justify-between">
              <div>
                <span className="text-3xl font-black text-blue-100 block mb-3 font-display">02</span>
                <div className="w-10 h-10 rounded-lg bg-blue-50 text-gov-blue flex items-center justify-center mb-3">
                  <FileSearch className="w-5 h-5" />
                </div>
                <h4 className="text-base font-bold text-slate-900 mb-1">Extract Label Information</h4>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                  OCR extracts key declarations: MRP, Net Quantity, Mfg Date, Expiry, Manufacturer Address, Customer Care contacts, and Barcode.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 text-xs text-slate-500 font-semibold">
                Step 2: Entity Recognition
              </div>
            </div>

            {/* Step 3 */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-subtle relative flex flex-col justify-between">
              <div>
                <span className="text-3xl font-black text-blue-100 block mb-3 font-display">03</span>
                <div className="w-10 h-10 rounded-lg bg-blue-50 text-gov-blue flex items-center justify-center mb-3">
                  <Scale className="w-5 h-5" />
                </div>
                <h4 className="text-base font-bold text-slate-900 mb-1">Check Compliance</h4>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                  The rule engine validates units (g/kg/ml), font heights against package area, presence of tax declaration, and complaint redressal channels.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 text-xs text-slate-500 font-semibold">
                Step 3: Rule Verification
              </div>
            </div>

            {/* Step 4 */}
            <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-subtle relative flex flex-col justify-between">
              <div>
                <span className="text-3xl font-black text-blue-100 block mb-3 font-display">04</span>
                <div className="w-10 h-10 rounded-lg bg-blue-50 text-gov-blue flex items-center justify-center mb-3">
                  <FileCheck2 className="w-5 h-5" />
                </div>
                <h4 className="text-base font-bold text-slate-900 mb-1">Get Report</h4>
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                  Receive an actionable score (0-100) with pass/fail badges, pinpointed violations, legal citations, and downloadable inspection certificates.
                </p>
              </div>
              <div className="mt-4 pt-3 border-t border-slate-100 text-xs text-slate-500 font-semibold">
                Step 4: Actionable Output
              </div>
            </div>

          </div>
        </div>
      </section>

      {/* Mandatory Declarations Checklist Summary */}
      <section className="py-14 sm:py-16 bg-white">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="bg-slate-900 text-white rounded-3xl p-6 sm:p-10 shadow-xl overflow-hidden relative">
            <div className="absolute right-0 top-0 w-80 h-80 bg-blue-600/10 rounded-full blur-3xl pointer-events-none"></div>

            <div className="max-w-2xl">
              <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold bg-blue-900/60 text-blue-300 border border-blue-700/50 mb-4">
                Statutory Coverage
              </span>
              <h3 className="text-xl sm:text-2xl font-bold tracking-tight text-white mb-3">
                Mandatory Declarations Checked Under Rule 6(1)
              </h3>
              <p className="text-xs sm:text-sm text-slate-300 leading-relaxed mb-6">
                Under the Legal Metrology (Packaged Commodities) Rules, every pre-packaged commodity sold in India must carry these six non-negotiable declarations:
              </p>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4 text-xs sm:text-sm">
              <div className="bg-slate-800/80 rounded-xl p-3.5 border border-slate-700/60 flex items-start gap-2.5">
                <IndianRupee className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-slate-100">MRP & Unit Sale Price</div>
                  <div className="text-xs text-slate-300 mt-0.5 font-normal">Incl. of all taxes + ₹/g or ₹/ml</div>
                </div>
              </div>

              <div className="bg-slate-800/80 rounded-xl p-3.5 border border-slate-700/60 flex items-start gap-2.5">
                <Scale className="w-4 h-4 text-emerald-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-slate-100">Net Quantity</div>
                  <div className="text-xs text-slate-300 mt-0.5 font-normal">Standard metric unit & min font size</div>
                </div>
              </div>

              <div className="bg-slate-800/80 rounded-xl p-3.5 border border-slate-700/60 flex items-start gap-2.5">
                <Building2 className="w-4 h-4 text-blue-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-slate-100">Manufacturer Address</div>
                  <div className="text-xs text-slate-300 mt-0.5 font-normal">Complete registered premise with PIN</div>
                </div>
              </div>

              <div className="bg-slate-800/80 rounded-xl p-3.5 border border-slate-700/60 flex items-start gap-2.5">
                <Calendar className="w-4 h-4 text-purple-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-slate-100">Mfg / Pkg Date</div>
                  <div className="text-xs text-slate-300 mt-0.5 font-normal">Month, year and best-before validity</div>
                </div>
              </div>

              <div className="bg-slate-800/80 rounded-xl p-3.5 border border-slate-700/60 flex items-start gap-2.5">
                <PhoneCall className="w-4 h-4 text-rose-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-slate-100">Consumer Care</div>
                  <div className="text-xs text-slate-300 mt-0.5 font-normal">Helpline phone, email & address</div>
                </div>
              </div>

              <div className="bg-slate-800/80 rounded-xl p-3.5 border border-slate-700/60 flex items-start gap-2.5">
                <ShieldCheck className="w-4 h-4 text-teal-400 shrink-0 mt-0.5" />
                <div>
                  <div className="font-bold text-slate-100">Country of Origin</div>
                  <div className="text-xs text-slate-300 mt-0.5 font-normal">Country & FSSAI Food Lic. No.</div>
                </div>
              </div>
            </div>

            <div className="mt-8 pt-6 border-t border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4">
              <span className="text-xs sm:text-sm text-slate-300 text-center sm:text-left font-medium">
                Empowering consumers and regulatory inspectors with AI verification.
              </span>
              <button
                onClick={onScanClick}
                className="px-5 py-2.5 rounded-xl text-xs sm:text-sm font-bold bg-white text-slate-900 hover:bg-slate-100 transition-colors flex items-center gap-2 shrink-0 cursor-pointer"
              >
                <Scan className="w-4 h-4 text-gov-blue" />
                Launch Label Scanner
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-12 border-t border-slate-200 bg-white py-8 text-center text-xs sm:text-sm text-slate-500">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-800">LabelCheck</span>
            <span>—</span>
            <span>AI Packaged Product Compliance Scanner</span>
          </div>
          <div className="font-semibold text-slate-600">
            SIH 2026 Demo • Problem Statement SIH26034
          </div>
        </div>
      </footer>
    </div>
  );
}

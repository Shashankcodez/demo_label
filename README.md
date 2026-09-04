# LabelCheck (SIH26034) — Automated Packaged Commodity Regulatory Compliance

LabelCheck is an AI-assisted statutory packaging compliance screening system built for the Smart India Hackathon 2026. It verifies packaged food and consumer commodity declarations under **The Legal Metrology Act, 2009**, **Legal Metrology (Packaged Commodities) Rules, 2011**, and **FSSAI Packaging & Labelling Regulations**.

---

## Architecture

```
  PHOTO OF ANY REASONABLY CLEAR PACKAGED-FOOD LABEL
                          ↓
   Image Quality Assessment (Non-blocking: underexposure, overexposure, dimensions)
                          ↓
   Gemini 3.8 Flash Vision (Primary Source: Google Generative Language API)
                          ↓
   Local Tesseract OCR (Fallback & Visual Evidence: 11 Indic language models)
                          ↓
   Extraction Merge Service (Gemini structured layout context + OCR visual evidence)
                          ↓
   DETERMINISTIC COMPLIANCE ENGINE (Legal Metrology Rules 2011 & FSSAI Standards)
                          ↓
   PERSISTENT H2 RECORD & USEFUL RESULT SCREEN (Never blank / useless)
```

### Key Principles
1. **Gemini 3.8 Flash is Primary Vision Extraction**: Reads the original unthresholded image, preserves visual layout context (columns, tables, borders, colors), and outputs structured declarations with visual evidence snippets and confidence scores.
2. **Local Tesseract OCR is Fallback**: If Gemini is offline, unconfigured, or rate-limited (429), the system automatically and gracefully falls back to local Tesseract OCR.
3. **Deterministic Compliance Boundary**: Gemini **NEVER** decides legal compliance. The Java deterministic rule engine (`ComplianceRuleEngine`) strictly evaluates compliance against statutory packaging mandates.
4. **API Key is Strictly Backend-Only**: The Gemini API key is never exposed to the frontend, never returned by health checks, and never logged.

---

## Configuration

Set the environment variable before starting the Spring Boot backend:

```powershell
# Windows PowerShell
$env:GEMINI_API_KEY="AIzaSyYourGeminiApiKeyHere"
$env:AI_ENABLED="true"
$env:AI_PROVIDER="gemini"
$env:AI_MODEL="gemini-3.8-flash"
```

Or configure in `backend/src/main/resources/application.properties`:

```properties
app.ai.enabled=true
app.ai.provider=gemini
app.ai.api-key=${GEMINI_API_KEY:}
app.ai.model=gemini-3.8-flash
app.ai.base-url=https://generativelanguage.googleapis.com
app.ai.timeout-seconds=30
app.ai.max-retries=1
app.ai.thinking-budget=1024
```

---

## Running the Application

### 1. Backend (Spring Boot + Java 21)
```powershell
cd backend
.\mvnw.cmd spring-boot:run
```
Backend runs on `http://localhost:8080`.
Verify health status:
```bash
curl http://localhost:8080/api/v1/health
```
Response:
```json
{
  "status": "UP",
  "service": "LabelCheck Backend",
  "aiEnabled": true,
  "aiProvider": "Gemini",
  "aiModel": "gemini-3.8-flash"
}
```

### 2. Frontend (React + Vite + Tailwind CSS)
```powershell
npm run dev
```
Frontend runs on `http://localhost:5173`.

/**
 * API client for interacting with the LabelCheck Spring Boot backend service.
 */

/**
 * Resolves the active backend API base URL from Vite environment or fallback.
 *
 * @returns {string}
 */
export function getBaseUrl() {
  const viteUrl = typeof import.meta !== 'undefined' && import.meta?.env?.VITE_API_BASE_URL;
  const processUrl = typeof process !== 'undefined' && process?.env?.VITE_API_BASE_URL;
  const configuredUrl = (viteUrl || processUrl || '').trim();

  // If explicitly configured in environment:
  if (configuredUrl) {
    // If the configured URL specifies localhost/127.0.0.1, but the browser was accessed via a LAN IP / hostname (e.g. mobile phone),
    // automatically rewrite hostname to match the server host so mobile phones don't fail by querying their own local loopback.
    if (typeof window !== 'undefined' && window.location?.hostname) {
      const hostname = window.location.hostname;
      if (hostname !== 'localhost' && hostname !== '127.0.0.1' && hostname !== '') {
        try {
          const parsed = new URL(configuredUrl);
          if (parsed.hostname === 'localhost' || parsed.hostname === '127.0.0.1') {
            parsed.hostname = hostname;
            return parsed.origin.replace(/\/+$/, '');
          }
        } catch {
          // fallback to configuredUrl if parsing fails
        }
      }
    }
    return configuredUrl.replace(/\/+$/, '');
  }

  // Fallback: in browser, dynamically bind to active hostname (supports both localhost on desktop and LAN IP on mobile)
  if (typeof window !== 'undefined' && window.location?.hostname) {
    const protocol = window.location.protocol || 'http:';
    const hostname = window.location.hostname || 'localhost';
    return `${protocol}//${hostname}:8080`;
  }

  return 'http://localhost:8080';
}

/**
 * Uploads a packaged commodity label image for validation, Tesseract OCR,
 * entity extraction, and statutory compliance evaluation.
 *
 * @param {File | Blob} imageFile The image file or blob to upload
 * @param {string} [filename] Optional fallback filename when passing a Blob
 * @param {string} [language='eng'] Optional OCR language code or combination (e.g. 'eng', 'eng+hin')
 * @returns {Promise<Object>} Complete backend scan analysis response
 * @throws {Error} User-friendly error message on failure
 */
export async function scanLabel(imageFile, filename = 'uploaded_label.jpg', language = 'eng') {
  if (!imageFile) {
    throw new Error('No image file provided for analysis.');
  }

  const baseUrl = getBaseUrl();
  const formData = new FormData();
  // Field name MUST be 'image' matching backend @RequestParam("image")
  if (imageFile instanceof File) {
    formData.append('image', imageFile);
  } else {
    formData.append('image', imageFile, filename);
  }

  const cleanLang = (language && typeof language === 'string' && language.trim()) ? language.trim() : 'eng';
  const queryParam = `?language=${encodeURIComponent(cleanLang)}`;

  let response;
  try {
    response = await fetch(`${baseUrl}/api/v1/scan${queryParam}`, {
      method: 'POST',
      // DO NOT manually set Content-Type header; browser must set multipart/form-data boundary automatically
      body: formData
    });
  } catch (err) {
    console.error('LabelCheck backend connection failed:', err);
    throw new Error(
      `Unable to connect to LabelCheck backend. Make sure the backend is running at ${baseUrl}.`
    );
  }

  let data;
  try {
    data = await response.json();
  } catch (err) {
    console.error('Failed to parse backend JSON response:', err);
    throw new Error('Unexpected response from analysis server.');
  }

  if (!response.ok) {
    const message = data?.message || data?.error || `Analysis failed with status code ${response.status}.`;
    throw new Error(message);
  }

  return data;
}

/**
 * Retrieves the set of supported OCR languages from the backend.
 * Safely defaults to ['eng'] on network or server error.
 *
 * @returns {Promise<string[]>} Array of language codes (e.g. ['eng', 'hin', 'tam', ...])
 */
export async function getOcrLanguages() {
  const baseUrl = getBaseUrl();
  try {
    const res = await fetch(`${baseUrl}/api/v1/ocr/languages`);
    if (!res.ok) {
      console.warn(`Backend returned status ${res.status} for OCR languages. Falling back to ['eng'].`);
      return ['eng'];
    }
    const data = await res.json();
    if (Array.isArray(data)) {
      return data;
    }
    if (Array.isArray(data?.languages)) {
      return data.languages;
    }
    return ['eng'];
  } catch (err) {
    console.warn('Could not fetch OCR languages from backend, defaulting to English:', err?.message);
    return ['eng'];
  }
}


/**
 * Checks backend service health.
 *
 * @returns {Promise<{status: string, service: string}>}
 */
export async function checkHealth() {
  const baseUrl = getBaseUrl();
  try {
    const res = await fetch(`${baseUrl}/api/v1/health`);
    if (!res.ok) return null;
    return await res.json();
  } catch {
    return null;
  }
}

/**
 * Retrieves full stored scan analysis by scanId.
 *
 * @param {string} scanId
 * @returns {Promise<Object>}
 */
export async function getScanById(scanId) {
  const baseUrl = getBaseUrl();
  let res;
  try {
    res = await fetch(`${baseUrl}/api/v1/scans/${encodeURIComponent(scanId)}`);
  } catch (err) {
    console.error('Failed to connect to backend for scan retrieval:', err);
    throw new Error(`Unable to connect to LabelCheck backend. Make sure the backend is running at ${baseUrl}.`);
  }

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to retrieve scan (${res.status})`);
  }
  return await res.json();
}

/**
 * Retrieves paginated lightweight scan history.
 *
 * @param {number} [page=0]
 * @param {number} [size=20]
 * @returns {Promise<Object>}
 */
export async function getScanHistory(page = 0, size = 20) {
  const baseUrl = getBaseUrl();
  let res;
  try {
    res = await fetch(`${baseUrl}/api/v1/scans?page=${page}&size=${size}`);
  } catch (err) {
    console.error('Failed to connect to backend for scan history:', err);
    throw new Error(`Unable to connect to LabelCheck backend. Make sure the backend is running at ${baseUrl}.`);
  }

  if (!res.ok) {
    const errorData = await res.json().catch(() => ({}));
    throw new Error(errorData.message || `Failed to retrieve scan history (${res.status})`);
  }
  return await res.json();
}

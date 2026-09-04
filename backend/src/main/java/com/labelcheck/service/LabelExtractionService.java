package com.labelcheck.service;

import com.labelcheck.dto.OcrLine;
import com.labelcheck.dto.OcrWord;
import com.labelcheck.dto.StructuredLabelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Robust contextual extraction engine for packaged food and commodity labels.
 * Employs region isolation (shielding nutrition panels), keyword context scoring,
 * strict date pattern matching, flexible Indian phone detection, FSSAI textual state recognition
 * (e.g. Applied For), and Rupee glyph OCR confusion correction (preventing ₹150 from becoming ₹2150).
 */
@Service
public class LabelExtractionService {

    private static final Logger log = LoggerFactory.getLogger(LabelExtractionService.class);

    // Nutrition lines exclusion: shields nutrition table numbers from contaminating MRP, dates, phone, etc.
    private static final Pattern NUTRITION_LINE_PATTERN = Pattern.compile(
            "(?i)\\b(calories?|kcal|kj|fat|fats?|carbohydrates?|carbs?|fiber|fibre|sugar|sugars?|proteins?|sodium|potassium|calcium|iron|vitamins?|minerals?|cholesterol|saturates?|trans|nutrition|nutritional|serving|per\\s*100g|approx|energy|daily\\s*value)\\b"
    );

    // MRP Patterns: matches ₹150, Rs 150, MRP ₹150/-, M.R.P. Rs. 150.00, MRP: ~150, MRP: z1 5 0, MRP : < 1 5 Q
    private static final Pattern MRP_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(?:M\\.?\\s*R\\.?\\s*P\\.?|Maximum\\s*Retail\\s*Price|Max\\s*Retail\\s*Price)\\b"
    );

    private static final Pattern MRP_NUMERIC_CANDIDATE = Pattern.compile(
            "(?i)(?:[₹¥$€£]|Rs\\.?|INR|[~z<\\?=\\*F])?\\s*([0-9OQoq\\s]{1,8}(?:\\.[0-9OQoq]{1,2})?)(?:\\s*[/\\\\\\-~=,]+)?"
    );

    private static final Pattern INCLUSIVE_TAX_PATTERN = Pattern.compile(
            "(?i)(?:incl(?:usive|uding)?\\.?\\s*(?:of\\s*)?(?:all\\s*)?taxes|incl\\.?\\s*(?:all\\s*)?taxes|inclusive\\s*of\\s*taxes|सभी\\s*करों\\s*सहित)"
    );

    // Unit Sale Price: supports up to 4 decimal places (e.g. Rs 0.425 per g)
    private static final Pattern UNIT_SALE_PRICE_PATTERN = Pattern.compile(
            "(?i)(?:Unit\\s*Sale\\s*Price|USP|U\\.?\\s*S\\.?\\s*P\\.?)\\s*[:\\-]?\\s*(?:[₹Rs\\.]*|INR)?\\s*([0-9]+(?:\\.[0-9]{1,4})?\\s*(?:per|/)\\s*[0-9]*\\s*[a-zA-Z]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern UNIT_SALE_PRICE_FALLBACK = Pattern.compile(
            "(?i)(?:Rs\\.?|₹)\\s*([0-9]+(?:\\.[0-9]{1,4})?\\s*(?:per|/)\\s*[0-9]*\\s*(?:g|gm|gms|kg|ml|l|ltr|unit|piece|N))\\b"
    );

    // Net Quantity: matches Net Qty, Net Wt, Net Quantity, Net Weight, Net Volume, Net Contents with g, kg, mg, ml, L, N
    private static final Pattern NET_QUANTITY_PATTERN = Pattern.compile(
            "(?i)(?:Net\\s*(?:Quantity|Qty|Weight|Wt\\.?|Volume|Vol\\.?|Contents|Cont\\.?)?|Weight|Qty)\\s*[:\\-]?\\s*([0-9]+(?:\\.[0-9]+)?\\s*(?:g(?:m|ms|rams)?\\.?|kg(?:s)?\\.?|kilo(?:grams)?|mg(?:s)?\\.?|milligrams?|ml\\.?|millilitres?|l(?:tr|itres?)?\\.?|N|units?))\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern NET_QUANTITY_STANDALONE = Pattern.compile(
            "(?i)(?<!per\\s|/\\s*|every\\s*|approx\\.?\\s*)\\b([0-9]+(?:\\.[0-9]+)?\\s*(?:g(?:m|ms|rams)?|kg(?:s)?|mg|ml|l(?:tr|itres?)?))\\b",
            Pattern.CASE_INSENSITIVE
    );

    // Strict Date Pattern: DD/MM/YYYY, MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD/MM/YY, YYYY-MM-DD
    private static final Pattern STRICT_DATE_PATTERN = Pattern.compile(
            "\\b([0-9]{1,2}[/\\.\\-][0-9]{1,2}[/\\.\\-][0-9]{2,4}|[0-9]{1,2}[/\\.\\-][0-9]{2,4}|[0-9]{4}[\\-][0-9]{2}[\\-][0-9]{2})\\b"
    );

    private static final Pattern RELATIVE_DATE_PATTERN = Pattern.compile(
            "\\b([0-9]+\\s*(?:months?|days?|weeks?|years?)\\s*(?:from|of)\\s*[^\\n,\\r]+)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern MFD_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(?:MFD|MFG|PKD|Pkd|Date\\s*of\\s*(?:Mfg|Manufacture|Packing)|Mfg\\.?\\s*(?:Date|Dt\\.?|on)|Packed\\s*on|Date\\s*of\\s*Pkg)\\b"
    );

    private static final Pattern EXPIRY_KEYWORD_PATTERN = Pattern.compile(
            "(?i)\\b(?:Best\\s*Before|Use\\s*By|Expiry(?:\\s*Date)?|Exp\\.?\\s*(?:Date|Dt\\.?)?|BB)\\b"
    );

    // FSSAI patterns: context + 14 digits OR "Applied For"
    private static final Pattern FSSAI_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?:\\bFSSAI\\b|\\bLic(?:en[sc]e)?\\s*(?:No|Number)?\\.?|\\b(?:Central|State)\\s*Lic(?:en[sc]e)?\\s*(?:No|Number)?\\.?|\\bLic\\.?\\s*(?:en[sc]e)?\\s*(?:No|Number)?\\.?|\\bLic\\.No\\.?|\\bSSA\\s*[il]?\\s*Lic)"
    );

    private static final Pattern FSSAI_APPLIED_FOR_PATTERN = Pattern.compile(
            "(?i)(?:Applied\\s*For|Under\\s*Process|Application\\s*Under\\s*Process|Pending)"
    );

    // Indian Phone Patterns: +91 8888 720 520, +91-8888720520, 8888720520, 08888720520, 1800 111 2233
    private static final Pattern PHONE_KEYWORD_PATTERN = Pattern.compile(
            "(?i)(?:Reach\\s*Us|Contact\\s*Us|Customer\\s*(?:Care|Support|Helpline)|Helpline|Helpdesk|Toll\\s*Free|Consumer\\s*Care|Ph(?:one)?|Tel|Call|Mobile)\\b"
    );

    private static final Pattern INDIAN_PHONE_PATTERN = Pattern.compile(
            "(?:\\+91[\\s\\-]?)?[6-9][0-9]{3}[\\s\\-]?[0-9]{3}[\\s\\-]?[0-9]{3}\\b"
    );

    private static final Pattern INDIAN_PHONE_SOLID_PATTERN = Pattern.compile(
            "(?:\\+91[\\s\\-]?)?[6-9][0-9]{9}\\b"
    );

    private static final Pattern TOLL_FREE_PATTERN = Pattern.compile(
            "\\b(1800[- ]?[0-9]{2,4}[- ]?[0-9]{3,4})\\b"
    );

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "\\b([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})\\b"
    );

    // Batch pattern: Batch : 20250509
    private static final Pattern BATCH_PATTERN = Pattern.compile(
            "(?i)\\b(?:Batch(?:\\s*No\\.?|\\s*Number)?|Lot(?:\\s*No\\.?|\\s*Number)?)\\s*[:\\-]?\\s*([A-Za-z0-9\\-_/]+)\\b"
    );

    // Manufacturer: Manufactured by, Packed by, Mfg by, Produced by, Marketed by
    private static final Pattern MANUFACTURER_PATTERN = Pattern.compile(
            "(?i)(?:(?:Manufactured|Packed|Produced|Marketed)\\s*(?:&|and)?\\s*(?:Marketed|Packed)?\\s*by|Mfg\\.?\\s*by|Packer)\\s*[:\\-]?\\s*([^\\n\\r]*)"
    );

    private static final Pattern IMPORTER_PATTERN = Pattern.compile(
            "(?i)(?:Imported\\s*by|Importer)\\s*[:\\-]?\\s*([^\\n\\r]+)"
    );

    private static final Pattern COUNTRY_OF_ORIGIN_PATTERN = Pattern.compile(
            "(?i)(?:Country\\s*of\\s*Origin|Made\\s*in|Product\\s*of)\\s*[:\\-]?\\s*([A-Za-z\\s]+)(?=[,\\.\\n]|$)"
    );

    /**
     * Extracts structured fields from raw OCR text (backward-compatible signature).
     */
    public StructuredLabelData extract(String rawOcrText) {
        return extract(rawOcrText, Collections.emptyList());
    }

    /**
     * Extracts structured fields using both OCR text and word bounding boxes when available.
     */
    public StructuredLabelData extract(String rawOcrText, List<OcrWord> words) {
        if (rawOcrText == null || rawOcrText.isBlank()) {
            return new StructuredLabelData(
                    null, null, null, null, false, null, null, null,
                    null, null, null, null, null, null, null, null,
                    null, rawOcrText != null ? rawOcrText : "", null, "NOT_DETECTED"
            );
        }

        String[] rawLines = rawOcrText.split("\\R");
        List<String> cleanLines = new ArrayList<>();
        for (String l : rawLines) {
            String t = l.trim();
            if (!t.isEmpty()) {
                cleanLines.add(t);
            }
        }

        // 1. Product Name
        String productName = extractProductName(cleanLines, words);

        // 2. Net Quantity
        String netQuantity = extractNetQuantity(cleanLines);

        // 3. MRP and Tax Status
        MrpResult mrpResult = extractMrp(cleanLines, words);

        // 4. Unit Sale Price
        String unitSalePrice = extractUnitSalePrice(rawOcrText);

        // 5. Dates (strict separation from nutrition and batch)
        String packedDate = extractPackedDate(cleanLines);
        String bestBefore = extractBestBefore(cleanLines);

        // 6. Batch Number
        String batchNumber = extractBatchNumber(cleanLines);

        // 7. FSSAI License or Applied For status
        FssaiResult fssaiResult = extractFssai(cleanLines);

        // 8. Manufacturer & Address
        String manufacturer = extractManufacturer(cleanLines);
        String address = extractAddress(cleanLines);

        // 9. Importer & Origin
        String importer = extractImporter(rawOcrText);
        String countryOfOrigin = extractCountryOfOrigin(rawOcrText, manufacturer);

        // 10. Phone and Email
        String phone = extractPhone(cleanLines, words);
        String email = extractEmail(rawOcrText);

        // 11. Brand
        String brand = extractBrand(cleanLines, manufacturer, productName);

        return new StructuredLabelData(
                productName,
                brand,
                netQuantity,
                mrpResult.mrp,
                mrpResult.inclusiveOfTaxes,
                unitSalePrice,
                manufacturer,
                address,
                importer,
                null,
                countryOfOrigin,
                packedDate,
                bestBefore,
                fssaiResult.licenseNumber,
                phone,
                email,
                null,
                rawOcrText,
                batchNumber,
                fssaiResult.status
        );
    }

    // =========================================================================
    // FIELD-SPECIFIC EXTRACTORS
    // =========================================================================

    /**
     * Extracts Product Name from title-like lines near the top of the label.
     * Normalizes uppercase titles like "APPLE SLICE" to "Apple Slice".
     */
    public String extractProductName(List<String> lines, List<OcrWord> words) {
        // Stopwords / packaging noise to exclude from product name
        Pattern stopwordPattern = Pattern.compile(
                "(?i)\\b(ready\\s*to\\s*cook|online\\s*grocery|coocking|fruits?|vegetables?|best\\s*prices|available\\s*online|marketed\\s*by|packed\\s*by|manufactured\\s*by|nutrition|calories|fat|batch|weight|store|estore|pure|100%|clean|fresh|full\\s*c|vitamin)\\b"
        );

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).replaceAll("[,;:\"'“”’\\-\\.]+$", "").trim();
            if (line.length() >= 3 && line.length() <= 35) {
                if (!stopwordPattern.matcher(line).find() &&
                        !NUTRITION_LINE_PATTERN.matcher(line).find() &&
                        !MRP_KEYWORD_PATTERN.matcher(line).find() &&
                        !MFD_KEYWORD_PATTERN.matcher(line).find() &&
                        !FSSAI_KEYWORD_PATTERN.matcher(line).find()) {
                    // Check if line looks like a title (e.g. "APPLE SLICE")
                    // Reject single-letter tokens like "Full C"
                    String[] tokens = line.split("\\s+");
                    boolean hasSingleChar = false;
                    for (String tok : tokens) {
                        if (tok.length() <= 1) {
                            hasSingleChar = true;
                            break;
                        }
                    }
                    if (!hasSingleChar && line.matches("^[A-Za-z\\s\\-&]+$")) {
                        return toTitleCase(line);
                    }
                }
            }
        }
        return null;
    }

    /**
     * Contextual MRP extractor with Rupee-glyph confusion resolution.
     */
    public MrpResult extractMrp(List<String> lines, List<OcrWord> words) {
        String bestPrice = null;
        boolean inclusiveTaxes = false;
        int highestScore = -1;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (NUTRITION_LINE_PATTERN.matcher(line).find()) {
                continue; // Ignore nutrition table lines
            }

            boolean hasMrpKeyword = MRP_KEYWORD_PATTERN.matcher(line).find();
            if (hasMrpKeyword) {
                // Check if tax inclusion is on this line or next line
                if (INCLUSIVE_TAX_PATTERN.matcher(line).find() ||
                        (i + 1 < lines.size() && INCLUSIVE_TAX_PATTERN.matcher(lines.get(i + 1)).find())) {
                    inclusiveTaxes = true;
                }

                // Look for candidates on this line
                int mrpIndex = findMrpKeywordIndex(line);
                String substring = mrpIndex >= 0 ? line.substring(mrpIndex) : line;

                List<PriceCandidate> candidates = extractPriceCandidates(substring, true, inclusiveTaxes);
                for (PriceCandidate cand : candidates) {
                    if (cand.score > highestScore) {
                        highestScore = cand.score;
                        bestPrice = cand.price;
                    }
                }

                // If no price on same line, check line immediately below
                if (bestPrice == null && i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1);
                    if (!NUTRITION_LINE_PATTERN.matcher(nextLine).find() && !MRP_KEYWORD_PATTERN.matcher(nextLine).find()) {
                        List<PriceCandidate> nextLineCandidates = extractPriceCandidates(nextLine, false, inclusiveTaxes);
                        for (PriceCandidate cand : nextLineCandidates) {
                            if (cand.score > highestScore) {
                                highestScore = cand.score;
                                bestPrice = cand.price;
                            }
                        }
                    }
                }
            }
        }

        // Fallback: search for prices with explicit currency symbols (₹, Rs.)
        if (bestPrice == null) {
            Pattern explicitCurrency = Pattern.compile("(?i)(?:[₹]|Rs\\.?|INR)\\s*([0-9]{1,5}(?:\\.[0-9]{1,2})?)(?:\\s*[/\\\\\\-]+)?");
            for (String line : lines) {
                if (NUTRITION_LINE_PATTERN.matcher(line).find()) continue;
                Matcher m = explicitCurrency.matcher(line);
                if (m.find()) {
                    String p = m.group(1);
                    if (isValidPrice(p)) {
                        bestPrice = cleanNumericPrice(p);
                        if (INCLUSIVE_TAX_PATTERN.matcher(line).find()) {
                            inclusiveTaxes = true;
                        }
                        break;
                    }
                }
            }
        }

        return new MrpResult(bestPrice, inclusiveTaxes);
    }

    private int findMrpKeywordIndex(String line) {
        Matcher m = MRP_KEYWORD_PATTERN.matcher(line);
        if (m.find()) {
            return m.start();
        }
        return -1;
    }

    private List<PriceCandidate> extractPriceCandidates(String text, boolean onSameLine, boolean inclTaxes) {
        List<PriceCandidate> list = new ArrayList<>();
        Matcher m = MRP_NUMERIC_CANDIDATE.matcher(text);

        while (m.find()) {
            String rawNum = m.group(1);
            if (rawNum == null || rawNum.isBlank()) continue;

            String cleaned = rawNum.replaceAll("\\s+", "").replace('O', '0').replace('o', '0')
                    .replace('Q', '0').replace('q', '0');

            if (cleaned.isEmpty()) continue;

            // Handle Rupee-sign OCR glyph confusion (e.g. ₹150 recognized as 2150)
            if (cleaned.length() == 4 && cleaned.startsWith("2")) {
                String potentialPrice = cleaned.substring(1); // 2150 -> 150
                if (isValidPrice(potentialPrice)) {
                    int score = (onSameLine ? 65 : 35) + (inclTaxes ? 25 : 0) + 15;
                    list.add(new PriceCandidate(potentialPrice, score));
                }
            }

            if (isValidPrice(cleaned)) {
                int score = (onSameLine ? 50 : 25) + (inclTaxes ? 20 : 0);
                // Boost for explicitly recognized currency symbols (~, z, <, ₹, Rs)
                if (text.contains("~") || text.contains("z") || text.contains("<") || text.contains("₹") || text.toLowerCase().contains("rs")) {
                    score += 25;
                }
                // Penalty for unusually large prices on everyday packaged items if a smaller candidate exists
                try {
                    double val = Double.parseDouble(cleaned);
                    if (val > 1000.0 && cleaned.startsWith("2")) {
                        score -= 30; // penalize likely Rupee confusion
                    }
                } catch (Exception ignored) {}

                list.add(new PriceCandidate(cleanNumericPrice(cleaned), score));
            }
        }

        return list;
    }

    private boolean isValidPrice(String s) {
        try {
            double val = Double.parseDouble(s);
            return val >= 0.10 && val <= 99999.00;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Strict Date Extractor for Manufacturing/Packing Date.
     * Prevents nutrition text like "Protein 0.5 Gms" from ever being treated as a date.
     */
    public String extractPackedDate(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (NUTRITION_LINE_PATTERN.matcher(line).find()) {
                // If the line contains a nutrition keyword, only match if it ALSO contains Packed On
                if (!MFD_KEYWORD_PATTERN.matcher(line).find()) {
                    continue;
                }
            }

            if (MFD_KEYWORD_PATTERN.matcher(line).find()) {
                // 1. Check same line for strict date
                String sameLineDate = findStrictDate(line);
                if (sameLineDate != null) {
                    return sameLineDate;
                }

                // 2. Scan next 1-3 lines
                for (int j = 1; j <= 3 && (i + j) < lines.size(); j++) {
                    String nextLine = lines.get(i + j);
                    if (NUTRITION_LINE_PATTERN.matcher(nextLine).find() && !STRICT_DATE_PATTERN.matcher(nextLine).find()) {
                        continue; // skip nutrition-only line
                    }
                    if (EXPIRY_KEYWORD_PATTERN.matcher(nextLine).find()) {
                        break; // hit Best Before heading
                    }
                    String nextDate = findStrictDate(nextLine);
                    if (nextDate != null) {
                        return nextDate;
                    }
                }
            }
        }

        // Fallback: check relative date pattern (e.g. "12 months from packing")
        for (String line : lines) {
            if (NUTRITION_LINE_PATTERN.matcher(line).find()) continue;
            Matcher rm = RELATIVE_DATE_PATTERN.matcher(line);
            if (rm.find()) {
                return rm.group(1).trim();
            }
        }

        return null;
    }

    /**
     * Strict Date Extractor for Best Before / Expiry Date.
     */
    public String extractBestBefore(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (NUTRITION_LINE_PATTERN.matcher(line).find() && !EXPIRY_KEYWORD_PATTERN.matcher(line).find()) {
                continue;
            }

            if (EXPIRY_KEYWORD_PATTERN.matcher(line).find()) {
                // 1. Check same line for strict numeric date
                String sameLineDate = findStrictDate(line);
                if (sameLineDate != null) {
                    return sameLineDate;
                }

                // 2. Check same line for relative date (e.g. "12 months from packing")
                Matcher rm = RELATIVE_DATE_PATTERN.matcher(line);
                if (rm.find()) {
                    return rm.group(1).trim();
                }

                // 3. Scan next 1-3 lines
                for (int j = 1; j <= 3 && (i + j) < lines.size(); j++) {
                    String nextLine = lines.get(i + j);
                    if (NUTRITION_LINE_PATTERN.matcher(nextLine).find() && !STRICT_DATE_PATTERN.matcher(nextLine).find()) {
                        continue;
                    }
                    if (MFD_KEYWORD_PATTERN.matcher(nextLine).find()) {
                        break;
                    }
                    String nextDate = findStrictDate(nextLine);
                    if (nextDate != null) {
                        return nextDate;
                    }
                    Matcher nrm = RELATIVE_DATE_PATTERN.matcher(nextLine);
                    if (nrm.find()) {
                        return nrm.group(1).trim();
                    }
                }
            }
        }

        // Fallback: check relative date pattern
        for (String line : lines) {
            if (NUTRITION_LINE_PATTERN.matcher(line).find()) continue;
            Matcher rm = RELATIVE_DATE_PATTERN.matcher(line);
            if (rm.find()) {
                return rm.group(1).trim();
            }
        }

        return null;
    }

    private String findStrictDate(String text) {
        Matcher m = STRICT_DATE_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    /**
     * Contextual FSSAI extractor recognizing both 14-digit numbers and textual states like "Applied For".
     */
    public FssaiResult extractFssai(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (FSSAI_KEYWORD_PATTERN.matcher(line).find()) {
                // Check if "Applied For" is present on this line or the next line
                if (FSSAI_APPLIED_FOR_PATTERN.matcher(line).find() ||
                        (i + 1 < lines.size() && FSSAI_APPLIED_FOR_PATTERN.matcher(lines.get(i + 1)).find())) {
                    return new FssaiResult(null, "APPLIED_FOR");
                }

                // Search for 14 numeric digits in this line or next line
                String combined = line + (i + 1 < lines.size() ? " " + lines.get(i + 1) : "");
                String digits = extract14Digits(combined);
                if (digits != null) {
                    return new FssaiResult(digits, "NUMBER_DETECTED");
                }

                return new FssaiResult(null, "TEXT_PRESENT_NUMBER_NOT_DETECTED");
            }
        }

        return new FssaiResult(null, "NOT_DETECTED");
    }

    private String extract14Digits(String text) {
        StringBuilder sb = new StringBuilder();
        boolean inSequence = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c)) {
                sb.append(c);
                inSequence = true;
                if (sb.length() == 14) {
                    // Check boundary: next character should not be a digit
                    if (i + 1 == text.length() || !Character.isDigit(text.charAt(i + 1))) {
                        return sb.toString();
                    }
                }
            } else if (inSequence && (c == ' ' || c == '-' || c == '/')) {
                // Allow cluster separators (e.g. 100 210 110 00456)
            } else {
                sb.setLength(0);
                inSequence = false;
            }
        }
        return null;
    }

    /**
     * Flexible Indian Phone Extractor supporting spaced formats like +91 8888 720 520.
     */
    public String extractPhone(List<String> lines, List<OcrWord> words) {
        // First try searching near phone keywords
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (PHONE_KEYWORD_PATTERN.matcher(line).find()) {
                String candidate = findIndianPhone(line);
                if (candidate != null) {
                    return candidate;
                }
                if (i + 1 < lines.size()) {
                    candidate = findIndianPhone(lines.get(i + 1));
                    if (candidate != null) {
                        return candidate;
                    }
                }
            }
        }

        // Second pass: scan full text
        for (String line : lines) {
            String candidate = findIndianPhone(line);
            if (candidate != null) {
                return candidate;
            }
        }

        return null;
    }

    private String findIndianPhone(String text) {
        // 1. Spaced Indian format: +91 8888 720 520 or 8888 720 520
        Matcher m1 = INDIAN_PHONE_PATTERN.matcher(text);
        if (m1.find()) {
            String match = m1.group().trim();
            if (isPlausiblePhone(match)) {
                return match;
            }
        }

        // 2. Solid Indian format: +918888720520 or 8888720520
        Matcher m2 = INDIAN_PHONE_SOLID_PATTERN.matcher(text);
        if (m2.find()) {
            String match = m2.group().trim();
            if (isPlausiblePhone(match)) {
                return match;
            }
        }

        // 3. Toll-free format: 1800-111-2233
        Matcher m3 = TOLL_FREE_PATTERN.matcher(text);
        if (m3.find()) {
            return m3.group(1).trim();
        }

        return null;
    }

    private boolean isPlausiblePhone(String s) {
        String digits = s.replaceAll("[^0-9]", "");
        // Valid Indian phone has 10 digits (or 12 with 91 prefix)
        return digits.length() == 10 || (digits.length() == 12 && digits.startsWith("91"));
    }

    /**
     * Extracts Batch Number (e.g. "20250509").
     */
    public String extractBatchNumber(List<String> lines) {
        for (String line : lines) {
            Matcher m = BATCH_PATTERN.matcher(line);
            if (m.find()) {
                return m.group(1).trim();
            }
        }
        return null;
    }

    /**
     * Extracts Manufacturer / Packer Name.
     */
    public String extractManufacturer(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Matcher m = MANUFACTURER_PATTERN.matcher(line);
            if (m.find()) {
                String candidate = m.group(1).trim();
                // Strip trailing noise like MRP or address
                candidate = candidate.replaceAll("(?i)\\s+(?:MRP|M\\.R\\.P\\.|Batch|Net|Pkd|Packed|Email|Ph).*$", "").trim();
                if (candidate.length() >= 2 && !NUTRITION_LINE_PATTERN.matcher(candidate).find()) {
                    return candidate;
                }
                // If "Packed By :" has no name on the same line, inspect the next line
                if (candidate.isEmpty() && i + 1 < lines.size()) {
                    String nextLine = lines.get(i + 1).trim();
                    nextLine = nextLine.replaceAll("(?i)\\s+(?:MRP|M\\.R\\.P\\.|Batch|Net|Pkd|Packed|Email|Ph).*$", "").trim();
                    if (nextLine.length() >= 2 && !NUTRITION_LINE_PATTERN.matcher(nextLine).find()) {
                        return nextLine;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Extracts Manufacturer / Packer Address.
     */
    public String extractAddress(List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (MANUFACTURER_PATTERN.matcher(line).find()) {
                // Collect subsequent address lines
                List<String> addrParts = new ArrayList<>();
                int startOffset = 1;
                // If manufacturer name is on line i + 1, address starts on line i + 2
                if (i + 1 < lines.size()) {
                    String nextL = lines.get(i + 1).trim();
                    if (nextL.length() <= 30 && !nextL.toLowerCase().matches(".*\\b(road|marg|street|near|building|villa|floor|virar|mumbai|dist|district|pin|state)\\b.*")) {
                        startOffset = 2;
                    }
                }

                for (int j = i + startOffset; j < lines.size(); j++) {
                    String nextLine = lines.get(j);
                    if (nextLine.toLowerCase().contains("marketed by") ||
                            nextLine.toLowerCase().contains("reach us") ||
                            nextLine.toLowerCase().contains("email") ||
                            nextLine.toLowerCase().contains("batch")) {
                        break;
                    }
                    if (NUTRITION_LINE_PATTERN.matcher(nextLine).find()) {
                        continue;
                    }
                    // Strip interleaved MRP / tax noise
                    String cleaned = nextLine.replaceAll("(?i)\\s*\\(?(?:incl|inclusive|tees|taxes)[^)]*\\)?", "")
                            .replaceAll("(?i)MRP\\s*:[^,]+,?", "")
                            .replaceAll("(?i)[@:;~]+$", "")
                            .replaceAll("(?i)^[@:;~]+", "").trim();
                    if (!cleaned.isEmpty()) {
                        addrParts.add(cleaned);
                    }
                }
                if (!addrParts.isEmpty()) {
                    return String.join(" ", addrParts).replaceAll("\\s+", " ").trim();
                }
            }
        }
        return null;
    }

    /**
     * Extracts Net Quantity.
     */
    public String extractNetQuantity(List<String> lines) {
        for (String line : lines) {
            if (NUTRITION_LINE_PATTERN.matcher(line).find() && !line.toLowerCase().contains("net")) {
                continue; // ignore standalone nutrition units like "FAT 0.3 Gms"
            }
            Matcher m = NET_QUANTITY_PATTERN.matcher(line);
            if (m.find()) {
                return normalizeQuantity(m.group(1).trim());
            }
        }

        // Standalone fallback: only if not nutrition
        for (String line : lines) {
            if (NUTRITION_LINE_PATTERN.matcher(line).find()) continue;
            Matcher sm = NET_QUANTITY_STANDALONE.matcher(line);
            if (sm.find()) {
                return normalizeQuantity(sm.group(1).trim());
            }
        }

        return null;
    }

    private String normalizeQuantity(String raw) {
        return raw.trim();
    }

    public String extractEmail(String text) {
        Matcher m = EMAIL_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public String extractUnitSalePrice(String text) {
        Matcher m = UNIT_SALE_PRICE_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        Matcher fm = UNIT_SALE_PRICE_FALLBACK.matcher(text);
        if (fm.find()) {
            return fm.group(1).trim();
        }
        return null;
    }

    public String extractCountryOfOrigin(String text, String manufacturer) {
        Matcher m = COUNTRY_OF_ORIGIN_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        if (text.toLowerCase().contains("india") || (manufacturer != null && !manufacturer.isBlank())) {
            return "Domestic (Substantiated by Manufacturer)";
        }
        return null;
    }

    public String extractBrand(List<String> lines, String manufacturer, String productName) {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.toLowerCase().contains("marketed by")) {
                String after = line.substring(line.toLowerCase().indexOf("marketed by") + "marketed by".length())
                        .replaceAll("[:\\-]+", "").trim();
                if (!after.isEmpty() && after.length() >= 3 && after.length() <= 30 && after.matches(".*[A-Za-z]{3,}.*") && !after.matches("^(.)\\1+$")) {
                    return after;
                }
                if (i + 1 < lines.size()) {
                    String nextL = lines.get(i + 1).replaceAll("[:\\-]+", "").trim();
                    if (!nextL.isEmpty() && nextL.length() >= 3 && nextL.length() <= 35 && nextL.matches(".*[A-Za-z]{3,}.*") && !nextL.matches("^(.)\\1+$")) {
                        return nextL;
                    }
                }
            }
        }
        if (manufacturer != null && !manufacturer.isBlank()) {
            return manufacturer;
        }
        return null;
    }

    private String extractImporter(String text) {
        Matcher m = IMPORTER_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(1).trim();
        }
        return null;
    }

    public String cleanNumericPrice(String priceStr) {
        if (priceStr == null) return null;
        String cleaned = priceStr.replaceAll("[^0-9.]", "").trim();
        try {
            double val = Double.parseDouble(cleaned);
            if (cleaned.contains(".")) {
                return String.format(java.util.Locale.US, "%.2f", val);
            }
            return String.valueOf((long) val);
        } catch (Exception e) {
            return cleaned;
        }
    }

    private String toTitleCase(String input) {
        if (input == null || input.isEmpty()) return input;
        StringBuilder sb = new StringBuilder();
        boolean nextTitle = true;
        for (char c : input.toCharArray()) {
            if (Character.isWhitespace(c) || c == '-' || c == '/') {
                nextTitle = true;
                sb.append(c);
            } else if (nextTitle) {
                sb.append(Character.toTitleCase(c));
                nextTitle = false;
            } else {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }

    // Records for internal candidate scoring
    public record MrpResult(String mrp, boolean inclusiveOfTaxes) {}
    public record FssaiResult(String licenseNumber, String status) {}
    private record PriceCandidate(String price, int score) {}
}

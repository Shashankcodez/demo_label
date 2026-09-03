package com.labelcheck.service;

import com.labelcheck.dto.StructuredLabelData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LabelExtractionServiceTest {

    private LabelExtractionService service;

    @BeforeEach
    void setUp() {
        service = new LabelExtractionService();
    }

    @Test
    @DisplayName("A. Extract MRP accurately from various common Indian currency formats with tax indication")
    void extractMrp_variousFormats() {
        StructuredLabelData d1 = service.extract("MRP Rs 50.00 (incl. of all taxes)");
        assertThat(d1.mrp()).isEqualTo("50.00");
        assertThat(d1.mrpInclusiveOfTaxes()).isTrue();

        StructuredLabelData d2 = service.extract("M.R.P. ₹ 149.50");
        assertThat(d2.mrp()).isEqualTo("149.50");
        assertThat(d2.mrpInclusiveOfTaxes()).isFalse();

        StructuredLabelData d3 = service.extract("Maximum Retail Price: Rs 99 inclusive of all taxes");
        assertThat(d3.mrp()).isEqualTo("99");
        assertThat(d3.mrpInclusiveOfTaxes()).isTrue();
    }

    @Test
    @DisplayName("B. Extract Unit Sale Price (USP) per gram, kg, or ml")
    void extractUnitSalePrice() {
        StructuredLabelData d1 = service.extract("MRP Rs 100.00\nUnit Sale Price: Rs 0.50 per g");
        assertThat(d1.unitSalePrice()).isEqualTo("0.50 per g");

        StructuredLabelData d2 = service.extract("USP ₹ 250.00 / kg");
        assertThat(d2.unitSalePrice()).isEqualTo("250.00 / kg");
    }

    @Test
    @DisplayName("C. Extract Net Quantity with metric units (g, kg, ml, L)")
    void extractNetQuantity_variousUnits() {
        StructuredLabelData d1 = service.extract("Net Quantity: 150 g");
        assertThat(d1.netQuantity()).isEqualTo("150 g");

        StructuredLabelData d2 = service.extract("Net Wt 500g");
        assertThat(d2.netQuantity()).isEqualTo("500g");

        StructuredLabelData d3 = service.extract("Net Volume: 1 L");
        assertThat(d3.netQuantity()).isEqualTo("1 L");

        StructuredLabelData d4 = service.extract("Net Contents 750 ml");
        assertThat(d4.netQuantity()).isEqualTo("750 ml");
    }

    @Test
    @DisplayName("D. Extract exact 14-digit FSSAI license numbers")
    void extractFssaiLicense() {
        StructuredLabelData d1 = service.extract("FSSAI Lic. No. 10012345678901");
        assertThat(d1.fssaiLicenseNumber()).isEqualTo("10012345678901");

        StructuredLabelData d2 = service.extract("Licence No. 12221999000123");
        assertThat(d2.fssaiLicenseNumber()).isEqualTo("12221999000123");
    }

    @Test
    @DisplayName("E. Extract manufacturing and expiry date markers")
    void extractDateMarkings() {
        StructuredLabelData d1 = service.extract("PKD: 08/2026\nBest Before: 12 months from packing");
        assertThat(d1.manufactureOrPackingDate()).isEqualTo("08/2026");
        assertThat(d1.bestBeforeOrExpiry()).contains("12 months");

        StructuredLabelData d2 = service.extract("MFD: 15/09/2026\nExpiry: 14/09/2027");
        assertThat(d2.manufactureOrPackingDate()).isEqualTo("15/09/2026");
        assertThat(d2.bestBeforeOrExpiry()).contains("14/09/2027");
    }

    @Test
    @DisplayName("F. Extract consumer care telephone and email contacts")
    void extractConsumerCareContacts() {
        String text = "Customer Helpline: 1800-123-4567\nFeedback email: support@abcfoods.in";
        StructuredLabelData data = service.extract(text);

        assertThat(data.customerCarePhone()).isEqualTo("1800-123-4567");
        assertThat(data.customerCareEmail()).isEqualTo("support@abcfoods.in");
    }

    @Test
    @DisplayName("G. Extract manufacturer and importer information")
    void extractManufacturerAndImporter() {
        String text = "Manufactured by ABC Confectioneries Pvt Ltd, Plot 4, Mumbai";
        StructuredLabelData data = service.extract(text);

        assertThat(data.manufacturerName()).contains("ABC Confectioneries");
    }

    @Test
    @DisplayName("H. Missing fields remain null and are never invented")
    void missingFields_remainNull() {
        String text = "Random promotional slogan without statutory markers";
        StructuredLabelData data = service.extract(text);

        assertThat(data.mrp()).isNull();
        assertThat(data.mrpInclusiveOfTaxes()).isFalse();
        assertThat(data.unitSalePrice()).isNull();
        assertThat(data.netQuantity()).isNull();
        assertThat(data.fssaiLicenseNumber()).isNull();
        assertThat(data.manufactureOrPackingDate()).isNull();
        assertThat(data.bestBeforeOrExpiry()).isNull();
        assertThat(data.customerCarePhone()).isNull();
        assertThat(data.customerCareEmail()).isNull();
        assertThat(data.manufacturerName()).isNull();
        assertThat(data.rawOcrText()).isEqualTo(text);
    }

    @Test
    @DisplayName("I. Extract FSSAI with space or hyphen separated digit clusters from OCR")
    void extractFssai_spacedOrHyphenatedDigits() {
        StructuredLabelData d1 = service.extract("FSSAI Lic. No. 100 210 1100 0456");
        assertThat(d1.fssaiLicenseNumber()).isEqualTo("10021011000456");

        StructuredLabelData d2 = service.extract("Lic. No.: 100-21011-000456");
        assertThat(d2.fssaiLicenseNumber()).isEqualTo("10021011000456");

        StructuredLabelData d3 = service.extract("FSSAI: 10012011 000456");
        assertThat(d3.fssaiLicenseNumber()).isEqualTo("10012011000456");
    }

    @Test
    @DisplayName("J. Extract real-world MRP variations with slash-dash and tax clauses")
    void extractMrp_additionalRealWorldVariations() {
        StructuredLabelData d1 = service.extract("MRP: ₹50/-");
        assertThat(d1.mrp()).isEqualTo("50");

        StructuredLabelData d2 = service.extract("M.R.P. : Rs. 50.00");
        assertThat(d2.mrp()).isEqualTo("50.00");

        StructuredLabelData d3 = service.extract("MRP (incl. of all taxes) : Rs 50");
        assertThat(d3.mrp()).isEqualTo("50");
        assertThat(d3.mrpInclusiveOfTaxes()).isTrue();
    }

    @Test
    @DisplayName("K. Extract net quantity with tight spacing (150g, 500ml, 1L)")
    void extractNetQuantity_tightSpacing() {
        StructuredLabelData d1 = service.extract("Net Quantity 150 g");
        assertThat(d1.netQuantity()).isEqualTo("150 g");

        StructuredLabelData d2 = service.extract("Net Weight: 150g");
        assertThat(d2.netQuantity()).isEqualTo("150g");

        StructuredLabelData d3 = service.extract("Net Qty 500ml");
        assertThat(d3.netQuantity()).isEqualTo("500ml");

        StructuredLabelData d4 = service.extract("Net Vol 1 L");
        assertThat(d4.netQuantity()).isEqualTo("1 L");
    }

    @Test
    @DisplayName("L. Extract manufacturer with multi-role packaging markers")
    void extractManufacturer_multiRoleMarkers() {
        StructuredLabelData d1 = service.extract("Manufactured & Marketed by Tasty Treats India Pvt Ltd, Mumbai\nFSSAI Lic. No. 10012345678901");
        assertThat(d1.manufacturerName()).isEqualTo("Tasty Treats India Pvt Ltd, Mumbai");

        StructuredLabelData d2 = service.extract("Packed by Organic Farms Co-op, Pune");
        assertThat(d2.manufacturerName()).isEqualTo("Organic Farms Co-op, Pune");
    }

    @Test
    @DisplayName("M. Extract Helpdesk and short toll-free helpline numbers")
    void extractConsumerPhone_helpdeskFormat() {
        StructuredLabelData d1 = service.extract("Helpdesk: 1800-11-4000");
        assertThat(d1.customerCarePhone()).isEqualTo("1800-11-4000");
    }

    @Test
    @DisplayName("N. OCR noise in MRP: letter O/Q confusions, slash-dash, and multi-line formats")
    void extractMrp_ocrNoiseAndLetterConfusions() {
        // Letter O for zero
        StructuredLabelData d1 = service.extract("MRP Rs 5O");
        assertThat(d1.mrp()).isEqualTo("50");

        // Letter O in decimal
        StructuredLabelData d2 = service.extract("M.R.P Rs 5O.OO");
        assertThat(d2.mrp()).isEqualTo("50.00");

        // Letter Q for zero
        StructuredLabelData d3 = service.extract("MRP Rs 5Q");
        assertThat(d3.mrp()).isEqualTo("50");

        // Equals and slash-dash
        StructuredLabelData d4 = service.extract("MRP = Rs 50/-");
        assertThat(d4.mrp()).isEqualTo("50");

        // Space inside M RP
        StructuredLabelData d5 = service.extract("M RP Rs 50");
        assertThat(d5.mrp()).isEqualTo("50");

        // Multi-line table layout
        StructuredLabelData d6 = service.extract("MRP (incl. of all taxes)\nRs. 75.00\nNet Weight: 200g");
        assertThat(d6.mrp()).isEqualTo("75.00");
        assertThat(d6.mrpInclusiveOfTaxes()).isTrue();
    }

    @Test
    @DisplayName("O. Unit Sale Price supports 3 and 4 decimal places")
    void extractUnitSalePrice_extendedDecimals() {
        StructuredLabelData d1 = service.extract("Unit Sale Price: Rs 0.425 per g");
        assertThat(d1.unitSalePrice()).isEqualTo("0.425 per g");

        StructuredLabelData d2 = service.extract("USP: Rs 0.0833 / ml");
        assertThat(d2.unitSalePrice()).isEqualTo("0.0833 / ml");
    }

    @Test
    @DisplayName("P. False-positive defense: Batch, phone, or barcode digits MUST NOT be extracted as FSSAI without keyword")
    void falsePositiveDefense_fssaiContextRequired() {
        // 14-digit batch number starting with 1
        StructuredLabelData d1 = service.extract("Batch No: 12345678901234\nNet Qty: 200 g");
        assertThat(d1.fssaiLicenseNumber()).isNull();

        // 14-digit sequence in phone or contact
        StructuredLabelData d2 = service.extract("Phone: 18001234567890\nMRP: Rs 50");
        assertThat(d2.fssaiLicenseNumber()).isNull();

        // Barcode digits
        StructuredLabelData d3 = service.extract("Barcode: 10012345678901");
        assertThat(d3.fssaiLicenseNumber()).isNull();
    }

    @Test
    @DisplayName("Q. False-positive defense: Nutrition table values MUST NOT be extracted as Net Quantity")
    void falsePositiveDefense_nutritionTableShield() {
        String nutritionText = """
                Nutritional Information (per 100g):
                Energy: 450 kcal
                Protein: 8.5g
                Carbohydrate: 65g
                Total Fat: 18g
                Sodium: 50mg
                """;
        StructuredLabelData data = service.extract(nutritionText);
        assertThat(data.netQuantity()).isNull();
    }

    @Test
    @DisplayName("R. Net Quantity supports mg, kg, g, ml, L with varied spacing")
    void extractNetQuantity_mgAndAdditionalUnits() {
        StructuredLabelData d1 = service.extract("Net Quantity: 100 mg");
        assertThat(d1.netQuantity()).isEqualTo("100 mg");

        StructuredLabelData d2 = service.extract("Net Wt 1.5 kg");
        assertThat(d2.netQuantity()).isEqualTo("1.5 kg");

        StructuredLabelData d3 = service.extract("Net Contents: 1L");
        assertThat(d3.netQuantity()).isEqualTo("1L");
    }

    @Test
    @DisplayName("S. Robust date extraction preserves month/year without inventing days")
    void extractDate_variousFormats() {
        StructuredLabelData d1 = service.extract("MFD 08/2026\nUse By 12/2026");
        assertThat(d1.manufactureOrPackingDate()).isEqualTo("08/2026");
        assertThat(d1.bestBeforeOrExpiry()).isEqualTo("12/2026");

        StructuredLabelData d2 = service.extract("MFG: 15-08-2026\nExpiry: 14-08-2027");
        assertThat(d2.manufactureOrPackingDate()).isEqualTo("15-08-2026");
        assertThat(d2.bestBeforeOrExpiry()).isEqualTo("14-08-2027");

        StructuredLabelData d3 = service.extract("PKD: 08/26\nBest Before 6 Months from packaging");
        assertThat(d3.manufactureOrPackingDate()).isEqualTo("08/26");
        assertThat(d3.bestBeforeOrExpiry()).contains("6 Months from packaging");
    }

    @Test
    @DisplayName("T. Mixed Hindi and English statutory declarations")
    void extract_mixedHindiAndEnglish() {
        StructuredLabelData data = service.extract("MRP ₹ 120.00 सभी करों सहित\nNet Qty: 500 g");
        assertThat(data.mrp()).isEqualTo("120.00");
        assertThat(data.mrpInclusiveOfTaxes()).isTrue();
        assertThat(data.netQuantity()).isEqualTo("500 g");
    }

    @Test
    @DisplayName("U. Customer Helpline phone is not confused with MRP or FSSAI")
    void falsePositiveDefense_helplinePhoneSeparation() {
        String text = """
                Batch No: 99887766554433
                Customer Helpline: 1800-111-2233
                Net Weight: 500 g
                MRP: ₹80
                """;
        StructuredLabelData data = service.extract(text);
        assertThat(data.customerCarePhone()).isEqualTo("1800-111-2233");
        assertThat(data.mrp()).isEqualTo("80");
        assertThat(data.netQuantity()).isEqualTo("500 g");
        assertThat(data.fssaiLicenseNumber()).isNull();
    }
}



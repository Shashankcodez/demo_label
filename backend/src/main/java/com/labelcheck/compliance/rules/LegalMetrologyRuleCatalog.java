package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.model.RegulationFamily;
import com.labelcheck.compliance.model.RuleMetadata;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central authoritative catalog of statutory packaging rules under:
 * - Legal Metrology Act, 2009 (Act 1 of 2010)
 * - Legal Metrology (Packaged Commodities) Rules, 2011 (as amended 2017, 2021, 2022)
 * - Food Safety and Standards (Labelling and Display) Regulations, 2020
 */
public final class LegalMetrologyRuleCatalog {

    public static final String ENGINE_VERSION = "LM-PCR-2026.01";

    private static final Map<String, RuleMetadata> REGISTRY = new LinkedHashMap<>();

    static {
        register(RuleMetadata.builder("RULE_MRP")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(e)")
                .title("Maximum Retail Price (MRP) Declaration")
                .description("Mandatory retail sale price declaration inclusive of all taxes in rupees.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(e)")
                .applicability("All pre-packaged commodities intended for retail sale")
                .build());

        register(RuleMetadata.builder("RULE_NET_QTY")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(c) & Rule 12")
                .title("Net Quantity & Standard Units Declaration")
                .description("Net quantity declared in standard units of weight, measure or number.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(c), Rule 12, Schedule I")
                .applicability("All pre-packaged commodities (exemptions under Rule 26)")
                .build());

        register(RuleMetadata.builder("RULE_USP")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(e) Second Proviso")
                .title("Unit Sale Price (USP) Declaration")
                .description("Unit sale price in rupees per g/kg/ml/l/metre/number where net quantity exceeds 1kg/1litre or is packaged in multiple items.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(e) Second Proviso (Amend. 2021)")
                .applicability("Retail packages containing net quantity > 1 kg, > 1 L, or items sold by number/piece")
                .build());

        register(RuleMetadata.builder("RULE_MANUFACTURER")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(a)")
                .title("Manufacturer / Packer / Importer Identity & Complete Address")
                .description("Name and complete address of the manufacturer, or packer, or importer.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(a)")
                .applicability("All pre-packaged commodities")
                .build());

        register(RuleMetadata.builder("RULE_ORIGIN")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(a) & Rule 6(10)")
                .title("Country of Origin Declaration")
                .description("Country of origin or manufacture/assembly for all commodities, strictly mandatory for imported goods.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(a) & Rule 6(10)")
                .applicability("All pre-packaged commodities; heightened scrutiny for imported goods")
                .build());

        register(RuleMetadata.builder("RULE_DATE")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(d)")
                .title("Month and Year of Manufacture / Packing / Import")
                .description("Month and year of manufacture, packing, or import legibly declared.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(d)")
                .applicability("All pre-packaged commodities")
                .build());

        register(RuleMetadata.builder("RULE_CONSUMER_CARE")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(da)")
                .title("Consumer Care Grievance Redressal Mechanism")
                .description("Contact details of official or consumer cell: name, address, telephone, email.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(da)")
                .applicability("All retail pre-packaged commodities")
                .build());

        register(RuleMetadata.builder("RULE_FSSAI")
                .family(RegulationFamily.FOOD_LABELING)
                .ruleNumber("Section 31 & FSS Reg 2020")
                .title("FSSAI License / Registration Display & Logo")
                .description("14-digit FSSAI license/registration number and logo on food commodity labels.")
                .authority("Food Safety and Standards Authority of India")
                .document("Food Safety and Standards (Labelling and Display) Regulations, 2020")
                .reference("Food Safety and Standards Act, 2006, Sec 31; FSS (Labelling and Display) Regulations, 2020")
                .applicability("All food and beverage products packaged for sale in India")
                .build());

        register(RuleMetadata.builder("RULE_GENERIC_NAME")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 6(1)(b) & Rule 6(1)(l)")
                .title("Common or Generic Name of Commodity")
                .description("Common or generic name of commodity contained in the package (brand alone is insufficient).")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(b) & Rule 6(1)(l)")
                .applicability("All pre-packaged commodities")
                .build());

        register(RuleMetadata.builder("RULE_QUANTITY_QUALIFIER")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 13(5)")
                .title("Prohibition of Misleading Quantity Qualifiers")
                .description("Prohibits qualifying words such as 'when packed', 'approximate', 'minimum', or 'not less than' with net quantity.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 13(5)")
                .applicability("All pre-packaged commodities declaring net quantity")
                .build());

        register(RuleMetadata.builder("RULE_FONT_SIZE")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 7 & Schedule II")
                .title("Minimum Numerals and Letters Height (Schedule II)")
                .description("Mandatory minimum height of numerals and letters based on Principal Display Panel area and net quantity.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 7 & Schedule II")
                .applicability("All declarations on pre-packaged commodities (requires physical scale verification)")
                .build());

        register(RuleMetadata.builder("RULE_LEGIBILITY")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 9")
                .title("General Display, Legibility, and Prominence Standards")
                .description("Declarations must be legible, prominent, definite, and conspicuous with adequate visual contrast.")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 9")
                .applicability("All packaging declarations")
                .build());

        register(RuleMetadata.builder("RULE_EXEMPTION")
                .family(RegulationFamily.LEGAL_METROLOGY)
                .ruleNumber("Rule 26")
                .title("Statutory Scope and Exemption Verification")
                .description("Identifies statutory exemptions (packages <= 10g/10ml, industrial/institutional consumer packages, agricultural packages > 50kg).")
                .reference("Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 26")
                .applicability("Packages claiming statutory exemption from retail packaging rules")
                .build());
    }

    private static void register(RuleMetadata metadata) {
        REGISTRY.put(metadata.ruleId(), metadata);
    }

    public static RuleMetadata get(String ruleId) {
        return REGISTRY.get(ruleId);
    }

    public static Map<String, RuleMetadata> getAll() {
        return Collections.unmodifiableMap(REGISTRY);
    }

    private LegalMetrologyRuleCatalog() {
    }
}

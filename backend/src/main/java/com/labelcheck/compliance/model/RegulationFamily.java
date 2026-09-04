package com.labelcheck.compliance.model;

/**
 * Statutory regulatory families for packaging declarations.
 * Ensures Legal Metrology requirements are cleanly separated from Food Safety (FSSAI)
 * and other sectoral regulations.
 */
public enum RegulationFamily {
    /**
     * Legal Metrology Act, 2009 and Legal Metrology (Packaged Commodities) Rules, 2011.
     */
    LEGAL_METROLOGY,

    /**
     * Food Safety and Standards Act, 2006 and FSSAI (Labelling and Display) Regulations, 2020.
     */
    FOOD_LABELING,

    /**
     * Other sectoral packaging mandates (e.g. Drugs and Cosmetics, Electronics).
     */
    OTHER_SECTORAL
}

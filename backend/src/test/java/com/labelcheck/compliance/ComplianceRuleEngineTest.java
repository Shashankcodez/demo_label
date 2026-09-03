package com.labelcheck.compliance;

import com.labelcheck.compliance.rules.ConsumerCareRule;
import com.labelcheck.compliance.rules.DateMarkingRule;
import com.labelcheck.compliance.rules.FssaiLicenseRule;
import com.labelcheck.compliance.rules.ManufacturerRule;
import com.labelcheck.compliance.rules.MrpRule;
import com.labelcheck.compliance.rules.NetQuantityRule;
import com.labelcheck.compliance.rules.OriginRule;
import com.labelcheck.compliance.rules.UnitSalePriceRule;
import com.labelcheck.dto.StructuredLabelData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComplianceRuleEngineTest {

    private ComplianceRuleEngine engine;

    @BeforeEach
    void setUp() {
        List<ComplianceRule> rules = List.of(
                new MrpRule(),
                new UnitSalePriceRule(),
                new NetQuantityRule(),
                new FssaiLicenseRule(),
                new DateMarkingRule(),
                new ManufacturerRule(),
                new ConsumerCareRule(),
                new OriginRule()
        );
        engine = new ComplianceRuleEngine(rules);
    }

    @Test
    @DisplayName("A. Fully compliant structured label with all statutory declarations produces PASS status and score 100")
    void evaluate_fullyCompliantLabel_producesPass() {
        StructuredLabelData label = new StructuredLabelData(
                "Masala Biscuits",
                "CrispyTreats",
                "150 g",
                "50.00",
                true, // mrpInclusiveOfTaxes
                "Rs 0.33 per g",
                "ABC Foods Pvt Ltd, Industrial Area, Mumbai - 400001",
                null,
                null,
                null,
                "India",
                "08/2026",
                "08/2027",
                "10012345678901",
                "1800-111-222",
                "care@abcfoods.in",
                null,
                "raw ocr"
        );

        ComplianceResult result = engine.evaluate(label);

        assertThat(result.overallStatus()).isEqualTo(RuleStatus.PASS);
        assertThat(result.overallScore()).isEqualTo(100);
        assertThat(result.checks()).hasSize(8);
        assertThat(result.checks()).allMatch(c -> c.status() == RuleStatus.PASS);
    }

    @Test
    @DisplayName("B. Incomplete label declarations produce WARNING status without falsely asserting legal violation")
    void evaluate_incompleteLabel_producesWarning() {
        // Missing MRP, FSSAI, USP, and consumer care
        StructuredLabelData label = new StructuredLabelData(
                null, null, "200 g", null, false, null,
                "ABC Foods", null, null, null, null,
                null, null, null, null, null, null, "raw ocr"
        );

        ComplianceResult result = engine.evaluate(label);

        assertThat(result.overallStatus()).isEqualTo(RuleStatus.WARNING);
        assertThat(result.overallScore()).isLessThan(100).isGreaterThanOrEqualTo(40);
        assertThat(result.checks()).anyMatch(c -> c.id().equals("RULE_MRP") && c.status() == RuleStatus.WARNING);
        assertThat(result.checks()).anyMatch(c -> c.id().equals("RULE_NET_QTY") && c.status() == RuleStatus.PASS);
        assertThat(result.checks()).anyMatch(c -> c.id().equals("RULE_USP") && c.status() == RuleStatus.WARNING);
    }

    @Test
    @DisplayName("C. Contradictory/illegal data (e.g. negative MRP) produces clear VIOLATION status")
    void evaluate_contradictoryData_producesViolation() {
        StructuredLabelData label = new StructuredLabelData(
                null, null, "100 g", "-10.00", false, null,
                "ABC Foods", null, null, null, null,
                "08/2026", null, "10012345678901", null, null, null, "raw ocr"
        );

        ComplianceResult result = engine.evaluate(label);

        assertThat(result.overallStatus()).isEqualTo(RuleStatus.VIOLATION);
        assertThat(result.checks()).anyMatch(c -> c.id().equals("RULE_MRP") && c.status() == RuleStatus.VIOLATION);
    }

    @Test
    @DisplayName("D. Imported commodity missing Country of Origin produces high-severity WARNING under Rule 6(10)")
    void evaluate_importedCommodityMissingOrigin_producesHighWarning() {
        StructuredLabelData label = new StructuredLabelData(
                null, null, "500 g", "250.00", true, null,
                null, null, "Global Imports Ltd, New Delhi", null, null,
                "08/2026", null, "10012345678901", null, null, null, "raw ocr"
        );

        ComplianceResult result = engine.evaluate(label);

        ComplianceCheck originCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_ORIGIN"))
                .findFirst()
                .orElseThrow();

        assertThat(originCheck.status()).isEqualTo(RuleStatus.WARNING);
        assertThat(originCheck.severity()).isEqualTo(RuleSeverity.HIGH);
        assertThat(originCheck.legalReason()).contains("strictly mandatory for all imported commodities");
    }

    @Test
    @DisplayName("E. MRP present without 'inclusive of all taxes' generates low-severity WARNING")
    void evaluate_mrpWithoutTaxStatement_producesLowWarning() {
        StructuredLabelData label = new StructuredLabelData(
                null, null, "100 g", "50.00", false, null,
                "ABC Foods", null, null, null, "India",
                "08/2026", null, "10012345678901", "1800-123-456", "support@test.in", null, "raw ocr"
        );

        ComplianceResult result = engine.evaluate(label);

        ComplianceCheck mrpCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_MRP"))
                .findFirst()
                .orElseThrow();

        assertThat(mrpCheck.status()).isEqualTo(RuleStatus.WARNING);
        assertThat(mrpCheck.severity()).isEqualTo(RuleSeverity.LOW);
        assertThat(mrpCheck.legalReason()).contains("inclusive of all taxes");
    }

    @Test
    @DisplayName("F. FSSAI check verifies format and explicitly disclaims official portal authenticity")
    void evaluate_fssaiDisclaimer_doesNotClaimAuthenticity() {
        StructuredLabelData label = new StructuredLabelData(
                null, null, "100 g", null, false, null,
                null, null, null, null, null,
                null, null, "10012345678901", null, null, null, "raw ocr"
        );

        ComplianceResult result = engine.evaluate(label);

        ComplianceCheck fssaiCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_FSSAI"))
                .findFirst()
                .orElseThrow();

        assertThat(fssaiCheck.status()).isEqualTo(RuleStatus.PASS);
        assertThat(fssaiCheck.legalReason()).contains("does not confirm active license validity, authenticity, or licensee identity on the official FoSCoS portal");
    }
}

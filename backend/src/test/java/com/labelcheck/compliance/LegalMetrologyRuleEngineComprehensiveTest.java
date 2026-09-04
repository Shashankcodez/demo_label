package com.labelcheck.compliance;

import com.labelcheck.compliance.model.ApplicabilityProfile;
import com.labelcheck.compliance.model.NormalizedLabel;
import com.labelcheck.compliance.model.RegulationFamily;
import com.labelcheck.compliance.rules.ConsumerCareRule;
import com.labelcheck.compliance.rules.DateMarkingRule;
import com.labelcheck.compliance.rules.ExemptionRule;
import com.labelcheck.compliance.rules.FontSizeRule;
import com.labelcheck.compliance.rules.FssaiLicenseRule;
import com.labelcheck.compliance.rules.GenericNameRule;
import com.labelcheck.compliance.rules.LegalMetrologyRuleCatalog;
import com.labelcheck.compliance.rules.LegibilityRule;
import com.labelcheck.compliance.rules.ManufacturerRule;
import com.labelcheck.compliance.rules.MrpRule;
import com.labelcheck.compliance.rules.NetQuantityRule;
import com.labelcheck.compliance.rules.OriginRule;
import com.labelcheck.compliance.rules.QuantityQualifierRule;
import com.labelcheck.compliance.rules.UnitSalePriceRule;
import com.labelcheck.dto.StructuredLabelData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LegalMetrologyRuleEngineComprehensiveTest {

    private ComplianceRuleEngine engine;

    @BeforeEach
    void setUp() {
        List<ComplianceRule> allRules = List.of(
                new MrpRule(),
                new UnitSalePriceRule(),
                new NetQuantityRule(),
                new FssaiLicenseRule(),
                new DateMarkingRule(),
                new ManufacturerRule(),
                new ConsumerCareRule(),
                new OriginRule(),
                new GenericNameRule(),
                new QuantityQualifierRule(),
                new FontSizeRule(),
                new LegibilityRule(),
                new ExemptionRule()
        );
        engine = new ComplianceRuleEngine(allRules);
    }

    @Test
    @DisplayName("1. Catalog contains version LM-PCR-2026.01 and all statutory rules")
    void catalog_containsVersionAndRules() {
        assertThat(LegalMetrologyRuleCatalog.ENGINE_VERSION).isEqualTo("LM-PCR-2026.01");
        assertThat(LegalMetrologyRuleCatalog.getAll()).hasSize(13);
        assertThat(LegalMetrologyRuleCatalog.get("RULE_MRP")).isNotNull();
        assertThat(LegalMetrologyRuleCatalog.get("RULE_QUANTITY_QUALIFIER")).isNotNull();
        assertThat(LegalMetrologyRuleCatalog.get("RULE_FONT_SIZE")).isNotNull();
    }

    @Test
    @DisplayName("2. Rule 13(5) QuantityQualifierRule flags prohibited expressions as FAIL")
    void quantityQualifier_prohibitedWords_producesFail() {
        StructuredLabelData labelWithProhibited = new StructuredLabelData(
                "Refined Sunflower Oil",
                "SunGold",
                "1 Litre when packed",
                "150.00",
                true,
                "Rs 150 per L",
                "ABC Oils Ltd, Industrial Area, Gujarat",
                null,
                null,
                null,
                "India",
                "08/2026",
                "08/2027",
                "10012345678901",
                "1800-111-222",
                "care@abcoils.in",
                null,
                "Net Qty: 1 Litre when packed"
        );

        ComplianceResult result = engine.evaluate(labelWithProhibited);

        assertThat(result.overallStatus()).isEqualTo(RuleStatus.VIOLATION);
        assertThat(result.violations()).anyMatch(c -> c.id().equals("RULE_QUANTITY_QUALIFIER") && (c.status() == RuleStatus.FAIL || c.status() == RuleStatus.VIOLATION));
    }

    @Test
    @DisplayName("3. Schedule II FontSizeRule on 2D image produces REQUIRES_MANUAL_VERIFICATION, never statutory FAIL")
    void fontSizeRule_uncalibratedImage_producesRequiresManualVerification() {
        StructuredLabelData compliantLabel = new StructuredLabelData(
                "Refined Sunflower Oil",
                "SunGold",
                "1 Litre",
                "150.00",
                true,
                "Rs 150 per L",
                "ABC Oils Ltd, Industrial Area, Gujarat",
                null,
                null,
                null,
                "India",
                "08/2026",
                "08/2027",
                "10012345678901",
                "1800-111-222",
                "care@abcoils.in",
                null,
                "Standard label text"
        );

        ComplianceResult result = engine.evaluate(compliantLabel);

        ComplianceCheck fontCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_FONT_SIZE"))
                .findFirst()
                .orElseThrow();

        assertThat(fontCheck.status()).isEqualTo(RuleStatus.REQUIRES_MANUAL_VERIFICATION);
        assertThat(fontCheck.severity()).isEqualTo(RuleSeverity.INFO);
        assertThat(fontCheck.legalReason()).contains("automated vision tools must never fabricate statutory font infractions without physical verification");
    }

    @Test
    @DisplayName("4. GenericNameRule detects commodity descriptor distinct from brand")
    void genericNameRule_detectsCommodityDescriptor() {
        StructuredLabelData brandOnlyLabel = new StructuredLabelData(
                "SuperCrunch",
                "SuperCrunch",
                "200 g",
                "40.00",
                true,
                null,
                "Snack Co",
                null,
                null,
                null,
                "India",
                "08/2026",
                null,
                null,
                null,
                null,
                null,
                "SuperCrunch"
        );

        ComplianceResult result = engine.evaluate(brandOnlyLabel);

        ComplianceCheck genericCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_GENERIC_NAME"))
                .findFirst()
                .orElseThrow();

        assertThat(genericCheck.status()).isEqualTo(RuleStatus.WARNING);
        assertThat(genericCheck.legalReason()).contains("brand without explicit generic commodity description");
    }

    @Test
    @DisplayName("5. ExemptionRule recognizes Rule 26(a) small pack exemption (<= 10g)")
    void exemptionRule_smallPack_recognizesExemption() {
        NormalizedLabel smallPack = new NormalizedLabel(
                "Shampoo Sachet",
                "Shampoo",
                "ShinyHair",
                "6 ml",
                6.0,
                "ml",
                "3.00",
                3.0,
                true,
                null,
                null,
                null,
                "Beauty Corp",
                "Mumbai",
                null, null, null, null, null, null,
                "India",
                "08/2026",
                "08/2028",
                "B123",
                null, null, null,
                null, "NOT_APPLICABLE",
                Map.of(), Map.of(), "6 ml Shampoo MRP 3.00", true
        );

        ExemptionRule rule = new ExemptionRule();
        ComplianceCheck check = rule.evaluateNormalized(smallPack, ApplicabilityProfile.defaultRetailFood());

        assertThat(check.status()).isEqualTo(RuleStatus.PASS);
        assertThat(check.detected()).contains("Small Package (<= 10g/ml)");
        assertThat(check.legalReason()).contains("small-package exemption under Rule 26(a)");
    }

    @Test
    @DisplayName("6. Engine resilience: crashing rule yields REQUIRES_MANUAL_VERIFICATION, does not crash pipeline")
    void engine_resilience_crashingRuleHandledGracefully() {
        ComplianceRule throwingRule = new ComplianceRule() {
            @Override
            public ComplianceCheck evaluate(StructuredLabelData labelData) {
                throw new RuntimeException("Simulated unexpected parsing crash");
            }

            @Override
            public String getRuleId() {
                return "RULE_FAULTY_MOCK";
            }
        };

        ComplianceRuleEngine resilientEngine = new ComplianceRuleEngine(List.of(throwingRule));
        ComplianceResult result = resilientEngine.evaluate(new StructuredLabelData(
                "Test", null, null, null, false, null, null, null, null, null, null, null, null, null, null, null, null, ""
        ));

        assertThat(result.checks()).hasSize(1);
        ComplianceCheck check = result.checks().get(0);
        assertThat(check.status()).isEqualTo(RuleStatus.REQUIRES_MANUAL_VERIFICATION);
        assertThat(check.legalReason()).contains("no legal violation is declared");
    }
}

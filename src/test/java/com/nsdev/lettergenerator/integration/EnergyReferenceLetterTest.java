package com.nsdev.lettergenerator.integration;

import com.nsdev.lettergenerator.fixtures.EnergyLetterTestSupport;
import com.nsdev.lettergenerator.fixtures.EnergyReferenceLetter;
import com.nsdev.lettergenerator.fixtures.FluentEnergyLetter;
import com.nsdev.lettergenerator.fixtures.TestResources;
import org.junit.jupiter.api.Test;

class EnergyReferenceLetterTest {
    @Test
    void typedApiRendersTheAnonymizedReferenceLetter() throws Exception {
        byte[] pdf = EnergyLetterTestSupport.generator().generate(EnergyReferenceLetter.document());
        EnergyLetterTestSupport.verifyAndWrite(pdf, "energy-reference-letter");
    }

    @Test
    void fluentApiRendersTheAnonymizedReferenceLetter() throws Exception {
        byte[] pdf = EnergyLetterTestSupport.generator().generate(FluentEnergyLetter.document());
        EnergyLetterTestSupport.verifyAndWrite(pdf, "energy-fluent-letter");
    }

    @Test
    void jsonApiRendersTheIndependentlyAuthoredAnonymizedReferenceLetter() throws Exception {
        String json = TestResources.text(TestResources.ENERGY_JSON);
        byte[] pdf = EnergyLetterTestSupport.generator().generateJson(json);
        EnergyLetterTestSupport.verifyAndWrite(pdf, "energy-json-letter");
    }
}

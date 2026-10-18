package com.nsdev.lettergenerator.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.fixtures.EnergyLetterTestSupport;
import com.nsdev.lettergenerator.fixtures.TestResources;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EnergyTemplateTest {
    @Test
    void htmlTemplateApiRendersTheAnonymizedReferenceLetter() throws Exception {
        String html = TestResources.text(TestResources.ENERGY_TEMPLATE);
        Map<String, String> values =
                new ObjectMapper()
                        .readValue(
                                TestResources.text(TestResources.ENERGY_VALUES),
                                new TypeReference<>() {});
        byte[] logo = TestResources.bytes(TestResources.ENERGY_LOGO);
        byte[] footer = TestResources.bytes(TestResources.ENERGY_FOOTER);
        var letter =
                Letter.template(html)
                        .values(values)
                        .asset("company-logo", logo)
                        .asset("footer-banner", footer)
                        .build();
        byte[] pdf = EnergyLetterTestSupport.generator().generate(letter);
        EnergyLetterTestSupport.verifyAndWrite(pdf, "energy-template-letter");
        Files.createDirectories(Path.of("target/examples/template-assets"));
        Files.write(Path.of("target/examples/template-assets/logo.png"), logo);
        Files.write(Path.of("target/examples/template-assets/footer.png"), footer);
    }
}

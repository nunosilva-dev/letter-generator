package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.renderFirst;
import static com.nsdev.lettergenerator.fixtures.PdfTestSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.fixtures.EdpReferenceLetter;
import com.nsdev.lettergenerator.font.FontVariant;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.junit.jupiter.api.Test;

class EdpTemplateTest {
    @Test
    void databaseStyleTemplateAndMapProduceTheEdpLetterWithAnImageFooter() throws Exception {
        String template = Files.readString(Path.of("examples/templates/edp-price-update.html"));
        Map<String, String> values =
                new ObjectMapper()
                        .readValue(
                                Files.readString(Path.of("examples/templates/edp-values.json")),
                                new TypeReference<>() {});
        byte[] logo = EdpReferenceLetter.logo();
        byte[] footer = banner();
        var letter =
                Letter.template(template)
                        .values(values)
                        .asset("company-logo", logo)
                        .asset("footer-banner", footer)
                        .build();
        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                EdpReferenceLetter.FONT_FAMILY,
                                FontVariant.REGULAR,
                                "fonts/DejaVuSans.ttf")
                        .registerClasspathFont(
                                EdpReferenceLetter.FONT_FAMILY,
                                FontVariant.BOLD,
                                "fonts/DejaVuSans-Bold.ttf")
                        .build();
        byte[] bytes = generator.generate(letter);
        Files.createDirectories(Path.of("target/examples/template-assets"));
        Files.write(Path.of("target/examples/template-assets/logo.png"), logo);
        Files.write(Path.of("target/examples/template-assets/footer.png"), footer);
        Files.write(Path.of("target/examples/edp-template-letter.pdf"), bytes);
        renderFirst(bytes, "edp-template-letter");
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(1, pdf.getNumberOfPages());
        }
        String content = text(bytes).replaceAll("\\s+", " ");
        for (String expected :
                new String[] {
                    "ANA EXEMPLO",
                    "Contrato 123456789",
                    "descida de 15%",
                    "0,1632",
                    "0,0815",
                    "Duarte Bettencourt",
                    "EDP Comercial & Energia"
                }) {
            assertTrue(content.contains(expected), "Missing template value: " + expected);
        }
    }

    private static byte[] banner() throws Exception {
        var image = new BufferedImage(1200, 40, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(
                    RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(new Color(16, 56, 74));
            graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
            graphics.setColor(Color.WHITE);
            graphics.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
            graphics.drawString("edp comercial  |  energia para o seu dia a dia", 320, 28);
        } finally {
            graphics.dispose();
        }
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}

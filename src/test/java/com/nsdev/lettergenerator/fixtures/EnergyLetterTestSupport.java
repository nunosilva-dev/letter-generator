package com.nsdev.lettergenerator.fixtures;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.font.FontVariant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/** Common output contract for the four independently authored reference-letter inputs. */
public final class EnergyLetterTestSupport {
    private static final double POINTS_PER_MM = 72.0 / 25.4;
    private static final double SIGNATURE_TOP_MM = 243;
    private static final double SIGNATURE_BOTTOM_MM = 268;

    private EnergyLetterTestSupport() {}

    public static LetterPdfGenerator generator() {
        return LetterPdfGenerator.builder()
                .registerClasspathFont("ReferenceSans", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                .registerClasspathFont(
                        "ReferenceSans", FontVariant.BOLD, "fonts/DejaVuSans-Bold.ttf")
                .build();
    }

    public static void verifyAndWrite(byte[] bytes, String example) throws Exception {
        try (var pdf = Loader.loadPDF(bytes)) {
            assertEquals(1, pdf.getNumberOfPages(), example + " must fit one A4 page");
            assertEquals(595.28, pdf.getPage(0).getMediaBox().getWidth(), 0.02);
            assertEquals(841.89, pdf.getPage(0).getMediaBox().getHeight(), 0.02);
            int images = 0;
            for (var name : pdf.getPage(0).getResources().getXObjectNames()) {
                if (pdf.getPage(0).getResources().isImageXObject(name)) {
                    images++;
                }
            }
            assertEquals(2, images, example + " must contain the synthetic logo and image footer");
            var positions =
                    new PDFTextStripper() {
                        @Override
                        protected void writeString(String text, List<TextPosition> characters) {
                            if (text.contains("Com os melhores cumprimentos")
                                    || text.contains("RESPONSÁVEL EXEMPLO")
                                    || text.contains("Direção de Apoio")) {
                                for (var character : characters) {
                                    assertTrue(
                                            character.getYDirAdj()
                                                            >= SIGNATURE_TOP_MM * POINTS_PER_MM
                                                    && character.getYDirAdj()
                                                            < SIGNATURE_BOTTOM_MM * POINTS_PER_MM,
                                            example
                                                    + " signature must stay at the bottom above the footer");
                                }
                            }
                        }
                    };
            positions.getText(pdf);
        }
        String content;
        try (var pdf = Loader.loadPDF(bytes)) {
            var reader = new PDFTextStripper();
            reader.setSortByPosition(true);
            content = reader.getText(pdf).replaceAll("\\s+", " ");
        }
        for (String expected :
                List.of(
                        "CLIENTE EXEMPLO",
                        "MORADA FICTÍCIA, 00",
                        "0000-000 LOCALIDADE",
                        "Energia Exemplo",
                        "Contrato DEMO-000001",
                        "descida de 15%",
                        "0,2008",
                        "0,1632",
                        "0,0645",
                        "0,0815",
                        "DEMO-PT-00000000",
                        "energia.example",
                        "000 000 000",
                        "RESPONSÁVEL EXEMPLO",
                        "Direção de Apoio",
                        "Dados totalmente fictícios")) {
            assertTrue(
                    content.contains(expected),
                    example + " missing selectable content: " + expected);
        }
        assertFalse(content.contains("${"), example + " has an unresolved placeholder");
        assertFalse(
                content.matches("(?s).*\\b[a-zA-Z0-9.-]+\\.pt\\b.*"),
                example + " has a real-country website");
        int previous = -1;
        for (String marker :
                List.of(
                        "Caro/a cliente",
                        "O preço do seu contrato",
                        "A Energia Exemplo vai",
                        "Esta redução entra em vigor",
                        "Sobre esta alteração",
                        "Comunicamos também",
                        "Informamos que",
                        "Conheça a sua nova componente",
                        "Potência 3,45",
                        "0,2008",
                        "0,1632",
                        "0,0645",
                        "0,0815",
                        "Nota: os preços",
                        "CPE (Código",
                        "Caso tenha alguma questão",
                        "Para esclarecer questões",
                        "Com os melhores cumprimentos",
                        "RESPONSÁVEL EXEMPLO",
                        "Direção de Apoio")) {
            int position = content.indexOf(marker);
            assertTrue(
                    position > previous, example + " has missing or reordered content: " + marker);
            previous = position;
        }
        Files.createDirectories(Path.of("target/examples"));
        Files.write(Path.of("target/examples/" + example + ".pdf"), bytes);
        PdfTestSupport.renderFirst(bytes, example);
    }
}

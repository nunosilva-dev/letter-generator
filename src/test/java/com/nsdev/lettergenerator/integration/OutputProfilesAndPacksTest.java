package com.nsdev.lettergenerator.integration;

import static com.nsdev.lettergenerator.fixtures.LetterFixtures.document;
import static com.nsdev.lettergenerator.fixtures.LetterFixtures.paragraph;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.document.PdfAttachment;
import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.DocumentMetadata;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Heading;
import com.nsdev.lettergenerator.model.block.ImageBlock;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.block.TableBlock;
import com.nsdev.lettergenerator.model.style.Style;
import com.nsdev.lettergenerator.model.table.TableCell;
import com.nsdev.lettergenerator.model.text.TextRun;
import com.nsdev.lettergenerator.render.OutputProfile;
import com.nsdev.lettergenerator.render.RenderOptions;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureElement;
import org.apache.pdfbox.pdmodel.documentinterchange.logicalstructure.PDStructureNode;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationText;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class OutputProfilesAndPacksTest {
    private final LetterPdfGenerator generator = new LetterPdfGenerator();

    @Test
    void metadataAndLanguageReachOrdinaryAndTaggedPdfs() throws Exception {
        var model =
                document(List.of(paragraph("Portuguese letter")))
                        .withSettings(
                                new DocumentSettings(
                                        new LocaleSettings("pt-PT"),
                                        new DocumentMetadata(
                                                "Title", "Author", "Subject", "one, two"),
                                        null,
                                        null,
                                        null));
        for (var profile : List.of(OutputProfile.STANDARD, OutputProfile.TAGGED)) {
            var result = generator.render(model, new RenderOptions(false, profile));
            try (var pdf = Loader.loadPDF(result.pdfBytes())) {
                assertEquals("Title", pdf.getDocumentInformation().getTitle());
                assertEquals("Author", pdf.getDocumentInformation().getAuthor());
                assertEquals("Subject", pdf.getDocumentInformation().getSubject());
                assertEquals("one, two", pdf.getDocumentInformation().getKeywords());
                assertEquals("pt-PT", pdf.getDocumentCatalog().getLanguage());
                if (profile == OutputProfile.TAGGED) {
                    assertTrue(pdf.getDocumentCatalog().getMarkInfo().isMarked());
                    assertNotNull(pdf.getDocumentCatalog().getStructureTreeRoot());
                }
            }
        }
    }

    @Test
    void documentLanguageRetainsDefaultAndExplicitBcp47ScriptAcrossProfiles() throws Exception {
        var archival =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Archive", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var model =
                new LetterDocument(
                                1,
                                null,
                                new Style(
                                        "Archive", null, null, null, null, null, null, null, null,
                                        null),
                                List.of(
                                        new Section(
                                                "body", null, List.of(paragraph("Serbian Latin")))),
                                null,
                                null,
                                null)
                        .withSettings(
                                new DocumentSettings(
                                        new LocaleSettings("sr-Latn-RS"), null, null, null, null));
        for (String language : List.of("en-GB", "sr-Latn-RS")) {
            var localized =
                    model.withSettings(
                            new DocumentSettings(
                                    new LocaleSettings(language), null, null, null, null));
            for (OutputProfile profile : OutputProfile.values()) {
                try (var pdf =
                        Loader.loadPDF(
                                archival.render(localized, new RenderOptions(false, profile))
                                        .pdfBytes())) {
                    assertEquals(language, pdf.getDocumentCatalog().getLanguage());
                }
            }
        }
    }

    @Test
    void nativeTagsPreserveHeadingTablesAndAvailableImageDescriptions() throws Exception {
        var model =
                document(
                        List.of(
                                new Heading(List.of(new TextRun("Heading", null, null)), null),
                                new TableBlock(
                                        List.of(1d),
                                        List.of(TableCell.text("Header")),
                                        List.of(List.of(TableCell.text("Cell"))),
                                        null),
                                new ImageBlock(
                                        com.nsdev.lettergenerator.fixtures.LetterFixtures.png(),
                                        "image/png",
                                        20d,
                                        10d,
                                        null,
                                        "Company symbol")));
        var result = generator.render(model, new RenderOptions(false, OutputProfile.TAGGED));
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            var elements = new ArrayList<PDStructureElement>();
            structure(pdf.getDocumentCatalog().getStructureTreeRoot(), elements);
            assertTrue(elements.stream().anyMatch(item -> item.getStructureType().equals("H1")));
            assertTrue(elements.stream().anyMatch(item -> item.getStructureType().equals("Table")));
            assertTrue(elements.stream().anyMatch(item -> item.getStructureType().equals("TD")));
            assertTrue(
                    elements.stream()
                            .anyMatch(
                                    item ->
                                            "Company symbol"
                                                    .equals(item.getAlternateDescription())));
        }
        assertTrue(
                result.diagnostics().stream()
                        .anyMatch(
                                item ->
                                        item.code().equals("TAGGED_READING_ORDER")
                                                && item.message().contains("PDF/UA")));
    }

    private static void structure(PDStructureNode node, List<PDStructureElement> elements) {
        for (Object child : node.getKids()) {
            if (child instanceof PDStructureElement element) {
                elements.add(element);
                structure(element, elements);
            }
        }
    }

    @Test
    void appendedPdfPreservesAnnotationsAndLetterOnlyPageCount() throws Exception {
        byte[] appendix = appendix(false);
        var attachment = PdfAttachment.of(appendix);
        var same = PdfAttachment.of(appendix);
        appendix[0] = 0;
        byte[] accessed = attachment.data();
        accessed[0] = 0;
        assertEquals(attachment, same);
        var model =
                document(
                                List.of(
                                        new Paragraph(
                                                List.of(
                                                        new TextRun("Letter total ", null, null),
                                                        TextRun.pageCount()),
                                                null)))
                        .withAttachments(List.of(attachment));
        var result = generator.render(model);
        assertEquals(2, result.pageCount());
        assertEquals(2, result.parts().size());
        assertEquals(1, result.parts().get(0).firstPage());
        assertEquals(1, result.parts().get(0).pageCount());
        assertEquals(2, result.parts().get(1).firstPage());
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            String text = new PDFTextStripper().getText(pdf);
            assertTrue(text.contains("Letter total 1"));
            assertTrue(text.contains("Appendix content"));
            assertEquals("Appendix note", pdf.getPage(1).getAnnotations().get(0).getContents());
        }
    }

    @Test
    void rejectsInvalidEncryptedAndUnpreservableProfileCombinations() throws Exception {
        assertThrows(InvalidLetterException.class, () -> PdfAttachment.of(new byte[] {1, 2}));
        assertThrows(InvalidLetterException.class, () -> PdfAttachment.of(appendix(true)));
        var packed =
                document(List.of(paragraph("Letter")))
                        .withAttachments(List.of(PdfAttachment.of(appendix(false))));
        assertThrows(
                InvalidLetterException.class,
                () -> generator.render(packed, new RenderOptions(false, OutputProfile.TAGGED)));
        assertThrows(
                InvalidLetterException.class,
                () -> generator.render(packed, new RenderOptions(false, OutputProfile.PDF_A_2B)));
        assertThrows(
                InvalidLetterException.class,
                () ->
                        generator.render(
                                document(List.of(paragraph("Built in font"))),
                                new RenderOptions(false, OutputProfile.PDF_A_2B)));
        assertThrows(
                InvalidLetterException.class,
                () ->
                        generator.render(
                                document(List.of(paragraph("Preview"))),
                                new RenderOptions(true, OutputProfile.TAGGED)));
    }

    @Test
    void nativeArchivalEngineRejectsAnUnembeddedFontIndependently() {
        var model = document(List.of(paragraph("Native rejection")));
        var composition =
                new com.nsdev.lettergenerator.internal.render.fo.FoComposer(model, false).compose();
        var exception =
                assertThrows(
                        com.nsdev.lettergenerator.exception.PdfGenerationException.class,
                        () ->
                                com.nsdev.lettergenerator.internal.render.fop.FopRenderer.render(
                                        composition,
                                        com.nsdev.lettergenerator.internal.font.FontRegistry
                                                .empty(),
                                        model.settings(),
                                        new RenderOptions(false, OutputProfile.PDF_A_2B),
                                        com.nsdev.lettergenerator.internal.hyphenation
                                                .HyphenationPatterns.empty()));
        assertTrue(
                java.util.stream.Stream.<Throwable>iterate(
                                exception, java.util.Objects::nonNull, Throwable::getCause)
                        .anyMatch(
                                cause ->
                                        cause.getMessage() != null
                                                && cause.getMessage().contains("embedded")));
    }

    @Test
    void archivalModeUsesNativeOutputIntentAndEmbeddedFonts() throws Exception {
        var archival =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                "Archive", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .build();
        var model =
                new LetterDocument(
                        1,
                        null,
                        new Style("Archive", null, null, null, null, null, null, null, null, null),
                        List.of(new Section("body", null, List.of(paragraph("Archive text")))),
                        null,
                        null,
                        null);
        var result = archival.render(model, new RenderOptions(false, OutputProfile.PDF_A_2B));
        try (var pdf = Loader.loadPDF(result.pdfBytes())) {
            assertFalse(pdf.getDocumentCatalog().getOutputIntents().isEmpty());
            assertNotNull(pdf.getDocumentCatalog().getMetadata());
            String xmp =
                    new String(
                            pdf.getDocumentCatalog().getMetadata().toByteArray(),
                            java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(
                    xmp.contains("pdfaid:part=\"2\"")
                            || xmp.contains("<pdfaid:part>2</pdfaid:part>"));
            for (var name : pdf.getPage(0).getResources().getFontNames()) {
                assertTrue(pdf.getPage(0).getResources().getFont(name).isEmbedded());
            }
        }
    }

    @Test
    void hyphenationRequiresExplicitOwnedPatterns() {
        var model =
                document(List.of(paragraph("representation")))
                        .withSettings(
                                new DocumentSettings(
                                        new LocaleSettings("en-GB"), null, null, null, true));
        assertThrows(InvalidLetterException.class, () -> generator.render(model));
        byte[] patterns =
                ("<hyphenation-info><classes>aA bB cC dD eE fF gG hH iI jJ kK lL "
                                + "mM nN oO pP qQ rR sS tT uU vV wW xX yY zZ</classes>"
                                + "<patterns>rep1re pre1sen sen1ta</patterns></hyphenation-info>")
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var registered =
                LetterPdfGenerator.builder().hyphenationPatterns("en-GB", patterns).build();
        java.util.Arrays.fill(patterns, (byte) 0);
        assertEquals(1, registered.render(model).pageCount());
        assertThrows(
                InvalidLetterException.class,
                () ->
                        LetterPdfGenerator.builder()
                                .hyphenationPatterns(
                                        "en-GB",
                                        "<!DOCTYPE x SYSTEM 'file:/etc/passwd'><hyphenation-info/>"
                                                .getBytes(java.nio.charset.StandardCharsets.UTF_8))
                                .build());
    }

    private static byte[] appendix(boolean encrypted) throws Exception {
        try (var pdf = new PDDocument()) {
            var page = new PDPage();
            var note = new PDAnnotationText();
            note.setContents("Appendix note");
            page.getAnnotations().add(note);
            pdf.addPage(page);
            try (var content = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(
                        new org.apache.pdfbox.pdmodel.font.PDType1Font(
                                org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA),
                        12);
                content.newLineAtOffset(40, 700);
                content.showText("Appendix content");
                content.endText();
            }
            if (encrypted) {
                pdf.protect(
                        new StandardProtectionPolicy("owner", "reader", new AccessPermission()));
            }
            var output = new ByteArrayOutputStream();
            pdf.save(output);
            return output.toByteArray();
        }
    }
}

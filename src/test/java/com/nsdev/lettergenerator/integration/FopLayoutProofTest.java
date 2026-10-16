package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.apache.fop.events.model.EventSeverity;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

/** Characterizes FOP's critical pagination and bounded-container overflow events. */
class FopLayoutProofTest {
    record Result(byte[] pdf, List<String> problems) {}

    private Result render(String xml) throws Exception {
        var factory = FopFactory.newInstance(new File(".").toURI());
        var agent = factory.newFOUserAgent();
        var problems = new ArrayList<String>();
        agent.getEventBroadcaster()
                .addEventListener(
                        e -> {
                            String id = e.getEventID();
                            if (id.toLowerCase(Locale.ROOT).contains("overflow")
                                    || e.getSeverity() == EventSeverity.ERROR
                                    || e.getSeverity() == EventSeverity.FATAL) {
                                problems.add(id);
                            }
                        });
        var out = new ByteArrayOutputStream();
        var fop = factory.newFop(MimeConstants.MIME_PDF, agent, out);
        TransformerFactory.newInstance()
                .newTransformer()
                .transform(
                        new StreamSource(new StringReader(xml)),
                        new SAXResult(fop.getDefaultHandler()));
        return new Result(out.toByteArray(), problems);
    }

    private String letter(int paragraphs, String box) {
        var xml =
                new StringBuilder(
                        "<fo:root xmlns:fo='http://www.w3.org/1999/XSL/Format'><fo:layout-master-set>");
        for (String m : List.of("only", "first", "middle", "last")) {
            boolean last = m.equals("only") || m.equals("last");
            xml.append("<fo:simple-page-master master-name='")
                    .append(m)
                    .append("' page-width='210mm' page-height='297mm' margin='20mm'>")
                    .append("<fo:region-body margin-top='25mm' margin-bottom='")
                    .append(last ? 47 : 15)
                    .append("mm'/>")
                    .append("<fo:region-before region-name='top-")
                    .append(m)
                    .append("' extent='25mm'/>")
                    .append("<fo:region-after region-name='bottom-")
                    .append(m)
                    .append("' extent='")
                    .append(last ? 47 : 15)
                    .append("mm'/></fo:simple-page-master>");
        }
        xml.append(
                "<fo:page-sequence-master master-name='letter'><fo:repeatable-page-master-alternatives>");
        for (String m : List.of("only", "first", "last", "middle")) {
            xml.append("<fo:conditional-page-master-reference master-reference='")
                    .append(m)
                    .append("' page-position='")
                    .append(m.equals("middle") ? "rest" : m)
                    .append("'/>");
        }
        xml.append(
                "</fo:repeatable-page-master-alternatives></fo:page-sequence-master></fo:layout-master-set><fo:page-sequence master-reference='letter' force-page-count='no-force'>");
        for (String m : List.of("only", "first", "middle", "last")) {
            boolean last = m.equals("only") || m.equals("last");
            xml.append("<fo:static-content flow-name='top-")
                    .append(m)
                    .append("'><fo:block>HEADER ")
                    .append(m)
                    .append("</fo:block>")
                    .append(box)
                    .append("</fo:static-content>");
            xml.append("<fo:static-content flow-name='bottom-").append(m).append("'>");
            if (last) {
                xml.append(
                        "<fo:block-container absolute-position='absolute' top='2mm' left='0mm' width='100mm' height='25mm' overflow='error-if-overflow'><fo:block>SIGNATURE</fo:block></fo:block-container>");
            }
            xml.append("<fo:block-container absolute-position='absolute' top='")
                    .append(last ? 32 : 0)
                    .append(
                            "mm' left='0mm' width='170mm' height='15mm' overflow='error-if-overflow'><fo:block>FOOTER</fo:block></fo:block-container></fo:static-content>");
        }
        xml.append("<fo:flow flow-name='xsl-region-body'>");
        for (int i = 0; i < paragraphs; i++) {
            xml.append("<fo:block font-size='11pt' line-height='1.2'>Body marker ")
                    .append(i)
                    .append(" - Pagination proof.</fo:block>");
        }
        return xml.append("</fo:flow></fo:page-sequence></fo:root>").toString();
    }

    @Test
    void onePageAndLastPageMastersKeepSignatureAndFooter() throws Exception {
        Files.createDirectories(Path.of("target/proof"));
        for (int n : List.of(2, 300)) {
            Result r = render(letter(n, ""));
            assertEquals(List.of(), r.problems());
            Files.write(Path.of("target/proof/" + n + ".pdf"), r.pdf());
            try (PDDocument pdf = Loader.loadPDF(r.pdf())) {
                assertTrue(n == 2 ? pdf.getNumberOfPages() == 1 : pdf.getNumberOfPages() > 3);
                var stripper = new PDFTextStripper();
                for (int p = 1; p <= pdf.getNumberOfPages(); p++) {
                    stripper.setStartPage(p);
                    stripper.setEndPage(p);
                    String text = stripper.getText(pdf);
                    assertTrue(text.contains("FOOTER"));
                    assertEquals(p == pdf.getNumberOfPages(), text.contains("SIGNATURE"));
                    String master =
                            pdf.getNumberOfPages() == 1
                                    ? "only"
                                    : p == 1
                                            ? "first"
                                            : p == pdf.getNumberOfPages() ? "last" : "middle";
                    assertTrue(text.contains("HEADER " + master), text);
                }
                var renderer = new PDFRenderer(pdf);
                ImageIO.write(
                        renderer.renderImageWithDPI(0, 90),
                        "png",
                        new File("target/proof/" + n + "-first.png"));
                ImageIO.write(
                        renderer.renderImageWithDPI(pdf.getNumberOfPages() - 1, 90),
                        "png",
                        new File("target/proof/" + n + "-last.png"));
            }
        }
    }

    @Test
    void fopReportsVerticalAndHorizontalFixedBoxOverflow() throws Exception {
        for (String block :
                List.of(
                        "<fo:block font-size='30pt'>too tall</fo:block>",
                        "<fo:block wrap-option='no-wrap'>" + "W".repeat(200) + "</fo:block>")) {
            String box =
                    "<fo:block-container absolute-position='absolute' top='10mm' left='0mm' width='15mm' height='3mm' overflow='error-if-overflow'>"
                            + block
                            + "</fo:block-container>";
            Result r = render(letter(2, box));
            assertTrue(
                    r.problems().stream()
                            .anyMatch(s -> s.toLowerCase(Locale.ROOT).contains("overflow")),
                    r.problems().toString());
        }
    }
}

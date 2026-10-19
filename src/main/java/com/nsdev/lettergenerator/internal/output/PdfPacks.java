package com.nsdev.lettergenerator.internal.output;

import com.nsdev.lettergenerator.document.PdfAttachment;
import com.nsdev.lettergenerator.exception.PdfGenerationException;
import com.nsdev.lettergenerator.render.RenderPart;
import com.nsdev.lettergenerator.render.RenderPartKind;
import com.nsdev.lettergenerator.render.RenderResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;

/** Appends explicitly supplied PDF pages after the fully accepted generated letter. */
public final class PdfPacks {
    /** Prevents utility construction. */
    private PdfPacks() {}

    /**
     * Imports attachment page content and annotations without decorating or renumbering them.
     *
     * @param letter accepted letter result, including any authoring overlay
     * @param attachments immutable validated PDF resources in append order
     * @return complete pack with one-based part boundaries and complete page count
     */
    public static RenderResult append(RenderResult letter, List<PdfAttachment> attachments) {
        if (attachments.isEmpty()) {
            return letter;
        }
        List<RenderPart> parts = new ArrayList<>(letter.parts());
        try (var destination = Loader.loadPDF(letter.pdfBytes())) {
            PDFMergerUtility merger = new PDFMergerUtility();
            for (PdfAttachment attachment : attachments) {
                try (var source = Loader.loadPDF(attachment.data())) {
                    int firstPage = destination.getNumberOfPages() + 1;
                    int count = source.getNumberOfPages();
                    merger.appendDocument(destination, source);
                    parts.add(new RenderPart(RenderPartKind.ATTACHMENT, firstPage, count));
                }
            }
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            destination.save(output);
            return new RenderResult(
                    output.toByteArray(),
                    destination.getNumberOfPages(),
                    letter.diagnostics(),
                    letter.debugBoxes(),
                    parts);
        } catch (IOException | RuntimeException exception) {
            throw new PdfGenerationException("Unable to append PDF attachments", exception);
        }
    }
}

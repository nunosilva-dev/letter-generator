package com.nsdev.lettergenerator.document;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.apache.pdfbox.Loader;

/**
 * Owned, validated PDF pages to append after a generated letter. Encrypted and empty PDFs are
 * rejected; equality and hashing compare encoded contents. Attachments are accepted only by the
 * standard output profile because merging arbitrary PDFs cannot preserve profile guarantees.
 *
 * @param data complete unencrypted PDF bytes, copied on construction and access
 */
public record PdfAttachment(byte[] data) {
    /**
     * Takes a defensive snapshot and checks PDF readability without retaining parser state.
     *
     * @param data complete nonempty PDF bytes
     * @throws InvalidLetterException if the bytes are absent, invalid, encrypted or have no pages
     */
    public PdfAttachment {
        if (data == null) {
            throw new InvalidLetterException("attachments", "PDF bytes are required");
        }
        data = data.clone();
        try (var pdf = Loader.loadPDF(data)) {
            if (pdf.isEncrypted() || pdf.getNumberOfPages() < 1) {
                throw new InvalidLetterException(
                        "attachments", "PDF attachments must be unencrypted and contain pages");
            }
        } catch (IOException exception) {
            throw new InvalidLetterException("attachments", "Invalid or encrypted PDF", exception);
        }
    }

    /**
     * Snapshots an already encoded PDF.
     *
     * @param bytes PDF contents
     * @return independent immutable attachment
     */
    public static PdfAttachment of(byte[] bytes) {
        return new PdfAttachment(bytes);
    }

    /**
     * Reads an explicitly trusted local PDF path once.
     *
     * @param path non-null PDF source
     * @return immutable attachment independent of later file changes
     * @throws InvalidLetterException if the path cannot be read or is not an accepted PDF
     */
    public static PdfAttachment of(Path path) {
        if (path == null) {
            throw new InvalidLetterException("attachments", "PDF path is required");
        }
        try {
            return of(Files.readAllBytes(path));
        } catch (IOException exception) {
            throw new InvalidLetterException("attachments", "Unable to read PDF path", exception);
        }
    }

    /**
     * @return an independent copy of the encoded PDF
     */
    @Override
    public byte[] data() {
        return data.clone();
    }

    /**
     * Compares exact encoded PDF contents.
     *
     * @param other nullable candidate
     * @return whether the encoded PDFs match
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof PdfAttachment attachment && Arrays.equals(data, attachment.data);
    }

    /**
     * @return content-based hash consistent with equality
     */
    @Override
    public int hashCode() {
        return Arrays.hashCode(data);
    }
}

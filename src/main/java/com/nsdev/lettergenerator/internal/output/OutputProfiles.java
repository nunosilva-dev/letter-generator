package com.nsdev.lettergenerator.internal.output;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.render.OutputProfile;
import com.nsdev.lettergenerator.render.RenderOptions;
import java.io.StringReader;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/** Rejects output choices whose native profile guarantees could not survive processing. */
public final class OutputProfiles {
    /** Shared path for profile compatibility errors. */
    private static final String PROFILE_PATH = "render.profile";

    /** Prevents utility construction. */
    private OutputProfiles() {}

    /**
     * Rejects incompatible postprocessors and appended PDFs before composition or rendering.
     *
     * @param document prepared and validated letter
     * @param options output options
     */
    public static void validate(LetterDocument document, RenderOptions options) {
        if (options.profile() == OutputProfile.STANDARD) {
            return;
        }
        if (options.debug() || document.watermark() != null || !document.attachments().isEmpty()) {
            throw new InvalidLetterException(
                    PROFILE_PATH,
                    "Tagged and PDF/A output do not support debug overlays, watermarks or PDF packs");
        }
    }

    /**
     * Requires embedded registered families for every resolved FO font before native PDF/A work.
     * Native FOP conformance checks remain active for fonts and any other unsupported content.
     *
     * @param fo trusted generated FO
     * @param fonts immutable registered-font snapshot
     */
    public static void requireEmbeddedFonts(String fo, FontRegistry fonts) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var elements =
                    factory.newDocumentBuilder()
                            .parse(new InputSource(new StringReader(fo)))
                            .getElementsByTagName("*");
            for (int index = 0; index < elements.getLength(); index++) {
                Element element = (Element) elements.item(index);
                if (element.hasAttribute("font-family")
                        && fonts.regular(element.getAttribute("font-family")).isEmpty()) {
                    throw new InvalidLetterException(
                            PROFILE_PATH,
                            "PDF/A requires an explicitly registered embedded font: "
                                    + element.getAttribute("font-family"));
                }
            }
        } catch (InvalidLetterException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidLetterException(
                    PROFILE_PATH,
                    "Unable to inspect generated PDF/A font configuration",
                    exception);
        }
    }
}

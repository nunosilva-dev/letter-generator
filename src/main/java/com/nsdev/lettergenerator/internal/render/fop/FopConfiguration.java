package com.nsdev.lettergenerator.internal.render.fop;

import com.nsdev.lettergenerator.font.FontVariant;
import com.nsdev.lettergenerator.internal.font.FontRegistry;
import com.nsdev.lettergenerator.internal.font.RegisteredFont;
import com.nsdev.lettergenerator.internal.resource.MemoryResourceResolver;
import com.nsdev.lettergenerator.internal.resource.ResourceUris;
import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import org.apache.fop.apps.FopConfParser;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.xml.sax.SAXException;

/** Configures embedded fonts without enabling filesystem or network access. */
final class FopConfiguration {
    /**
     * Joins a private engine URI to an immutable registered-font snapshot.
     *
     * @param uri memory identifier written into the FOP font configuration
     * @param font font whose stream supplier resolves that identifier
     */
    private record FontBinding(URI uri, RegisteredFont font) {}

    /** Prevents instantiation of the per-attempt configuration utility. */
    private FopConfiguration() {}

    /**
     * Builds one factory from generated font XML and a strict per-render memory resolver; it
     * enables no system-font scanning or external resource fallback.
     *
     * @param images composer-assigned image URIs and in-memory bytes for this attempt
     * @param fonts immutable registered-font snapshot supplied by the generator
     * @return a new factory restricted to the registered in-memory resources
     * @throws IOException if reading the generated configuration or a required resource fails
     * @throws SAXException if FOP cannot parse the generated configuration
     */
    static FopFactory factory(Map<URI, byte[]> images, FontRegistry fonts)
            throws IOException, SAXException {
        List<FontBinding> bindings = fontBindings(fonts);
        MemoryResourceResolver resolver = resolver(images, bindings);
        byte[] configuration = fontConfiguration(bindings).getBytes(StandardCharsets.UTF_8);
        try (InputStream input = new ByteArrayInputStream(configuration)) {
            return new FopConfParser(input, ResourceUris.BASE, resolver)
                    .getFopFactoryBuilder()
                    .build();
        }
    }

    /**
     * Sorts font variants before assigning stable private URIs so registry iteration order cannot
     * affect generated configuration.
     *
     * @param fonts immutable registry whose custom faces are to be embedded
     * @return family/variant-sorted bindings with deterministic indexed memory URIs
     */
    private static List<FontBinding> fontBindings(FontRegistry fonts) {
        List<RegisteredFont> sorted =
                fonts.registeredFonts().stream()
                        .sorted(
                                Comparator.comparing(RegisteredFont::family)
                                        .thenComparing(RegisteredFont::variant))
                        .toList();
        List<FontBinding> bindings = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            bindings.add(new FontBinding(ResourceUris.font(index), sorted.get(index)));
        }
        return bindings;
    }

    /**
     * Registers repeatable image-byte streams and registered-font streams under only the URIs
     * created for this attempt.
     *
     * @param images composer-assigned image identifiers and their bytes
     * @param fonts registered faces and their assigned private identifiers
     * @return resolver that creates a fresh stream for each registered resource request
     */
    private static MemoryResourceResolver resolver(
            Map<URI, byte[]> images, List<FontBinding> fonts) {
        Map<URI, Supplier<InputStream>> resources = new HashMap<>();
        images.forEach((uri, bytes) -> resources.put(uri, () -> new ByteArrayInputStream(bytes)));
        fonts.forEach(binding -> resources.put(binding.uri(), binding.font()::openStream));
        return new MemoryResourceResolver(resources);
    }

    /**
     * Writes strict FOP configuration that embeds registered variants as subsets and exposes their
     * configured family/style/weight triplets.
     *
     * @param fonts deterministic custom-face bindings to expose to the PDF renderer
     * @return complete FOP configuration XML with subset-embedding font entries
     */
    private static String fontConfiguration(List<FontBinding> fonts) {
        XmlBuilder xml = new XmlBuilder();
        xml.start("fop").attribute("version", "1.0");
        xml.start("strict-configuration").text("true").end();
        xml.start("renderers")
                .start("renderer")
                .attribute("mime", MimeConstants.MIME_PDF)
                .start("fonts");
        for (FontBinding binding : fonts) {
            FontVariant variant = binding.font().variant();
            xml.start("font")
                    .attribute("embed-url", binding.uri())
                    .attribute("embedding-mode", "subset");
            xml.start("font-triplet")
                    .attribute("name", binding.font().family())
                    .attribute("style", variant.isItalic() ? "italic" : "normal")
                    .attribute("weight", variant.isBold() ? "bold" : "normal")
                    .end();
            xml.end();
        }
        xml.end().end().end().end();
        return xml.finish();
    }
}

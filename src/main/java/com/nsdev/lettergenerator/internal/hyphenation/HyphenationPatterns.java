package com.nsdev.lettergenerator.internal.hyphenation;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.model.LetterDocument;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.hyphenation.HyphenationTree;
import org.apache.fop.hyphenation.HyphenationTreeCache;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

/** Generator-local owned XML patterns; never downloads or scans for dictionaries. */
public final class HyphenationPatterns {
    /** Immutable language-tag to copied pattern XML. */
    private final Map<String, byte[]> patterns;

    /**
     * Owns and validates explicitly trusted application registrations.
     *
     * @param sources language tags and pattern XML bytes
     */
    public HyphenationPatterns(Map<String, byte[]> sources) {
        Map<String, byte[]> owned = new HashMap<>();
        sources.forEach(
                (tag, bytes) -> {
                    String normalized = normalize(tag);
                    if (bytes == null) {
                        throw new InvalidLetterException(
                                "hyphenation", "Pattern XML bytes are required");
                    }
                    byte[] snapshot = bytes.clone();
                    validateXml(snapshot);
                    if (owned.putIfAbsent(normalized, snapshot) != null) {
                        throw new InvalidLetterException(
                                "hyphenation", "Duplicate normalized language tag");
                    }
                });
        patterns = Map.copyOf(owned);
    }

    /**
     * @return empty immutable pattern registrations
     */
    public static HyphenationPatterns empty() {
        return new HyphenationPatterns(Map.of());
    }

    /**
     * Requires an exact registered tag whenever hyphenation is requested.
     *
     * @param document input whose settings select language and hyphenation
     */
    public void validate(LetterDocument document) {
        if (document != null
                && Boolean.TRUE.equals(document.settings().hyphenate())
                && !patterns.containsKey(normalize(document.settings().locale().languageTag()))) {
            throw new InvalidLetterException(
                    "settings.hyphenate",
                    "Hyphenation requires explicitly registered patterns for "
                            + document.settings().locale().languageTag());
        }
    }

    /**
     * Populates one fresh factory's native cache with owned patterns, without URI resolution.
     *
     * @param factory per-attempt FOP factory
     */
    public void configure(FopFactory factory) {
        patterns.forEach(
                (tag, bytes) -> {
                    try {
                        HyphenationTree tree = new HyphenationTree();
                        tree.loadPatterns(new InputSource(new ByteArrayInputStream(bytes)));
                        Locale locale = Locale.forLanguageTag(tag);
                        String country = locale.getCountry().isEmpty() ? null : locale.getCountry();
                        factory.getHyphenationTreeCache()
                                .cache(
                                        HyphenationTreeCache.constructLlccKey(
                                                locale.getLanguage(), country),
                                        tree);
                    } catch (Exception exception) {
                        throw new InvalidLetterException(
                                "hyphenation[" + tag + "]",
                                "Unable to load trusted hyphenation XML",
                                exception);
                    }
                });
    }

    /**
     * Normalizes a finite language/country tag supported by native FOP hyphenation keys.
     *
     * @param tag explicitly supplied BCP 47 language tag
     * @return normalized language tag
     */
    private static String normalize(String tag) {
        try {
            Locale locale = new Locale.Builder().setLanguageTag(tag).build();
            if (locale.getLanguage().isEmpty()
                    || !locale.getScript().isEmpty()
                    || !locale.getVariant().isEmpty()
                    || locale.hasExtensions()) {
                throw new IllegalArgumentException("Expected a language or language-country tag");
            }
            return locale.toLanguageTag();
        } catch (RuntimeException exception) {
            throw new InvalidLetterException(
                    "hyphenation", "Invalid language-country tag", exception);
        }
    }

    /**
     * Rejects entities and external declarations before the native pattern parser sees XML.
     *
     * @param bytes owned XML snapshot
     */
    private static void validateXml(byte[] bytes) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            var builder = factory.newDocumentBuilder();
            builder.setErrorHandler(new DefaultHandler());
            var document = builder.parse(new ByteArrayInputStream(bytes));
            if (!"hyphenation-info".equals(document.getDocumentElement().getTagName())) {
                throw new IllegalArgumentException("Expected hyphenation-info root");
            }
        } catch (Exception exception) {
            throw new InvalidLetterException("hyphenation", "Invalid safe pattern XML", exception);
        }
    }
}

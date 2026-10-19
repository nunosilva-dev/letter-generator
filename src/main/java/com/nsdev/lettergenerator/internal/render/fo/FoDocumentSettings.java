package com.nsdev.lettergenerator.internal.render.fo;

import com.nsdev.lettergenerator.internal.xml.XmlBuilder;
import com.nsdev.lettergenerator.localization.LocaleSettings;
import com.nsdev.lettergenerator.model.DocumentSettings;
import com.nsdev.lettergenerator.model.layout.TextDirection;
import java.awt.ComponentOrientation;
import java.util.Locale;
import java.util.Set;

/** Emits inherited native language, shaping, direction and explicitly enabled hyphenation. */
final class FoDocumentSettings {
    /** Native PDF dictionary extension vocabulary, resolved locally by FOP. */
    private static final String PDF_NAMESPACE = "http://xmlgraphics.apache.org/fop/extensions/pdf";

    /** Explicit BCP 47 scripts whose Unicode writing direction is right-to-left. */
    private static final Set<String> RTL_SCRIPTS =
            Set.of("Adlm", "Arab", "Hebr", "Mand", "Mend", "Nkoo", "Rohg", "Samr", "Syrc", "Thaa");

    /** Common language defaults beyond those recognized by the JDK orientation lookup. */
    private static final Set<String> RTL_LANGUAGES = Set.of("dv", "ps", "sd", "syr", "ug", "yi");

    /** Prevents utility construction. */
    private FoDocumentSettings() {}

    /**
     * Adds native inherited properties while leaving legacy direction implicit when absent. AUTO
     * derives from an explicit script first, then the configured document language.
     *
     * @param xml current FO root, before child elements
     * @param settings normalized immutable document settings
     */
    static void write(XmlBuilder xml, DocumentSettings settings) {
        Locale locale = Locale.forLanguageTag(settings.locale().languageTag());
        boolean explicitLayoutLanguage =
                !settings.locale().languageTag().equals(LocaleSettings.defaults().languageTag())
                        || settings.direction() != null
                        || Boolean.TRUE.equals(settings.hyphenate());
        if (explicitLayoutLanguage) {
            xml.attribute("language", locale.getLanguage());
            if (!locale.getCountry().isEmpty()) {
                xml.attribute("country", locale.getCountry());
            }
            if (!locale.getScript().isEmpty()) {
                xml.attribute("script", locale.getScript());
            }
        }
        TextDirection direction = settings.direction();
        if (direction != null) {
            boolean rtl =
                    direction == TextDirection.RTL
                            || direction == TextDirection.AUTO && rightToLeft(locale);
            xml.attribute("writing-mode", rtl ? "rl-tb" : "lr-tb")
                    .attribute("direction", rtl ? "rtl" : "ltr");
        }
        if (Boolean.TRUE.equals(settings.hyphenate())) {
            xml.attribute("hyphenate", true);
        }
    }

    /**
     * Emits the complete BCP 47 tag directly into the native PDF catalog. Keeping the default
     * shaping language implicit retains legacy glyph positioning, while this declaration still
     * supplies language metadata without a postprocessing pass. Explicit nondefault languages,
     * writing direction and hyphenation also receive native layout properties.
     *
     * @param xml FO root after its layout-master-set, before page sequences
     * @param settings explicit normalized document language
     */
    static void catalog(XmlBuilder xml, DocumentSettings settings) {
        xml.start("declarations")
                .start("pdf", "catalog", PDF_NAMESPACE)
                .start("pdf", "string", PDF_NAMESPACE)
                .attribute("key", "Lang")
                .text(settings.locale().languageTag())
                .end()
                .end()
                .end();
    }

    /**
     * Resolves a language default without consulting the JVM default locale.
     *
     * @param locale explicit document locale
     * @return whether its selected script or language has a right-to-left base direction
     */
    private static boolean rightToLeft(Locale locale) {
        if (!locale.getScript().isEmpty()) {
            return RTL_SCRIPTS.contains(locale.getScript());
        }
        return RTL_LANGUAGES.contains(locale.getLanguage())
                || !ComponentOrientation.getOrientation(locale).isLeftToRight();
    }
}

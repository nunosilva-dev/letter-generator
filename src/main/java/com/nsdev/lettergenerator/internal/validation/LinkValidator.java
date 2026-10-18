package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Restricts PDF link targets to explicit HTTP, HTTPS or mailto URIs; relative links,
 * protocol-relative URLs, and file URIs are not accepted as link targets.
 */
final class LinkValidator {
    /** Mail-address URI scheme, which does not require an HTTP-style host. */
    private static final String MAILTO_SCHEME = "mailto";

    /** Lowercase schemes allowed for PDF external links; comparison is case-insensitive. */
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", MAILTO_SCHEME);

    /** Prevents instantiation of the stateless hyperlink check. */
    private LinkValidator() {}

    /**
     * Accepts a null link as ordinary text. Otherwise checks URI syntax, an allowed scheme,
     * nonempty target and a host for HTTP(S), reporting the caller field path.
     *
     * @param href optional external hyperlink; null denotes unlinked text
     * @param path model-field path used to locate any validation error
     * @throws com.nsdev.lettergenerator.exception.InvalidLetterException if the supplied URI is
     *     malformed, uses a forbidden scheme, or lacks a target/required host
     */
    static void validate(String href, String path) {
        if (href == null) {
            return;
        }
        try {
            URI uri = new URI(href);
            String scheme = uri.getScheme();
            ValidationChecks.require(
                    scheme != null && ALLOWED_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT)),
                    path,
                    "only http, https and mailto links are allowed");
            ValidationChecks.require(!uri.getRawSchemeSpecificPart().isBlank(), path, "empty link");
            if (!MAILTO_SCHEME.equalsIgnoreCase(scheme)) {
                ValidationChecks.require(uri.getHost() != null, path, "URL needs a host");
            }
        } catch (URISyntaxException exception) {
            throw new InvalidLetterException(path, "malformed URL", exception);
        }
    }
}

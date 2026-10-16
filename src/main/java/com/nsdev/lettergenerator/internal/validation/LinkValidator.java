package com.nsdev.lettergenerator.internal.validation;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

final class LinkValidator {
    private static final String MAILTO_SCHEME = "mailto";
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https", MAILTO_SCHEME);

    private LinkValidator() {}

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

package com.nsdev.lettergenerator.fixtures;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Reads packaged test inputs independently of the process working directory. */
public final class TestResources {
    public static final String ENERGY_TEMPLATE = "letters/energy/energy-price-update.html";
    public static final String ENERGY_VALUES = "letters/energy/energy-values.json";
    public static final String ENERGY_JSON = "letters/energy/energy-letter.json";
    public static final String ENERGY_LOGO = "letters/energy/energy-logo.png";
    public static final String ENERGY_FOOTER = "letters/energy/energy-footer.png";

    private TestResources() {}

    public static byte[] bytes(String name) throws IOException {
        try (var stream = TestResources.class.getResourceAsStream("/" + name)) {
            if (stream == null) {
                throw new IOException("Missing test resource: " + name);
            }
            return stream.readAllBytes();
        }
    }

    public static String text(String name) throws IOException {
        return new String(bytes(name), StandardCharsets.UTF_8);
    }
}

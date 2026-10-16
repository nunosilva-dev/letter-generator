package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Trusted font source supplied by Java configuration, never by a letter document. */
public sealed interface FontSource permits FontSource.FileSource, FontSource.ClasspathSource {
    String family();

    FontVariant variant();

    byte[] readBytes() throws IOException;

    static FontSource file(String family, FontVariant variant, Path path) {
        if (path == null) {
            throw new InvalidLetterException("fonts", "path is required");
        }
        return new FileSource(family, variant, path);
    }

    static FontSource classpath(String family, FontVariant variant, String resourcePath) {
        return new ClasspathSource(family, variant, resourcePath);
    }

    record FileSource(String family, FontVariant variant, Path path) implements FontSource {
        @Override
        public byte[] readBytes() throws IOException {
            return Files.readAllBytes(path);
        }
    }

    record ClasspathSource(String family, FontVariant variant, String resourcePath)
            implements FontSource {
        @Override
        public byte[] readBytes() throws IOException {
            if (resourcePath == null || resourcePath.isBlank()) {
                throw new IOException("Missing resource path");
            }
            String name = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                loader = FontSource.class.getClassLoader();
            }
            try (InputStream input = loader.getResourceAsStream(name)) {
                if (input == null) {
                    throw new IOException("Classpath font not found: " + name);
                }
                return input.readAllBytes();
            }
        }
    }
}

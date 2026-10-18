package com.nsdev.lettergenerator.internal.font;

import com.nsdev.lettergenerator.exception.InvalidLetterException;
import com.nsdev.lettergenerator.font.FontVariant;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/** Trusted font source supplied by Java configuration, never by a letter document. */
public sealed interface FontSource permits FontSource.FileSource, FontSource.ClasspathSource {
    /**
     * @return caller-configured, case-sensitive family identifier; validated at registry load
     */
    String family();

    /**
     * @return configured face variant; validated at registry load
     */
    FontVariant variant();

    /**
     * Reads a fresh complete font byte array; this operation does not verify the font format.
     *
     * @return bytes owned by the caller
     * @throws IOException if the configured resource cannot be read
     */
    byte[] readBytes() throws IOException;

    /**
     * Describes a trusted file to read when the generator loads its font registry.
     *
     * @param family configured family name, validated later
     * @param variant configured face, validated later
     * @param path non-null application-selected path
     * @return deferred file source; the file is not opened here
     * @throws InvalidLetterException if path is null
     */
    static FontSource file(String family, FontVariant variant, Path path) {
        if (path == null) {
            throw new InvalidLetterException("fonts", "path is required");
        }
        return new FileSource(family, variant, path);
    }

    /**
     * Describes a classpath resource, read through the context class loader at registry load.
     *
     * @param family configured family name, validated later
     * @param variant configured face, validated later
     * @param resourcePath resource name, optionally with a leading slash; validated on read
     * @return deferred classpath source
     */
    static FontSource classpath(String family, FontVariant variant, String resourcePath) {
        return new ClasspathSource(family, variant, resourcePath);
    }

    /**
     * Deferred trusted filesystem source; construction performs no validation or I/O.
     *
     * @param family caller-visible family identifier
     * @param variant registered face variant
     * @param path font file selected by Java configuration
     */
    record FileSource(String family, FontVariant variant, Path path) implements FontSource {
        @Override
        public byte[] readBytes() throws IOException {
            return Files.readAllBytes(path);
        }
    }

    /**
     * Deferred resource source; a leading slash is removed before class-loader lookup. The context
     * loader is preferred, falling back to this library's loader only if absent.
     *
     * @param family caller-visible family identifier
     * @param variant registered face variant
     * @param resourcePath font resource selected by Java configuration
     */
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

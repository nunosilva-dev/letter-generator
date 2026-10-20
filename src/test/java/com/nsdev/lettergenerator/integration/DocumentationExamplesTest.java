package com.nsdev.lettergenerator.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nsdev.lettergenerator.fixtures.PdfTestSupport;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class DocumentationExamplesTest {
    @Test
    void completePublicApiExamplesCompileAndRenderTheirDocumentedOutput() throws Exception {
        Path resources = Path.of("src/test/resources/documentation");
        List<String> apis = List.of("typed", "fluent", "json", "html");
        for (String api : apis) {
            assertTrue(
                    Files.isRegularFile(resources.resolve("java/" + className(api) + ".java")),
                    "Missing complete public example: " + api);
        }
        Path classes = Files.createDirectories(Path.of("target/documentation/classes"));
        var compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "Documentation examples require a JDK");
        try (var files = compiler.getStandardFileManager(null, null, null);
                var sources = Files.list(resources.resolve("java"))) {
            var units =
                    files.getJavaFileObjectsFromPaths(
                            sources.filter(path -> path.toString().endsWith(".java")).toList());
            assertTrue(
                    compiler.getTask(
                                    null,
                                    files,
                                    null,
                                    List.of(
                                            "--release",
                                            "17",
                                            "-Xlint:all",
                                            "-Werror",
                                            "-classpath",
                                            System.getProperty("java.class.path"),
                                            "-d",
                                            classes.toString()),
                                    null,
                                    units)
                            .call(),
                    "Public documentation sources must compile without private helpers");
        }
        try (var loader =
                new URLClassLoader(
                        new URL[] {classes.toUri().toURL(), resources.toUri().toURL()},
                        getClass().getClassLoader())) {
            Files.createDirectories(Path.of("target/examples"));
            for (String api : apis) {
                Path output = Path.of("target/examples/quickstart-" + api + ".pdf");
                loader.loadClass(className(api))
                        .getMethod("main", String[].class)
                        .invoke(null, (Object) new String[] {output.toString()});
                byte[] bytes = Files.readAllBytes(output);
                try (var pdf = Loader.loadPDF(bytes)) {
                    assertEquals(1, pdf.getNumberOfPages());
                    assertTrue(
                            new PDFTextStripper()
                                    .getText(pdf)
                                    .contains("Hello from Letter Generator"),
                            api + " must produce the documented selectable text");
                }
                PdfTestSupport.renderFirst(bytes, "quickstart-" + api);
            }
        }
    }

    private static String className(String api) {
        return switch (api) {
            case "typed" -> "TypedQuickstart";
            case "fluent" -> "FluentQuickstart";
            case "json" -> "JsonQuickstart";
            case "html" -> "HtmlQuickstart";
            default -> throw new IllegalArgumentException(api);
        };
    }
}

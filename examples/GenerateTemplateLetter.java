import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.font.FontVariant;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Application-owned template and value loading; the library only compiles and renders the letter.
 */
public final class GenerateTemplateLetter {
    /** Classpath directory containing fallback template, bindings and image inputs. */
    private static final String RESOURCE_DIRECTORY = "/letters/energy/";

    /** Application-facing family alias shared by template styles and registered font resources. */
    private static final String FONT_FAMILY = "ReferenceSans";

    /**
     * Loads template inputs, registers regular/bold TrueType faces and writes the rendered PDF.
     * Missing positional inputs fall back to energy example classpath resources; include {@code
     * src/test/resources} on the runtime classpath to use those defaults.
     *
     * @param args optional template, JSON values, logo, footer and output file paths at indices 0
     *     through 4; default output is {@code target/examples/application-template-letter.pdf}
     * @throws Exception if input loading, JSON parsing, font registration, template compilation,
     *     PDF generation or output writing fails
     */
    public static void main(String[] args) throws Exception {
        String template = textInput(args, 0, "energy-price-update.html");
        String bindings = textInput(args, 1, "energy-values.json");
        byte[] logo = input(args, 2, "energy-logo.png");
        byte[] footer = input(args, 3, "energy-footer.png");
        Path output =
                Path.of(
                        args.length > 4
                                ? args[4]
                                : "target/examples/application-template-letter.pdf");
        Map<String, String> values =
                new ObjectMapper().readValue(bindings, new TypeReference<>() {});

        var generator =
                LetterPdfGenerator.builder()
                        .registerClasspathFont(
                                FONT_FAMILY, FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                        .registerClasspathFont(
                                FONT_FAMILY, FontVariant.BOLD, "fonts/DejaVuSans-Bold.ttf")
                        .build();
        var letter =
                Letter.template(template)
                        .values(values)
                        .asset("company-logo", logo)
                        .asset("footer-banner", footer)
                        .build();
        byte[] pdf = generator.generate(letter);
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.write(output, pdf);
        System.out.println("Wrote " + output.toAbsolutePath() + " (" + pdf.length + " bytes)");
    }

    /**
     * Loads one positional file argument, or its bundled example resource when omitted. Resource
     * streams are closed before returning; file paths are owned by this application.
     *
     * @param args positional command-line arguments
     * @param index argument position to use when supplied
     * @param resource fallback name relative to the example resource directory
     * @return complete input bytes
     * @throws IOException if the file/resource cannot be read or the resource is missing
     */
    private static byte[] input(String[] args, int index, String resource) throws IOException {
        if (args.length > index) {
            return Files.readAllBytes(Path.of(args[index]));
        }
        try (var stream =
                GenerateTemplateLetter.class.getResourceAsStream(RESOURCE_DIRECTORY + resource)) {
            if (stream == null) {
                throw new IOException(
                        "Missing example resource: "
                                + resource
                                + "; include src/test/resources on the classpath");
            }
            return stream.readAllBytes();
        }
    }

    /**
     * Decodes a positional file or bundled fallback resource as UTF-8 text.
     *
     * @param args positional command-line arguments
     * @param index argument position to use when supplied
     * @param resource fallback name relative to the example resource directory
     * @return decoded template or bindings text
     * @throws IOException if the selected input cannot be read
     */
    private static String textInput(String[] args, int index, String resource) throws IOException {
        return new String(input(args, index, resource), StandardCharsets.UTF_8);
    }
}

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
    private static final String RESOURCE_DIRECTORY = "/letters/energy/";
    private static final String FONT_FAMILY = "ReferenceSans";

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

    private static String textInput(String[] args, int index, String resource) throws IOException {
        return new String(input(args, index, resource), StandardCharsets.UTF_8);
    }
}

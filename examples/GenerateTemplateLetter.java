import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.font.FontVariant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Application-owned template and value loading; the library only compiles and renders the letter.
 */
public final class GenerateTemplateLetter {
    public static void main(String[] args) throws Exception {
        Path templatePath = argument(args, 0, "examples/templates/edp-price-update.html");
        Path valuesPath = argument(args, 1, "examples/templates/edp-values.json");
        Path logoPath = argument(args, 2, "target/examples/template-assets/logo.png");
        Path footerPath = argument(args, 3, "target/examples/template-assets/footer.png");
        Path output = argument(args, 4, "target/examples/application-template-letter.pdf");
        Map<String, String> values =
                new ObjectMapper()
                        .readValue(Files.readString(valuesPath), new TypeReference<>() {});

        var generator =
                LetterPdfGenerator.builder()
                        .registerFont(
                                "ReferenceSans",
                                FontVariant.REGULAR,
                                Path.of("src/test/resources/fonts/DejaVuSans.ttf"))
                        .registerFont(
                                "ReferenceSans",
                                FontVariant.BOLD,
                                Path.of("src/test/resources/fonts/DejaVuSans-Bold.ttf"))
                        .build();
        var letter =
                Letter.template(Files.readString(templatePath))
                        .values(values)
                        .asset("company-logo", logoPath)
                        .asset("footer-banner", footerPath)
                        .build();
        byte[] pdf = generator.generate(letter);
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.write(output, pdf);
        System.out.println("Wrote " + output.toAbsolutePath() + " (" + pdf.length + " bytes)");
    }

    private static Path argument(String[] args, int index, String fallback) {
        return Path.of(args.length > index ? args[index] : fallback);
    }
}

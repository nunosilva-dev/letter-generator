import com.nsdev.lettergenerator.LetterPdfGenerator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Render a strict JSON resource with the same defaults as Java construction. */
public final class JsonQuickstart {
    private JsonQuickstart() {}

    public static void main(String[] args) throws Exception {
        String source;
        try (var stream = JsonQuickstart.class.getResourceAsStream("/letters/hello.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing /letters/hello.json resource");
            }
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        byte[] pdf = new LetterPdfGenerator().generateJson(source);
        Files.write(Path.of(args.length == 0 ? "letter.pdf" : args[0]), pdf);
    }
}

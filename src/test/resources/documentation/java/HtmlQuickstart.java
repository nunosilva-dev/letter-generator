import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Bind literal data to trusted letter markup and render through the shared model. */
public final class HtmlQuickstart {
    private HtmlQuickstart() {}

    public static void main(String[] args) throws Exception {
        String source;
        try (var stream = HtmlQuickstart.class.getResourceAsStream("/letters/hello.html")) {
            if (stream == null) {
                throw new IllegalStateException("Missing /letters/hello.html resource");
            }
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var letter =
                Letter.template(source)
                        .values(Map.of("message", "Hello from Letter Generator"))
                        .build();
        byte[] pdf = new LetterPdfGenerator().generate(letter);
        Files.write(Path.of(args.length == 0 ? "letter.pdf" : args[0]), pdf);
    }
}

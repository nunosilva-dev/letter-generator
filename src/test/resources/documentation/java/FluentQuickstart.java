import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import java.nio.file.Files;
import java.nio.file.Path;

/** A complete first letter using the fluent API and engine-provided standard fonts. */
public final class FluentQuickstart {
    private FluentQuickstart() {}

    public static void main(String[] args) throws Exception {
        var letter = Letter.builder().body("Hello from Letter Generator").build();
        byte[] pdf = new LetterPdfGenerator().generate(letter);
        Files.write(Path.of(args.length == 0 ? "letter.pdf" : args[0]), pdf);
    }
}

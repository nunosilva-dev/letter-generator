import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.model.Section;
import com.nsdev.lettergenerator.model.block.Paragraph;
import com.nsdev.lettergenerator.model.text.TextRun;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** The same letter constructed directly from immutable public model values. */
public final class TypedQuickstart {
    private TypedQuickstart() {}

    public static void main(String[] args) throws Exception {
        var letter =
                new LetterDocument(
                        LetterDocument.SUPPORTED_SCHEMA_VERSION,
                        null,
                        null,
                        List.of(
                                new Section(
                                        "body",
                                        null,
                                        List.of(
                                                new Paragraph(
                                                        List.of(
                                                                new TextRun(
                                                                        "Hello from Letter Generator",
                                                                        null,
                                                                        null)),
                                                        null)))),
                        List.of(),
                        null,
                        null);
        byte[] pdf = new LetterPdfGenerator().generate(letter);
        Files.write(Path.of(args.length == 0 ? "letter.pdf" : args[0]), pdf);
    }
}

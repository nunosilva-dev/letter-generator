import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.model.layout.Margins;
import com.nsdev.lettergenerator.model.style.TextAlign;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Runnable manual examples: built-in fonts, three header alignments and an exact address box. */
public final class HeaderPlacementExamples {
    public static void main(String[] args) throws Exception {
        var generator = new LetterPdfGenerator();
        Path output = Path.of(args.length == 0 ? "target/examples" : args[0]);
        Files.createDirectories(output);
        for (TextAlign alignment :
                new TextAlign[] {TextAlign.LEFT, TextAlign.CENTER, TextAlign.RIGHT}) {
            var letter =
                    Letter.builder()
                            .header("Example Company\nCustomer Services\nLisboa, Portugal")
                            .headerAlignment(alignment)
                            .headerWidthMm(75)
                            .headerHeightMm(25)
                            .heading("Confirmation of your request")
                            .body("Dear customer,\n\nYour request has been received.")
                            .footer("Example Company | example.com")
                            .build();
            Files.write(
                    output.resolve("header-" + alignment.name().toLowerCase(Locale.ROOT) + ".pdf"),
                    generator.generate(letter));
        }
        var positioned =
                Letter.builder()
                        .margins(new Margins(15, 18, 17, 22))
                        .header("CLIENTE EXEMPLO\nMORADA FICTÍCIA, 00\n0000-000 LOCALIDADE")
                        .headerBoundsMm(88, 23, 78, 28)
                        .heading("Address-window placement example")
                        .body(
                                "Dear Ana,\n\nThe address box begins 110 mm from the left paper edge and 38 mm from the top.",
                                "These are illustrative dimensions; choose the measurements required by your stationery.")
                        .footer("Example Company | example.com")
                        .build();
        Files.write(output.resolve("header-window.pdf"), generator.generate(positioned));
        System.out.println("Wrote four header examples to " + output.toAbsolutePath());
    }
}

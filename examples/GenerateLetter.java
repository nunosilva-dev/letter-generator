import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.Table;
import com.nsdev.lettergenerator.model.LetterDocument;
import com.nsdev.lettergenerator.signature.TextSignature;
import java.nio.file.Files;
import java.nio.file.Path;

/** Plain Java application using the fluent API; an optional JSON file uses the advanced API. */
public final class GenerateLetter {
    public static void main(String[] args) throws Exception {
        var generator = new LetterPdfGenerator();
        byte[] bytes =
                args.length == 0
                        ? generator.generate(example())
                        : generator.generateJson(Files.readString(Path.of(args[0])));
        Path output = Path.of(args.length > 1 ? args[1] : "target/examples/application-letter.pdf");
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        Files.write(output, bytes);
        System.out.println("Wrote " + output.toAbsolutePath() + " (" + bytes.length + " bytes)");
    }

    public static LetterDocument example() {
        return Letter.builder()
                .header("Example Company\nCustomer Services")
                .heading("Confirmation of your request")
                .body(
                        "Dear Sir or Madam,\n\nWe confirm receipt of your request.",
                        "The following services have been registered:")
                .table(
                        Table.builder("Service", "Status")
                                .row("Account update", "Received")
                                .row("Contact preferences", "Updated")
                                .build())
                .body("Please keep this letter for your records.")
                .signature(TextSignature.of("The Director\nCustomer Services"))
                .footer("Example Company | example.com")
                .build();
    }
}

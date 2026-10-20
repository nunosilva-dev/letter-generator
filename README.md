# Letter Generator

A Java 17 library for generating PDF letters through fluent Java, immutable Java models, JSON or letter HTML. Shared rendering supports company stationery, pagination, rich tables, reusable themes/components, localized values, PNG/JPEG/static SVG, visible signatures and optional PDF packs.

Coordinates: **`io.github.nunosilva-dev:letter-generator`**. Version: **`1.0.0`**; this does not imply public Maven Central availability.

## Start with a letter

Add the artifact from a repository where it is available, or install it locally from an authorized checkout:

```xml
<dependency>
  <groupId>io.github.nunosilva-dev</groupId>
  <artifactId>letter-generator</artifactId>
  <version>1.0.0</version>
</dependency>
```

```java
import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import java.nio.file.Files;
import java.nio.file.Path;

var letter = Letter.builder().body("Hello from Letter Generator").build();
byte[] pdf = new LetterPdfGenerator().generate(letter);
Files.write(Path.of("letter.pdf"), pdf);
```

The no-argument generator uses engine-provided Helvetica, Times and Courier. Register appropriate TrueType fonts for wider character coverage or embedding; example fonts are not bundled production defaults. Building a document does not render or guarantee fit. Rendering returns complete PDF bytes or throws without returning a partial PDF.

## Read the instruction manual

The complete manual now lives in the tracked [documentation website](website/README.md), organized by developer task:

- [Installation](website/src/content/docs/start/installation.mdx), [API comparison](website/src/content/docs/start/choose-api.md), and [four complete quickstarts](website/src/content/docs/start/quickstart.mdx).
- [Fluent Java](website/src/content/docs/apis/fluent.mdx), [typed Java](website/src/content/docs/apis/typed.mdx), [JSON](website/src/content/docs/apis/json.mdx), and [HTML](website/src/content/docs/apis/html.mdx).
- [Feature guides](website/src/content/docs/guides), [every supported HTML tag](website/src/content/docs/reference/html-tags.mdx), [CSS/units](website/src/content/docs/reference/css.mdx), and [JSON model reference](website/src/content/docs/reference/json-model.mdx).
- [Anonymized energy-letter walkthrough](website/src/content/docs/examples/energy-letter.mdx), [generated PDF gallery](website/src/content/docs/examples/gallery.mdx), [troubleshooting](website/src/content/docs/support/troubleshooting.md), and [supported limits](website/src/content/docs/support/limits.md).

MDX pages include components and generated references; use the local website for the full reading experience. It provides search, remembered API tabs, downloadable runnable examples and public Javadocs. No hosted URL is configured yet.

## Build and verify

Use JDK 17 for project acceptance. Maven Wrapper manages Maven 3.9.11; a global Maven installation is optional. The runtime requires `java.desktop`, including on headless servers.

```sh
./mvnw clean install
```

On Windows use `mvnw.cmd`. The first build downloads dependencies. Formatting, compiler warnings-as-errors, tests, Checkstyle and strict Javadocs are checked. Tests generate PDFs and PNG previews under ignored `target/examples/`.

The build produces binary, sources and public Javadoc JARs. It currently packages actual implementation sources; choose publication/source-distribution settings before deploying artifacts.

Generate only the API documentation:

```sh
./mvnw javadoc:javadoc
./mvnw -Pinternal-javadocs javadoc:javadoc
```

Open `target/apidocs/index.html` for public contracts or `target/internal-docs/apidocs/index.html` for contributor internals. Packaging always attaches public documentation; errors/warnings fail the build. See [Maven Javadoc Plugin](https://maven.apache.org/plugins/maven-javadoc-plugin/usage.html).

Build and serve the website with Node >=22.22.0 and pnpm 11.25.0, after the Maven build above:

```sh
cd website
pnpm install --frozen-lockfile
pnpm verify
pnpm dev
```

The production static output is `website/dist/`. No publishing or deployment happens during these commands. The site contains the manual, approved example assets/source and public Javadocs; it excludes implementation source and internal Javadocs from its generated downloads/site.

## Contributing

[CONTRIBUTING.md](CONTRIBUTING.md) maps packages and authoring/rendering paths, explains conventions, and describes adding a template tag. Internal plans and implementation notes remain under ignored `docs/`. Keep user-facing instructions in the website rather than duplicating the manual in this README.

## Licence

Letter Generator and its examples are licensed under [Apache License 2.0](LICENSE). Dependencies and example fonts retain their own licences and notices.

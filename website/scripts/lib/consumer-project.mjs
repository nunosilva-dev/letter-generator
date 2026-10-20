import { cp, mkdir, readFile, writeFile } from 'node:fs/promises';
import path from 'node:path';

export async function createConsumerProject(root, destination, version) {
  const java = path.join(destination, 'src/main/java');
  const resources = path.join(destination, 'src/main/resources');
  const energy = path.join(java, 'com/nsdev/lettergenerator/examples');
  await mkdir(energy, { recursive: true });
  await mkdir(resources, { recursive: true });
  await cp(path.join(root, 'LICENSE'), path.join(destination, 'LICENSE'));
  await cp(path.join(root, 'src/test/resources/documentation/java'), java, { recursive: true });
  await cp(path.join(root, 'src/test/resources/documentation/letters'), path.join(resources, 'letters'), { recursive: true });
  await cp(path.join(root, 'src/test/resources/letters/energy'), path.join(resources, 'letters/energy'), { recursive: true });
  await cp(path.join(root, 'src/test/resources/fonts'), path.join(resources, 'fonts'), { recursive: true });
  for (const name of ['EnergyReferenceLetter', 'FluentEnergyLetter', 'EnergyLetterContent', 'TestResources']) {
    let source = await readFile(path.join(root, `src/test/java/com/nsdev/lettergenerator/fixtures/${name}.java`), 'utf8');
    source = source.replaceAll('com.nsdev.lettergenerator.fixtures', 'com.nsdev.lettergenerator.examples')
      .replaceAll('TestResources', 'ExampleResources').replaceAll('test resource', 'example resource').replaceAll('test inputs', 'example inputs');
    await writeFile(path.join(energy, `${name === 'TestResources' ? 'ExampleResources' : name}.java`), source);
  }
  const values = JSON.parse(await readFile(path.join(root, 'src/test/resources/letters/energy/energy-values.json'), 'utf8'));
  const entries = Object.entries(values).map(([key,value]) => `                Map.entry(${JSON.stringify(key)}, ${JSON.stringify(value)})`).join(',\n');
  await writeFile(path.join(energy, 'EnergyExamples.java'), `package com.nsdev.lettergenerator.examples;

import com.nsdev.lettergenerator.Letter;
import com.nsdev.lettergenerator.LetterPdfGenerator;
import com.nsdev.lettergenerator.font.FontVariant;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/** Generates the fictional energy letter through all four public APIs. */
public final class EnergyExamples {
    private EnergyExamples() {}
    public static void main(String[] args) throws Exception {
        Path output = Files.createDirectories(Path.of(args.length == 0 ? "output" : args[0]));
        var generator = LetterPdfGenerator.builder()
                .registerClasspathFont("ReferenceSans", FontVariant.REGULAR, "fonts/DejaVuSans.ttf")
                .registerClasspathFont("ReferenceSans", FontVariant.BOLD, "fonts/DejaVuSans-Bold.ttf")
                .build();
        Files.write(output.resolve("energy-typed.pdf"), generator.generate(EnergyReferenceLetter.document()));
        Files.write(output.resolve("energy-fluent.pdf"), generator.generate(FluentEnergyLetter.document()));
        Files.write(output.resolve("energy-json.pdf"), generator.generateJson(ExampleResources.text(ExampleResources.ENERGY_JSON)));
        var values = Map.ofEntries(
${entries});
        var document = Letter.template(ExampleResources.text(ExampleResources.ENERGY_TEMPLATE))
                .values(values)
                .asset("company-logo", ExampleResources.bytes(ExampleResources.ENERGY_LOGO))
                .asset("footer-banner", ExampleResources.bytes(ExampleResources.ENERGY_FOOTER))
                .build();
        Files.write(output.resolve("energy-html.pdf"), generator.generate(document));
    }
}
`);
  await writeFile(path.join(destination, 'pom.xml'), `<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>example</groupId><artifactId>letter-generator-examples</artifactId><version>1.0.0</version>
  <properties><maven.compiler.release>17</maven.compiler.release><project.build.sourceEncoding>UTF-8</project.build.sourceEncoding></properties>
  <dependencies><dependency><groupId>io.github.nunosilva-dev</groupId><artifactId>letter-generator</artifactId><version>${version}</version></dependency></dependencies>
  <build><plugins>
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-compiler-plugin</artifactId><version>3.14.1</version></plugin>
    <plugin><groupId>org.apache.maven.plugins</groupId><artifactId>maven-dependency-plugin</artifactId><version>2.8</version></plugin>
  </plugins></build>
</project>
`);
  await writeFile(path.join(destination, 'README.md'), `# Letter Generator runnable examples

Requires JDK 17+ and Maven 3.9+. This project consumes only io.github.nunosilva-dev:letter-generator:${version}; it needs no library source checkout, test framework or internal API.

The library version must already be available in your configured Maven repository or installed locally. These instructions do not imply it is published on Maven Central.

Build (Windows: replace the classpath colon below with a semicolon):

\`\`\`sh
mvn package dependency:copy-dependencies
java -Djava.awt.headless=true -cp "target/classes:target/dependency/*" FluentQuickstart
java -Djava.awt.headless=true -cp "target/classes:target/dependency/*" TypedQuickstart
java -Djava.awt.headless=true -cp "target/classes:target/dependency/*" JsonQuickstart
java -Djava.awt.headless=true -cp "target/classes:target/dependency/*" HtmlQuickstart
java -Djava.awt.headless=true -cp "target/classes:target/dependency/*" com.nsdev.lettergenerator.examples.EnergyExamples
\`\`\`

Each quickstart writes letter.pdf (or its first command-line argument). EnergyExamples writes four separate PDFs to output/ (or its first argument). All identities, account identifiers, contacts and artwork in the energy example are fictional.

Java sources are in src/main/java; templates, JSON, placeholder values and synthetic artwork are in src/main/resources/letters. DejaVu example fonts include their original license at src/main/resources/fonts/LICENSE.txt. The library itself does not bundle these fonts by default.

The example programs are licensed under Apache License 2.0; see LICENSE. The fonts retain their own licence as described above. This download contains example code and resources, not the library implementation.
`);
}

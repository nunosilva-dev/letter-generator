# Documentation website

Astro/Starlight instruction manual for `com.nsdev:letter-generator`. This is a local, statically built site; no hosting account, remote repository or deployment has been created.

## Build and preview

Requires JDK 17+ (JDK 17 for acceptance), Node >=22.22.0 and pnpm 11.25.0. From the repository root:

```sh
./mvnw clean verify
cd website
pnpm install --frozen-lockfile
pnpm verify
pnpm dev
```

Open the Local URL printed by Astro (normally http://localhost:4321). `pnpm preview` serves the production build after `pnpm build`. `pnpm dev` expects generated examples/Javadocs to have been prepared; `pnpm prepare:assets` prepares them independently.

`pnpm verify` runs script tests, copies approved Maven-generated PDF/PNG/public Javadoc assets, builds a complete downloadable consumer example project, checks markup/JSON reference coverage, builds the static website/search index and verifies local links/assets/fragment anchors. Missing Maven outputs produce a concrete preparation error. `dist/` is the root-hosted production output.

To run the downloadable consumer project against the local development artifact, first install it with `./mvnw install`, then build `target/documentation/consumer/pom.xml` with Maven and run its README commands. It uses the artifact and public APIs, rather than a source dependency or test classes.

## Source of truth

- `src/content/docs/`: focused manual, API guides, reference and gallery pages.
- `src/components/`: shared synchronized API tabs, generated model/tag references and previews.
- `src/data/markup-reference.json`: authored descriptions/defaults/context for every supported tag and CSS property; checked against parser vocabulary.
- `scripts/lib/model-reference.mjs`: extracts reachable public record fields/Javadocs/enums/subtype names for the JSON reference.
- `../src/test/resources/documentation/`: canonical complete Java/JSON/HTML quickstarts, compiled/rendered by `DocumentationExamplesTest` and imported directly by the site.
- Existing energy test fixtures feed the four reference-letter inputs, assets and downloadable standalone examples. Their content is fictional.
- `scripts/prepare-assets.mjs`: explicit publication boundary; only approved example assets/source and **public** Javadocs enter generated public directories. Actual library implementation sources are not included in the example ZIP.

Internal plans remain in ignored `../docs/`. `node_modules`, `.astro`, `dist` and generated `public/{api,examples,downloads}` directories are ignored. The website displays the version from the library POM; it currently documents a development snapshot and does not claim artifact availability on Maven Central.

A live editing/rendering playground, multiple-version selector and remote publishing are outside the initial site. The library's Maven sources JAR is a separate distribution setting and is not changed by website preparation.

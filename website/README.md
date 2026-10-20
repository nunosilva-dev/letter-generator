# Documentation website

Astro/Starlight instruction manual for `io.github.nunosilva-dev:letter-generator`. The deployment URL is https://nunosilva-dev.github.io/letter-generator/. The site is static and needs no Java server after it is built.

## Build and preview

Requires JDK 17+ (JDK 17 for acceptance), Node >=22.22.0 and pnpm 11.25.0. From the repository root:

```sh
./mvnw clean verify
cd website
pnpm install --frozen-lockfile
pnpm verify
pnpm dev
```

Open the Local URL printed by Astro (normally http://localhost:4321/letter-generator/). `pnpm preview` serves the production build after `pnpm build`. `pnpm dev` expects generated examples/Javadocs to have been prepared; `pnpm prepare:assets` prepares them independently.

`pnpm verify` runs script tests, copies approved Maven-generated PDF/PNG/public Javadoc assets, builds a complete downloadable consumer example project, checks markup/JSON reference coverage, builds the static website/search index and verifies local links/assets/fragment anchors. The checker rejects links escaping the deployment base, including same-origin absolute URLs. Missing Maven outputs produce a concrete preparation error. `dist/` contains the files to upload; do not wrap them in an additional `letter-generator/` directory.

To run the downloadable consumer project against the local development artifact, first install it with `./mvnw install`, then build `target/documentation/consumer/pom.xml` with Maven and run its README commands. It uses the artifact and public APIs, rather than a source dependency or test classes.

## Automatic GitHub Pages deployment

The repository's **Settings → Pages → Build and deployment → Source** must be **GitHub Actions**. No personal access token or Maven Central credentials are required.

The tracked entry-point workflow is [`.github/workflows/docs.yml`](../.github/workflows/docs.yml), which calls the shared read-only [verification workflow](../.github/workflows/verify.yml):

- Pull requests targeting `main` run the complete verification job and never deploy.
- Pushes to `main`, including merged pull requests, run verification and deploy only after it succeeds.
- Manual runs from the Actions tab verify the selected branch; deployment is allowed only when that branch is `main`.

The `Verify project / Java 17 and documentation` check runs release eligibility tests, formatting, warnings-as-errors compilation, the full Java suite, Checkstyle, strict public Javadocs, website tests, reference checks, the production build, link/asset/anchor checks and all eight standalone consumer examples. It uses JDK 17, Node 24 and the pnpm version declared in `package.json`, with a frozen lockfile. The Maven artifact is installed only in the runner's local repository for consumer checks; this workflow does not publish the library.

Only `website/dist` becomes the Pages artifact. Pages write and OIDC permissions belong to the deployment job; pull request verification has read-only repository access. Actions are pinned to reviewed commit SHAs. Concurrent pull request runs are cancelled when superseded; `main` deployments are allowed to finish.

After merging, follow **Actions → Documentation → Deploy documentation** for deployment status and the resulting URL. The `github-pages` environment can be restricted to `main` under repository environment settings. Require the shared verification check in `main` branch protection after its first successful run; if a rule previously required the standalone job, select its new `Verify project / Java 17 and documentation` name after this refactor.

Maven Central publishing uses a separate tag-triggered workflow with the same verification and a final manual publication step in Central Portal; see [maintainer release instructions](../RELEASING.md).

## Base paths and authored links

`scripts/site.mjs` is the source of truth for the site origin and `/letter-generator` base. Astro uses these settings for assets, canonical URLs, the sitemap and Pagefind search.

Write Markdown links and literal MDX `href`/`src` attributes relative to the site root, for example `/guides/headers/`. The native Sätteri plugin prefixes the base during compilation and leaves fenced code unchanged. Custom Astro components must call `withBase()` for generated local URLs. Starlight prefixes its sidebar/favicon paths itself; route middleware handles hero actions and removes misleading canonical metadata from the 404 page.

If moving to a custom domain or a different repository path, update `site` and `base` in `scripts/site.mjs`, then run `pnpm verify` and check navigation/search with `pnpm preview` before deploying.

## Source of truth

- `src/content/docs/`: focused manual, API guides, reference and gallery pages.
- `src/components/`: shared synchronized API tabs, generated model/tag references and previews.
- `src/data/markup-reference.json`: authored descriptions/defaults/context for every supported tag and CSS property; checked against parser vocabulary.
- `scripts/lib/model-reference.mjs`: extracts reachable public record fields/Javadocs/enums/subtype names for the JSON reference.
- `../src/test/resources/documentation/`: canonical complete Java/JSON/HTML quickstarts, compiled/rendered by `DocumentationExamplesTest` and imported directly by the site.
- Existing energy test fixtures feed the four reference-letter inputs, assets and downloadable standalone examples. Their content is fictional.
- `scripts/prepare-assets.mjs`: explicit publication boundary; only approved example assets/source and **public** Javadocs enter generated public directories. Actual library implementation sources are not included in the example ZIP.

Internal plans remain in ignored `../docs/`. `node_modules`, `.astro`, `dist`, generated `public/{api,examples,downloads}` directories and `public/LICENSE.txt` are ignored. The website displays the version from the library POM; it does not claim artifact availability on Maven Central.

A live editing/rendering playground and multiple-version selector are outside the current site. The library's Maven sources JAR is a separate distribution setting and is not changed by website preparation.

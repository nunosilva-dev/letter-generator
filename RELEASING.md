# Publishing a release

Letter Generator publishes to Maven Central as `io.github.nunosilva-dev:letter-generator`.
The `release` Maven profile attaches signatures and uses Sonatype's Central Publisher
Portal. Normal builds and Pages deployments do not activate this profile.

## One-time setup

Sign in to [Central Portal](https://central.sonatype.com/) and verify the
`io.github.nunosilva-dev` namespace. Generate a Portal user token; its generated
username/password are separate from GitHub credentials.

Configure these repository secrets under **Settings → Secrets and variables → Actions**:

| Secret | Value |
| --- | --- |
| `CENTRAL_USERNAME` | Central Portal token username |
| `CENTRAL_TOKEN` | Central Portal token password |
| `GPG_PRIVATE_KEY` | Entire ASCII-armoured private signing key, including BEGIN/END lines |
| `GPG_PASSPHRASE` | Signing key's passphrase |

The configured public signing fingerprint is
`EBD500D0F60FEF497CBC62091CE8424378E708D5`. Publish the public key to a
[Sonatype-supported keyserver](https://central.sonatype.org/publish/requirements/gpg/#distributing-your-public-key).
Keep the private key, passphrase and revocation certificate backed up securely;
never commit them. When renewing a key's expiration, republish its public key and
update the exported private key secret. When rotating keys, also update
`gpg.keyname` in the release profile.

## Prepare the release

1. Update the POM version and any authored version examples that must change.
2. Merge the release configuration and all intended release changes into `main`
   through the usual pull request checks.
3. Confirm the release commit has passed **Documentation → Verify project →
   Java 17 and documentation**. This includes Java 17 tests, formatting,
   Checkstyle, strict Javadocs, release eligibility tests, documentation tests,
   reference/link checks and all eight standalone consumer examples.
4. Tag the exact intended commit after updating your local `main`:

   ```sh
   git switch main
   git pull --ff-only origin main
   git tag -a v1.0.0 -m "Release 1.0.0"
   git push origin v1.0.0
   ```

   Use the appropriate version for later releases. The tag must use stable
   `vX.Y.Z` format, match the POM version exactly, and identify a commit already
   on `origin/main`. An annotated or lightweight tag works. Snapshot/prerelease
   tags are intentionally rejected by this release workflow.

## What the release workflow does

[`.github/workflows/release.yml`](.github/workflows/release.yml) runs for pushed
`v*` tags. It checks eligibility without secrets, then invokes the same read-only
[verification workflow](.github/workflows/verify.yml) as Pages. The release call
does not upload or deploy a Pages site.

After verification passes, a separate job imports the signing key into the
temporary keyring managed by `actions/setup-java`. It writes Maven server
configuration referring to credential environment variables, builds with Java 17,
signs the binary, sources and Javadoc JARs plus their POM, and runs:

```sh
./mvnw -B -ntp -Prelease clean deploy
```

The Central plugin generates checksums, uploads the bundle and waits for
validation. **`autoPublish` is explicitly `false`: workflow success means
validation succeeded, not that the release is publicly available.**

Open [Central Portal deployments](https://central.sonatype.com/publishing/deployments),
review the deployment named with the artifact's full coordinates and click
**Publish** when ready. Confirm it reaches `PUBLISHED` and that a clean consumer
build can resolve the artifact from Maven Central before announcing availability.

Central releases are immutable: a published version cannot be replaced. Fix a
published release with a new version. If upload or validation fails, inspect the
workflow/Portal details and remove any unwanted unpublished deployment before
retrying. Re-run the workflow for the same unchanged tag when appropriate; do not
move a released tag to a different commit.

## Local dry run without uploading

With the signing key installed locally and JDK 17 selected, read the passphrase
without placing it in shell history:

```bash
read -r -s -p 'GPG passphrase: ' GPG_PASSPHRASE
printf '\n'
export GPG_PASSPHRASE
./mvnw -B -ntp -Prelease -Dcentral.skipUpload=true clean deploy
unset GPG_PASSPHRASE
```

This builds and signs the real release bundle at
`target/central-publishing/central-bundle.zip` without uploading it. No Central
token is needed. Dependency resolution can still access Maven repositories.
For an isolated test key, override the public selection with
`-Dgpg.keyname=TEST_KEY_FINGERPRINT` and use a separate `GNUPGHOME`.

For local upload, Maven's `settings.xml` needs a server named `central` whose
username/password refer to `${env.CENTRAL_USERNAME}` and `${env.CENTRAL_TOKEN}`.
Keep that file and all credentials outside this repository. Consult
[Sonatype's Maven publishing documentation](https://central.sonatype.org/publish/publish-portal-maven/)
for Portal states and troubleshooting.

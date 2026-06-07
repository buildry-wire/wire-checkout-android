# Releasing wire-checkout-android (Maven Central: `mn.wire:wire-checkout-android`)

Releases publish the Android library AAR to **Maven Central via the Sonatype Central Portal**
using a Central Portal token plus a GPG signature. Maven Central does **not** support OIDC trusted
publishing, so the token + key path below is required. The release runs from a tag push (`v*`).

## One-time setup (maintainer)

### 1. Claim the `mn.wire` namespace
1. Sign in at https://central.sonatype.com.
2. Go to **Namespaces** → **Add Namespace** and enter `mn.wire`.
3. Verify ownership of the `wire.mn` domain via the DNS TXT record the portal shows.

(If the `mn.wire` namespace is already verified for the other Wire SDKs, this step is done.)

### 2. Generate a Central Portal token
1. In the Central Portal, open **Account** → **Generate User Token**.
2. This yields a **username** and **password** pair (the token), used for the upload API.

### 3. Generate a GPG signing key
```bash
gpg --gen-key                      # use a real name + email; set a passphrase
gpg --list-secret-keys --keyid-format=long
gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>
gpg --armor --export-secret-keys <KEY_ID> > signing-key.asc
```

### 4. Add the GitHub repository secrets
**Settings → Secrets and variables → Actions → New repository secret.** Add all four:

| Secret name | Value |
|---|---|
| `CENTRAL_PORTAL_USERNAME` | Central Portal token **username** (from step 2) |
| `CENTRAL_PORTAL_PASSWORD` | Central Portal token **password** (from step 2) |
| `SIGNING_KEY` | full contents of `signing-key.asc` (ASCII-armored private key) |
| `SIGNING_PASSWORD` | the passphrase for that GPG key |

These map in `release.yml` to the Gradle properties the publish plugin reads:
`mavenCentralUsername`, `mavenCentralPassword`, `signingInMemoryKey`, `signingInMemoryKeyPassword`.

> Never commit any token or key. `signing-key.asc` stays out of git (see `.gitignore`).

## Cut a release
1. Bump `version` in `wire-checkout-android/build.gradle.kts` and the `1.0.0` references in
   `README.md`.
2. Move changelog items under `## [x.y.z] - YYYY-MM-DD` in `CHANGELOG.md`.
3. Commit on `main`, then tag (tag must equal the project version, without the `v`):
   ```bash
   git tag vX.Y.Z
   git push origin vX.Y.Z
   ```

The `release` workflow verifies the tag matches the version, builds + tests, signs, uploads the
bundle to the Central Portal with `automaticRelease = true`, and creates a GitHub Release.

## Verify
```bash
# Once Central finishes processing (a few minutes), it appears on Maven Central:
# https://repo1.maven.org/maven2/mn/wire/wire-checkout-android/X.Y.Z/
```

## Note on the Gradle wrapper jar
`gradle/wrapper/gradle-wrapper.jar` is a binary that must be generated once with a local Gradle and
**could not be produced on the authoring machine** (no Java/Gradle installed). It is therefore not
committed. Generate it once:
```bash
gradle wrapper --gradle-version 8.10.2
git add gradle/wrapper/gradle-wrapper.jar && git commit -m "build: add gradle wrapper jar"
```
Both CI workflows regenerate it automatically if missing (`gradle wrapper` step), so builds work
either way.

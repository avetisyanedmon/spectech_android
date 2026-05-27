# 03 — CI / CD

## Recommended: GitHub Actions

iOS doesn't currently have CI in this repo. The Android port should land
with one from the start.

```yaml
# .github/workflows/android.yml
name: Android CI

on:
  push:
    branches: [main]
  pull_request:

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Decrypt google-services.json
        run: |
          echo "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" \
            | base64 --decode > spectech_android/app/google-services.json

      - name: Lint
        working-directory: spectech_android
        run: ./gradlew lint

      - name: Unit tests
        working-directory: spectech_android
        env:
          SPECTECH_API_CLIENT_SECRET: ${{ secrets.SPECTECH_API_CLIENT_SECRET }}
        run: ./gradlew test

      - name: Build debug APK
        working-directory: spectech_android
        env:
          SPECTECH_API_CLIENT_SECRET: ${{ secrets.SPECTECH_API_CLIENT_SECRET }}
        run: ./gradlew assembleDebug

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: debug-apk
          path: spectech_android/app/build/outputs/apk/debug/app-debug.apk

  release:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - uses: gradle/actions/setup-gradle@v4

      - name: Decrypt release keystore
        run: |
          echo "${{ secrets.RELEASE_KEYSTORE_BASE64 }}" \
            | base64 --decode > spectech_android/release.keystore

      - name: Decrypt google-services.json
        run: |
          echo "${{ secrets.GOOGLE_SERVICES_JSON_BASE64 }}" \
            | base64 --decode > spectech_android/app/google-services.json

      - name: Build release AAB
        working-directory: spectech_android
        env:
          SPECTECH_API_CLIENT_SECRET: ${{ secrets.SPECTECH_API_CLIENT_SECRET }}
          KEYSTORE_PATH: ../release.keystore
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
        run: ./gradlew bundleRelease

      - name: Upload to Play Console (internal track)
        uses: r0adkll/upload-google-play@v1
        with:
          serviceAccountJsonPlainText: ${{ secrets.PLAY_SERVICE_ACCOUNT_JSON }}
          packageName: ru.spectech.android
          releaseFiles: spectech_android/app/build/outputs/bundle/release/app-release.aab
          track: internal
          status: completed
```

## Secrets to configure in GitHub

| Secret name | Purpose |
|---|---|
| `SPECTECH_API_CLIENT_SECRET` | HMAC client secret (`AppConfiguration.clientSecret`) |
| `GOOGLE_SERVICES_JSON_BASE64` | Base64-encoded `google-services.json` (Firebase config) |
| `RELEASE_KEYSTORE_BASE64` | Base64-encoded release signing keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing key alias |
| `KEY_PASSWORD` | Signing key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Play Console service-account credentials |

## Branch protection

Enforce on the `main` branch:
- ✅ Require PR before merging
- ✅ Require status checks to pass (`build` job)
- ✅ Require linear history (squash & merge only)
- ✅ Require branches to be up to date before merging

## PR checks (recommended)

Add a separate workflow for PRs that runs faster:

```yaml
name: PR checks
on: pull_request

jobs:
  fast-checks:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with: { distribution: 'temurin', java-version: '17' }
      - uses: gradle/actions/setup-gradle@v4

      - name: Detekt + lint
        working-directory: spectech_android
        run: ./gradlew detekt ktlintCheck

      - name: Unit tests (fast modules only)
        working-directory: spectech_android
        run: ./gradlew :core:domain:test :core:network:test :core:data:test
```

## Instrumented tests (optional)

Add a Firebase Test Lab job that runs Espresso / Compose UI tests on a real
device:

```yaml
- name: Run instrumented tests on Firebase Test Lab
  uses: asadmansr/Firebase-Test-Lab-Action@v1.0
  with:
    arg-line: "--type instrumentation --app app-debug.apk --test app-debug-androidTest.apk --device model=Pixel2,version=29"
    service-account: ${{ secrets.FIREBASE_TEST_LAB_SERVICE_ACCOUNT }}
```

Skip this if Test Lab cost is a concern; local emulator-based testing
covers the same surface for most CI needs.

## Code coverage

Use JaCoCo. Add to root `build.gradle.kts`:

```kotlin
subprojects {
    plugins.withId("org.gradle.jacoco") {
        the<org.gradle.testing.jacoco.plugins.JacocoPluginExtension>().apply {
            toolVersion = "0.8.12"
        }
    }
}
```

Configure a multi-module coverage report task that aggregates per-module
reports — see Gradle JaCoCo documentation for the boilerplate.

Upload to Codecov:

```yaml
- name: Upload coverage
  uses: codecov/codecov-action@v4
  with:
    files: spectech_android/build/reports/jacoco/jacocoFullReport/jacocoFullReport.xml
    token: ${{ secrets.CODECOV_TOKEN }}
```

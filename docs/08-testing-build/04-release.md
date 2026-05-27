# 04 — Release

## Signing config

```kotlin
// app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH") ?: "release.keystore"
            storeFile = file(keystorePath)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}
```

Generate the keystore once:

```bash
keytool -genkey -v -keystore release.keystore -keyalg RSA -keysize 4096 \
    -validity 25000 -alias spectech \
    -dname "CN=SpecTech, OU=Mobile, O=SpecTech, L=Moscow, S=Moscow, C=RU"
```

Store the password in a password manager. Lose it and you cannot push
updates to existing Play Store users — **back this up to two separate
secure locations.**

## Play App Signing

Strongly recommended: enroll in Play App Signing so Google holds the real
release key in escrow. You upload an "upload key" and Google re-signs each
artifact. If you ever lose your upload key, Google can reset it without
locking you out of the listing.

Steps:
1. Generate an upload key (the keystore above).
2. Create the app on Play Console.
3. Enroll in Play App Signing — upload the existing key OR let Google
   generate a fresh release key.

## Versioning

| Build | `versionCode` | `versionName` |
|---|---|---|
| First release | 1 | `1.0.0` |
| Patch | 2 | `1.0.1` |
| Minor | 3 | `1.1.0` |

Bump `versionCode` by 1 for every Play Store upload (even if `versionName`
is unchanged — internal beta releases share `versionName` but need unique
`versionCode`).

Automate with a Gradle task that reads from git:

```kotlin
defaultConfig {
    versionCode = providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
    }.standardOutput.asText.get().trim().toInt()
    versionName = "1.0.0"
}
```

## Pre-launch checklist

Before promoting to production:

- ☐ All localized strings have Russian translations (`./gradlew lint` —
      `MissingTranslation` clean)
- ☐ `compileSdk` is the current Play Store requirement (35 for new uploads
      in 2025)
- ☐ ProGuard rules tested against a real release APK install path
- ☐ Privacy Policy URL set in Play Console (Profile screen URLs match)
- ☐ Data safety form filled out: phone number collected, email collected,
      device ID for FCM, no third-party sharing beyond YooKassa payment URL
- ☐ Permissions justified: `POST_NOTIFICATIONS` for push, no camera or
      location unless required (the photo picker doesn't need any)
- ☐ Screenshots captured for phone + tablet (at minimum, phone hero shot)
- ☐ App icon and feature graphic uploaded to Play Console
- ☐ Russian localization variants of store listing
- ☐ Internal test track works end-to-end (sign-in, browse, submit bid,
      deposit flow if applicable, push receive)

## Rollout strategy

1. **Internal testing** (Play Console) — backend team + designers, ~5 testers.
2. **Closed testing** — invite contractors / customers from the iOS user
   base, ~50 testers, 1 week.
3. **Open testing** (optional) — public sign-up testing track.
4. **Production**, staged rollout:
   - Day 1: 5%
   - Day 3: 20% (assuming no crash spikes)
   - Day 7: 50%
   - Day 10: 100%

Watch Play Console vitals dashboards (ANRs, crashes, slow cold starts)
between each step. Halt if crash-free sessions drop below 99.5%.

## Crash & vitals monitoring

If Firebase Crashlytics is enabled (see [07-infrastructure/05-logging.md](../07-infrastructure/05-logging.md)),
set Crashlytics alerts:
- Velocity alert: 0.5% crash rate
- Stability alert: any new fatal in the latest version

Play Console Android Vitals also flags ANRs and excessive wakelocks
automatically — no extra config needed.

## Release notes template

Use Play Console's per-language release-notes field. Keep notes
user-focused, not change-log-y.

```
1.0.0
What's new:
- SpecTech is now available on Android.
- Sign in with your phone, browse marketplace orders, submit bids, manage your garage from your phone.
- Push notifications when a new order matches your saved filter.
```

Russian:
```
1.0.0
Что нового:
- Приложение SpecTech теперь доступно на Android.
- Войдите по номеру телефона, просматривайте заказы, подавайте предложения, управляйте своим гаражом с телефона.
- Push-уведомления, когда появится новый заказ под ваш сохранённый фильтр.
```

## Bug-fix releases

For urgent fixes:
1. Branch from the prod tag: `git checkout -b hotfix/1.0.1 v1.0.0`
2. Apply minimal fix
3. Bump `versionCode` and `versionName`
4. Run CI; assemble release bundle
5. Skip closed testing for true emergencies; use the **Halted rollout** +
   higher rollout percentage approach from the Play Console "Manage
   releases" page.

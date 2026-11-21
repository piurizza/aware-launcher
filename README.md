# Aware

A minimalist Android launcher that shows how much time you spend in each app today.

No icons, no widgets, no feeds. A clock, today's screen time compared with yesterday, a search
field and a plain list of your apps, each with the time spent and the number of times you opened it
today. The point is to make you pause for a second before opening something.

```
14:32
Saturday, 21 November
2h 14m today · 20m less than this time yesterday

Search

Messages                    8m · 12×
Camera
Instagram            47m / 30m · 23×
Maps                        12m · 2×
Old Game                unopened 45d
```

## Design goals

- **Fast.** A single framework `Activity` and a `ListView`. No Jetpack Compose, no AndroidX, no
  third-party libraries: the release APK is tens of kilobytes and starts instantly.
- **Light on resources.** No background service. Time spent comes from Android's own usage log
  (`UsageStatsManager`), read only when the home screen is shown. Pure black background for OLED.
- **Private.** No internet permission, no analytics, no backups. Nothing leaves the phone. Pins,
  hidden apps and limits live in a small private settings file, so they reset if Aware is reinstalled.

## Using it

1. Install the APK from the [latest release](../../releases/latest).
2. Press Home and choose **Aware** as the default home app (or Settings → Apps → Default apps → Home app).
3. Tap the line under the date and allow **Usage access** for Aware. Without it the launcher still
   works, it just can't show time spent.

- **Tap** an app to open it. **Type** to filter; the keyboard's **Go** key opens the first match.
- **Tap the summary line** for the last 7 days as a small bar chart, with your daily average.
- **Swipe down** anywhere on the home screen for notifications.
- **Home** while already home clears the search and scrolls back to the top.

**Long-press** an app for its menu:

| Option | What it does |
|---|---|
| Pin to top | Keeps the app at the top of the list. |
| Hide | Removes it from the list; it still appears in search, marked `· hidden`. |
| Mark as distracting | Opening it first shows a **mindful pause**: today's time and opens, and an Open button that unlocks after 3 seconds. |
| Daily limit… | Shows usage against the limit (`47m / 30m`), turns amber once reached, and adds the pause from then on. Nothing is ever blocked. |
| App info | Android's app info screen (uninstall, permissions…). |

Apps you haven't opened in 30+ days show `unopened 45d`: a hint they could go.

Usage is per Android profile, so work-profile apps (marked `· work`) show no times and have no limits.

Requires Android 10 (API 29) or newer.

## Building

Requirements: JDK 17 and the Android SDK (Android Studio installs both).

```sh
./gradlew assembleDebug          # app/build/outputs/apk/debug/
./gradlew installDebug           # install on a connected device
```

Debug builds use the application ID `app.aware.launcher.debug`, so they install alongside a release build.
They are signed with the public `app/debug.keystore`, so each CI debug build installs over the previous one.

## Quality checks

Every push and pull request runs the [CI workflow](.github/workflows/ci.yml). Run the same checks locally with:

```sh
./gradlew ktlintCheck detekt lintRelease testDebugUnitTest assembleRelease
```

| Check | What it enforces |
|---|---|
| `ktlintCheck` | Formatting (Android Studio style, see `.editorconfig`). Fix with `./gradlew ktlintFormat`. |
| `detekt` | Static analysis for Kotlin: complexity, bugs, style. Config in `config/detekt/detekt.yml`. |
| `lintRelease` | Android Lint with every warning treated as an error. |
| Kotlin compiler | Kotlin's type checker, with `allWarningsAsErrors`, so deprecations and unchecked casts fail the build. |
| `testDebugUnitTest` | JVM unit tests for the time accounting and formatting. |
| `assembleRelease` | Full R8 shrink of the release build. |
| actionlint | Lints the GitHub Actions workflows themselves. |

detekt, ktlint and Android Lint findings are also uploaded to GitHub code scanning, so they appear
inline on pull requests. Dependabot opens weekly grouped updates for Gradle dependencies and Actions.

## Releases

Releases are cut by pushing a version tag. The [release workflow](.github/workflows/release.yml)
re-runs all of CI, builds a signed APK, verifies the signature, attaches the APK and a
`SHA256SUMS` file to a GitHub release with generated notes, and records a build provenance attestation.

### One-time setup

Create the signing key and store it as secrets in a `release` environment (needs the
[GitHub CLI](https://cli.github.com/), logged in):

```sh
./scripts/create-release-key.sh
```

Then move `aware-release.jks` somewhere safe outside the repo and back it up. If the key is lost,
existing users have to uninstall Aware before they can install updates.

Optionally, in **Settings → Environments → release**, add yourself as a required reviewer so
every release waits for your approval.

### Cutting a release

```sh
git tag v1.0.0
git push origin v1.0.0
```

Tags must be plain `vMAJOR.MINOR.PATCH`. The Android `versionCode` is derived from the tag
(`1.2.3` → `1002003`), so every release upgrades cleanly over the previous one.

Release notes are generated from merged pull requests and grouped by label (`enhancement`, `bug`,
`dependencies`); see `.github/release.yml`.

To check that a downloaded APK was built by this repository's workflow:

```sh
gh attestation verify aware-1.0.0.apk --repo <owner>/aware-launcher
```

## Project layout

```
app/src/main/java/app/aware/launcher/
  MainActivity.kt          home screen
  apps/AppRepository.kt    installed apps + today's usage
  apps/AppAdapter.kt       the text list
  usage/UsageTracker.kt    reads the system usage log
  usage/UsageCalculator.kt turns events into time per app (pure Kotlin, unit tested)
  usage/DurationFormat.kt  "1h 05m"
```

## License

[MIT](LICENSE)

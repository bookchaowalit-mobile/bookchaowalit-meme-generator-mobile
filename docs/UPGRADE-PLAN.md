# Upgrade plan

## Current state

Score: 5/10 (was 4/10) — tested pure-Kotlin domain core and honest CI; the
Compose UI is still a placeholder and the Android build is unverified locally.

## Backlog

- P0: Confirm the `android` CI job is green (AGP 8.5 + Kotlin 2.0.21 Compose
  plugin + material dependency were fixed statically, without an Android SDK).
- P0: Replace the placeholder Home/Explore screens with UI backed by `core`
  (ViewModel + state holder calling the `core` API).
- P1: Render captions onto a Bitmap/Canvas with outlined text using `core` layouts; save via MediaStore.
- P1: Bundle a small set of license-clear templates; allow picking a photo from the gallery.
- P2: Compose UI tests (`androidx.compose.ui:ui-test-junit4`) for the main flow.
- P2: Pin `distributionSha256Sum` in `gradle-wrapper.properties`.

## Done in this pass (pass 1)

- Added `core/` (included Gradle build) with the app's domain logic and 7 unit tests.
- Committed the Gradle wrapper (8.10.2).
- Fixed Kotlin 2.0 Compose setup: `org.jetbrains.kotlin.plugin.compose`
  replaces the removed `composeOptions.kotlinCompilerExtensionVersion`; Java/Kotlin
  target 17; added `com.google.android.material` for the manifest theme.
- CI: removed `|| true`; separate `core` and `android` jobs.
- README now states what is verified and what is not.

## Done in this pass (pass 2)

- Fixed: hard-splitting a long word could cut an emoji (surrogate pair) in half, producing broken glyphs.
- Fixed: templates shorter than 4px (or narrower than 2px) crashed `layout`; captions now fall back to the minimum font. `fit` rejects `maxFontPx < MIN_FONT_PX` instead of silently returning a larger font.
- 8 more edge-case tests (15 total): surrogate pairs, repeated spaces, Unicode upper-casing, argument validation, monotonic fit, file-name bounds. Verified with `./gradlew -p core test --offline`.
- Still not verified locally: the Compose `app/` (no Android SDK; dl.google.com is blocked here).

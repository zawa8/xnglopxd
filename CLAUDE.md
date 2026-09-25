# xnglopxd -- htr-xnglo notepad (Android)

An Android notepad app with a font picker and htrlib
(https://github.com/zawa8/htrlib) -integrated transliteration (xi38 full
romanization, and u*38 semi-transliteration that keeps native-script
letters and only converts marks -- see htrlib's own README/CLAUDE.md for
what those mean). Supports 6 scripts so far: devanagari, gurmukhi
(punjabi), gujarati, oriya, kannada, sinhala -- see "What's NOT done
yet" for the other 4 (bengali, tamil, telugu, malayalam), which htrlib
itself doesn't have verified per-script data for yet either.

## Layout
- `xnglo_core/` -- **plain Kotlin/JVM module, no Android dependency.**
  Hand-ported from htrlib's TypeScript (`src/hsciistr/u10_to_xi52.ts` +
  `xnglo_post.ts`). 6 scripts ported: devanagari (`U1Map.kt`), gurmukhi
  (`u3_map.kt`), gujarati (`u4_map.kt`), oriya (`u5_map.kt`), kannada
  (`u8_map.kt`), sinhala (`u10_map.kt`) -- the ones htrlib itself has
  verified per-script data for. Dispatch across them lives in
  `XngloCore.kt`'s `ScriptTable`/`SCRIPTS`/`tableFor()` -- same structure
  as `xnglonpp-ext`'s (the Notepad++ plugin) `src/xnglo_core/core.cpp`;
  the two should behave identically. `toU38()` (semi-transliteration)
  only supports the 5 ISCII-aligned scripts (not sinhala -- see
  `ScriptTable.isciiAligned`). This is the part that's actually been
  **compiled and run** in this repo's sandbox (see below) -- everything
  under `app/` has not.
- `app/` -- the actual Android app: Jetpack Compose UI (`MainActivity.kt`
  has a `NotepadScreen` composable), a font-picker menu (currently just
  the built-in `FontFamily` choices -- Default/Serif/SansSerif/
  Monospace/Cursive; no custom `.ttf` bundled yet, see below), and a
  transliterate menu that runs `XngloCore.toXi38`/`toU38` over the
  current text selection (or the whole document if nothing's selected).

## What's actually been verified here
This sandbox has no Android SDK, no emulator, and no real Gradle Android
build -- `app/` has **never been compiled**, not even a syntax check
beyond careful manual writing. Treat every file under `app/` as a first
draft.

What HAS been built and run, with a bare `kotlinc` (no Gradle, no
Android tooling):
```
cd xnglo_core
kotlinc src/main/kotlin/xnglo/*.kt \
  src/test/kotlin/xnglo/CoreTest.kt -include-runtime -d /tmp/xnglotest.jar
java -jar /tmp/xnglotest.jar
```
This passes the same assertions as htrlib's own
`__tests__/hsciistr.test.ts` (devanagari path) and `xnglonpp-ext`'s
`tests/core_test.cpp` -- all three should keep agreeing.

## What's NOT done yet
- 4 scripts still unported: bengali, tamil, telugu, malayalam -- htrlib
  itself doesn't have verified per-script data for these yet (its
  u2/u6/u7/u9_map.ts are still devanagari-derived placeholders), so
  there's nothing correct to port until that happens there first. Adding
  one once it is: pull that script's `uN_map.ts` array out of htrlib
  (same way `U1Map.kt`/`u3_map.kt`/etc were generated -- a straight
  literal copy of the raw value array) into a new file, add its
  `_BASE`/`_VIRAMA_OFFSET` constants and an entry in `XngloCore.kt`'s
  `SCRIPTS` list.
- `composeNukta()`/`dropMalformedVowelMatra()` in `XngloCore.kt` are
  still devanagari-only (match literal devanagari codepoints) -- they're
  harmless no-ops on the other 5 scripts' text, but don't do the
  equivalent cleanup for THEIR nukta-decomposition or malformed-vowel-
  matra quirks. The oriya test uses the precomposed nukta form for
  exactly this reason (decomposed ଡ଼ currently reads as plain ଡ, same
  class of bug documented in htrlib's own u5_map.ts commit history).
- No Gradle wrapper (`gradlew`/`gradle-wrapper.jar`) checked in -- this
  sandbox has no network access to fetch the Gradle distribution, and
  the wrapper jar is a binary that shouldn't be hand-typed. Opening this
  project in Android Studio should generate one automatically; otherwise
  run `gradle wrapper` once on a machine with Gradle installed.
- No launcher icon (`android:icon` removed from `AndroidManifest.xml`
  rather than pointing at a `@mipmap/ic_launcher` that doesn't exist) --
  add one under `app/src/main/res/mipmap-*/` when there's real artwork.
- Font picker only offers Android's built-in generic font families. To
  add a real Devanagari-friendly display face: drop a `.ttf`/`.otf`
  under `app/src/main/res/font/`, declare it as a `FontFamily(Font(R.font.xxx))`,
  and add it to `FONT_CHOICES` in `MainActivity.kt`.
- No persistence (open/save files) -- text lives only in the in-memory
  `TextFieldValue` for now.
- No unit/instrumented tests for `app/` itself (would need Robolectric or
  a real device/emulator, neither available here).

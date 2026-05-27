# 04 — Localization

iOS source: `SpecTechIOS/Resources/Localizable.xcstrings` (4670 lines).

## Migration plan

1. Generate `strings.xml` (English) by walking the `.xcstrings` JSON and
   emitting one `<string name="...">` per entry. Use the iOS key as the
   Android key, sanitized: replace dots with underscores, lowercase, replace
   spaces with underscores.
   - `unit.hour` → `unit_hour`
   - `Sign in to view and manage your profile.` → use a stable slug like
     `profile_signin_prompt_message` and put the English text inside.
2. Generate `values-ru/strings.xml` from the Russian variant in the
   xcstrings entries.
3. For strings that aren't in `.xcstrings` (the iOS source hardcodes many
   in Swift like `"Welcome Back"`), grep the source for `String(localized:)`
   and `Text(…)` and create stable keys for them.

## Tooling

A 30-line Kotlin or Python script can do step 1+2 automatically. Pseudo-code:

```python
import json
data = json.load(open('Localizable.xcstrings'))
strings = data['strings']
en_xml = ['<resources>']
ru_xml = ['<resources>']
for key, entry in strings.items():
    slug = sanitize(key)
    en = entry['localizations'].get('en', {}).get('stringUnit', {}).get('value', key)
    ru = entry['localizations'].get('ru', {}).get('stringUnit', {}).get('value', key)
    en_xml.append(f'    <string name="{slug}">{escape(en)}</string>')
    ru_xml.append(f'    <string name="{slug}">{escape(ru)}</string>')
en_xml.append('</resources>')
ru_xml.append('</resources>')
```

XML escape rules:
- `'` → `\'`
- `"` → `\"`
- `&` → `&amp;`
- `<` → `&lt;`
- `>` → `&gt;`
- `\n` → `\\n` (string-resource style)
- Wrap strings containing leading/trailing whitespace or punctuation that
  Android trims by default in double quotes.

## Per-app locale

Android 13+ exposes per-app language preference natively. Earlier versions
use `AppCompatDelegate.setApplicationLocales(...)`. Both paths converge
through the AndroidX `LocaleListCompat` API — call it from the Profile
language picker and persist the choice in `LocalProfile.language`.

Add `xml/locales_config.xml` (see [09-profile.md](../05-features/09-profile.md))
to declare the supported locales.

## Plurals

iOS has minimal plural usage. Where it exists (e.g. "5 bids" / "1 bid"), use
Android `<plurals>` resources:

```xml
<plurals name="bid_count">
    <item quantity="one">%d bid</item>
    <item quantity="few">%d bids</item>
    <item quantity="many">%d bids</item>
    <item quantity="other">%d bids</item>
</plurals>
```

Russian has the most complex plural rules — use the `one/few/many/other`
quantities (see CLDR table).

## Backend display strings

The Russian display strings on enums (`backendCreateValue`) are **not** UI
strings — they go in HTTP request bodies. Keep them inside the Kotlin enum
definitions, NOT in `strings.xml`. They never change with the UI language.

UI titles for enums (the human-readable label the user sees) DO go in
`strings.xml`:

```xml
<!-- values/strings.xml -->
<string name="cat_dump_truck">Dump truck</string>
<string name="unit_hour">per hour</string>
<string name="payment_cash">Cash</string>
<string name="status_open">Open</string>

<!-- values-ru/strings.xml -->
<string name="cat_dump_truck">Самосвал</string>
<string name="unit_hour">за час</string>
<string name="payment_cash">Наличные</string>
<string name="status_open">Открыт</string>
```

## RTL

No RTL languages in scope (English + Russian both LTR). No work needed.

## Stale-key audit

After the initial generation, run a one-time audit:
1. Compare the set of slugs in `values/strings.xml` against
   `values-ru/strings.xml` — flag missing translations.
2. Search the Compose source for hard-coded English literals (the `Text("...")`
   pattern with a raw string) and extract them.

A small CI check (`./gradlew lint`) catches most missing-translation cases
via the `MissingTranslation` lint check.

# Translation notes

The app ships its interface in 34 languages — the same set as the landing page.
Every locale is a complete file: `composeApp/src/commonMain/composeResources/values-<code>/strings.xml`
carries all 111 strings and the `challenge_habit_count` plurals block, and
`composeApp/src/androidMain/res/values-<code>/strings.xml` carries the widget
picker's description.

The translations were produced by a language model against the landing page's
already-reviewed copy, constrained by `LocaleCompletenessTest`, and **not**
reviewed by native speakers. This file records what to trust and what to look at
first.

**Read the landing's notes alongside this one.** `landing/TRANSLATION_NOTES.md`
holds the per-language decisions the app inherits: the noun each language uses
for the ✓ mark, the grid and cell words, the "back pages" idiom, the restart
vocabulary, and the gender agreements those choices force. The app and the site
have to speak identically; where this file is silent, the landing's decision
stands.

## Confidence at a glance

| Locale | Language | Confidence | ✓ in running copy | The notebook |
|---|---|---|---|---|
| en | English | source | tick | notebook |
| sk | Slovenčina | native-reviewed | značka | zápisník |
| cs | Čeština | high | značka | zápisník |
| pl | Polski | high | znak | zeszyt |
| de | Deutsch | high | Häkchen | Heft |
| nl | Nederlands | medium-high | vinkje | schrift |
| fr | Français | high | coche | carnet |
| it | Italiano | high | spunta | quaderno |
| es | Español | high | marca | cuaderno |
| pt | Português | high | visto | caderno |
| ca | Català | medium-high | marca (f.) | quadern |
| ro | Română | high | bifă | caiet |
| da | Dansk | high | flueben | hæfte |
| sv | Svenska | high | bock | häfte |
| nb | Norsk bokmål | high | hake | hefte |
| is | Íslenska | **needs native review** | hak / hök | stílabók |
| fi | Suomi | medium-high | rasti | vihko |
| et | Eesti | **medium — native pass wanted** | linnuke | vihik |
| hu | Magyar | high | pipa | füzet |
| lv | Latviešu | medium | atzīme | burtnīca |
| lt | Lietuvių | medium-high | varnelė | sąsiuvinis |
| el | Ελληνικά | high | σημάδι | τετράδιο |
| tr | Türkçe | high | işaret | defter |
| sq | Shqip | medium-high | shenjë | fletore |
| mt | Malti | **needs native review** | marka | kwadernu |
| sl | Slovenščina | medium-high | kljukica | zvezek |
| hr | Hrvatski | high | kvačica | bilježnica |
| bs | Bosanski | high | kvačica | bilježnica |
| sr | Српски | **medium — native pass wanted** | квачица | свеска |
| bg | Български | high | отметка | тетрадка |
| mk | Македонски | **medium — native pass wanted** | квачка | тетратка |
| uk | Українська | high | позначка | зошит |
| be | Беларуская | **needs native review** | знак | сшытак |
| ru | Русский | high | отметка | тетрадь |

Before launch, get a native pass on **is, mt, be, sr and mk**; **et** is the next
one down. That is the same shortlist the landing carries, for the same reasons.

**The ✓ column is not the landing's ✓ column.** The landing table records the
noun a reader *pronounces* when the tagline puts a numeral in front of the glyph
(*fajka*, *ptaszek*, *галочка*). The app has no tagline; it has running prose,
where several of those languages reach for a different, plainer noun — Slovak
and Czech write *značka* while still saying *odfajknúť*, Polish writes *znak*,
Ukrainian *позначка*, Belarusian *знак*, Latvian *atzīme*, Turkish *işaret*,
Greek *σημάδι*. Both files are right about their own sentence; neither
contradicts the other.

## App-specific vocabulary

Four words the app needs and the landing never had to choose:

- **notebook** — the whole record: the attempt you are on plus every archived
  one. It carries the export/import strings and the two backup errors. Chosen
  per language in the table above; the physical school-exercise-book word won
  almost everywhere over the office "notepad".
- **back pages** — the History screen. Taken verbatim from the landing's
  `articlesIndex.kicker` in every language, which is why it is the longest
  header in the app (see the length notes below).
- **attempt** — one run at the 90 days, archived on failure.
- **nudge** — the reminder notification, deliberately weaker than "alert":
  *pripomenutie*, *Anstupser*, *puf*, *knuff*, *dult*, *σκούντημα*, *dürtme*.
  Four languages have no weak-nudge noun that does not collide with tapping or
  poking, so lt, mt, be and mk collapse it into the ordinary reminder word.
- **tip jar** — the donation screen. Rule everywhere: never the word that reads
  *charitable*.

## Deviations from the landing, on purpose

Each of these is a place where the app says something the site does not. They
are listed so a reviewer can settle them in one pass rather than find them one
at a time.

- **pt — the tip jar is `gorjeta`,** where `pt.json`'s `faq.q2.a` says
  *donativo*. In European Portuguese *donativo* reads charitable. Either the
  landing moves to *gorjeta* or the app moves back; right now they differ.
- **da — the tip jar is `Drikkepenge`,** where the landing says *et frivilligt
  bidrag*. *Bidrag* was judged too bureaucratic for a screen title. The two do
  not contradict, but they are not the same word.
- **mt — the tip jar is `Għotja`,** not the landing's *donazzjoni volontarja*,
  for the charitable-reading rule.
- **lv — the tip jar is `Ziedojums`,** which *does* read charitable, kept only
  because the landing already says *brīvprātīgs ziedojums* and the true tip word
  (*dzeramnauda*) will not fit the amount button. This one wants a native to
  break the tie.
- **be — system notifications are `апавяшчэнні`,** not the landing's
  *паведамленні*, which means "message" and would clash with Android's own
  settings wording.
- **sl** keeps the landing's male past participles rather than de-gendering the
  reader, because the site already does; cs, lv, lt, is, bg, uk, ru and the
  South Slavic files all rewrite them out. That inconsistency is inherited.
- **sl, hr, bs, sr** use the en dash `–` for parenthetical dashes where the base
  file uses `—`, following their landing files and South Slavic typography.

## Per-language, the one string to check first

**sk** `history_failed` / `history_failed_missed` — "zlyhal v %1$d. deň": case
government after *v*, and the masculine agrees with *pokus*, not the reader.
**cs** `history_exported` "Zápisník je zapsaný do souboru." — colloquial
adjectival passive; a purist wants "byl zapsán".
**pl** `history_stats_best` = "rekord", a deliberate departure from "best
attempt", chosen so it does not collide with "najdłuższa seria" beside it.
**de** `tip_jar_amounts_title` "Falls du etwas dalassen willst" — *dalassen* is
deliberately colloquial; the safe version is "Wenn du etwas übrig hast".
**nl** `history_exported` "Het schrift is weggeschreven." — correct technically,
odd next to the paper metaphor; and confirm *schrift* over *notitieboek*, which
propagates through seven strings.
**da** the `tip_jar_title` decision above, then `challenge_fail_condition_summary`
"Misset hvis:".
**sv** `history_completed` "klarad" — a one-word outcome label with no landing
precedent; *avklarad* or *klar* may be preferred.
**nb** `reminders_intro` / `reminder_channel_description` "ett dult" — correct
for the intended weakness, but uncommon as a noun; "et lite puff" is the
fallback.
**is** `challenge_setup_confirm_restart` — a subjunctive-flavoured conditional
inversion that may read stiff; and whether **stílabók** or *kompa* is the
notebook.
**fi** `history_failed` "katkesi %1$d. päivänä" — the chain-breaking metaphor
instead of a literal *epäonnistui*.
**et** `history_title` "Tagumised leheküljed" (20 characters against 10). Kept
for landing fidelity; the drop-in fix is "Tagumised lehed", which would also
need `home_history_link` and `history_past_empty`.
**hu** `history_stats_streak` = **"csúcssorozat"**, a coinage on the productive
*csúcs-* prefix, because "leghosszabb sorozat" wrecks the three-column row.
**lv** `tip_jar_title` "Ziedojums" — see the deviations above.
**lt** `history_stats_streak` "ilgiausia serija" (16 characters) will wrap while
its two neighbours stay on one line; and `history_title` is the longest header
in the app at 21.
**el** `history_stats_best` "καλύτερη" — the noun had to be dropped to fit, so it
reads as a bare feminine superlative.
**tr** "dürtme" for the nudge — the honest weak word, but a phone reader's first
association is the social-media poke; *hatırlatma* is the safe fallback and
collapses nudge into reminder.
**sq** `history_stats_best` "më e mira", the same nounless-superlative problem as
Greek.
**mt** the three strings rewritten to dodge a definite article before a digit —
`challenge_setup_start`, `home_restart_explanation`, `widget_invite`. Maltese
orthography for *il-/id-* before figures is unsettled.
**sl** `challenge_setup_hint_name_required` "Navada brez imena ne gre na seznam."
— a free rewrite chosen for the quiet register.
**hr** `history_title` "Stražnje stranice" (17 characters), the landing's kicker
verbatim; "Zadnje stranice" is the shorter fallback.
**bs** **bilježnica** as the notebook — a genuinely shared ijekavian word, but if
a native prefers *sveska* it changes seven strings.
**sr** `backup_error_malformed` / `backup_error_unsupported_version`, where the
sentence was reshaped to avoid inflecting the brand (`Snurko-а`, which the
landing notes flag as contested).
**bg** `challenge_setup_confirm_title` "Започваме ли?" — an impersonal rewrite of
"Ready to start?" to avoid the gendered "Готов ли си".
**mk** `home_grid` / `history_completion` "со квачка" — whether *квачка* survives
as the everyday word in running copy, or whether a native flips the file to
*штиклирање*.
**uk** `tip_jar_title` "Скарбничка", the noun the whole screen hangs on.
**be** `history_stats_best` / `history_stats_streak` "лепшая спроба" / "лепшая
серыя" — the comparative used in its superlative sense to fit; *найлепшая* is
the strict form and is two characters too wide.
**ru** `history_title` "Последние полосы" (16 characters), the newspaper idiom
from the landing, and the widest header in the app after lt and et.
**fr** `history_stats_best` "meilleur essai" — the stats row says *essai* while
`history_attempt` says *Tentative*, because "meilleure tentative" does not fit.
**it** `history_stats_best` "miglior prova", the same trade-off.
**es** `home_restart_acknowledge` "Volver a empezar" (16 characters on a dialog
button).
**pt** the *gorjeta* / *donativo* split above, then `history_completed`
"concluída" agreeing with *tentativa*.
**ca** `home_restart_explanation` and `challenge_setup_confirm_locked`, the
longest clitic-dense sentences, plus whether "Demana-ho" is right for
`tip_jar_price_retry`. Catalan is the Romance file most wanting a native.
**ro** `history_stats_best` = **"record"** — "cea mai bună încercare" is 22
characters and cannot sit in a third of a row.

## Compose Multiplatform, the mechanics

- **Qualifier syntax.** Compose Multiplatform 1.11 accepts a two-letter ISO
  639-1 or three-letter ISO 639-2 language, optionally followed by a region as
  `-rXX`. It does **not** accept BCP-47 (`b+sr+Cyrl`) or a script subtag. All 33
  directories are therefore plain two-letter codes.
- **Serbian is Cyrillic in `values-sr`.** There is no way to qualify the script,
  so a device set to Serbian gets Cyrillic whatever its script preference —
  which matches the landing and the official standard, but is a real limitation
  rather than a choice.
- **Norwegian is `values-nb`.** `nb` is a valid ISO 639-1 code and resolves for a
  device set to Norwegian Bokmål; a device set to Nynorsk (`nn`) falls back to
  English.
- **Placeholders** are only `%<index>$d` and `%<index>$s`. Width and padding
  flags are copied to the screen unsubstituted, so `history_date` prints its day
  and month unpadded and each locale orders the three arguments itself.
- **Apostrophes are escaped in opposite directions in the two resource trees.**
  Compose resources take a plain `'` — the values converter does not process
  Android's backslash escapes, so `\'` would put a literal backslash on screen.
  Android resources under `androidMain/res` take `\'`, because AAPT2 rejects a
  bare apostrophe. Both directions are asserted by tests.
- **Locale strings are never shrunk.** Compose resources ship as APK assets, not
  as Android resources, so R8's resource shrinking cannot reach them; no
  `localeFilters` is configured, and the app bundle disables the language split
  so every locale is delivered with the base APK.

## What the tests enforce

`composeApp/src/androidUnitTest/kotlin/app/snurko/resources/LocaleCompletenessTest.kt`:

- every shipped locale has both files, and no unexpected qualifier directory exists;
- every locale defines exactly the base key set — no missing key, no extra key;
- the plurals block carries exactly the categories CLDR defines for that
  language, as bundled in Compose Multiplatform 1.11 (a missing category falls
  back to `other` at runtime, so nothing else would catch it);
- every placeholder in a base string survives translation with the same index
  bound to the same meaning, and no unsupported format specifier is used;
- the marks and the bare numeral (`×`, `←`, `—`, `✓`, `%1$d`, `Snurko`) are
  untranslated, and the brand survives in the seven strings that name it;
- no compose-resource value carries a backslash, and every android-resource
  apostrophe carries one;
- no locale is a partial copy of the English file.

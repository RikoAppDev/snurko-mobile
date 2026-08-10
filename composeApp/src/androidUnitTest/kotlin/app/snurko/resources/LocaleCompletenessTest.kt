package app.snurko.resources

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val SHIPPED_LOCALES = listOf(
    "sk", "cs", "pl", "de", "nl", "fr", "it", "es", "pt", "ca", "ro", "da", "sv",
    "nb", "is", "fi", "et", "hu", "lv", "lt", "el", "tr", "sq", "mt", "sl", "hr",
    "bs", "sr", "bg", "mk", "uk", "be", "ru",
)

/**
 * The plural categories CLDR defines per language, as bundled in Compose
 * Multiplatform 1.11. A category a language needs but a file omits falls back to
 * `other` at runtime rather than failing, so only this test catches the gap.
 */
private val PLURAL_CATEGORIES = mapOf(
    "cs" to setOf("one", "few", "many", "other"),
    "sk" to setOf("one", "few", "many", "other"),
    "pl" to setOf("one", "few", "many", "other"),
    "be" to setOf("one", "few", "many", "other"),
    "lt" to setOf("one", "few", "many", "other"),
    "ru" to setOf("one", "few", "many", "other"),
    "uk" to setOf("one", "few", "many", "other"),
    "sl" to setOf("one", "two", "few", "other"),
    "hr" to setOf("one", "few", "other"),
    "bs" to setOf("one", "few", "other"),
    "sr" to setOf("one", "few", "other"),
    "ro" to setOf("one", "few", "other"),
    "lv" to setOf("zero", "one", "other"),
    "mt" to setOf("one", "two", "few", "many", "other"),
    "fr" to setOf("one", "many", "other"),
    "pt" to setOf("one", "many", "other"),
    "it" to setOf("one", "many", "other"),
    "ca" to setOf("one", "many", "other"),
    "es" to setOf("one", "many", "other"),
    "de" to setOf("one", "other"),
    "nl" to setOf("one", "other"),
    "sv" to setOf("one", "other"),
    "da" to setOf("one", "other"),
    "nb" to setOf("one", "other"),
    "is" to setOf("one", "other"),
    "fi" to setOf("one", "other"),
    "et" to setOf("one", "other"),
    "hu" to setOf("one", "other"),
    "el" to setOf("one", "other"),
    "bg" to setOf("one", "other"),
    "mk" to setOf("one", "other"),
    "tr" to setOf("one", "other"),
    "sq" to setOf("one", "other"),
)

private val VERBATIM_KEYS = setOf(
    "challenge_setup_remove_mark",
    "reminders_switch_off_mark",
    "reminders_back_mark",
    "history_back_mark",
    "reminders_off",
    "sticker_wordmark",
    "history_stats_count",
)

private val BRAND_KEYS = setOf(
    "history_support",
    "tip_jar_free",
    "reminders_blocked_title",
    "widget_open",
    "backup_error_malformed",
    "backup_error_unsupported_version",
    "sticker_wordmark",
)

/**
 * Compose Multiplatform substitutes `%<index>$d` and `%<index>$s` and nothing
 * else; a width flag such as `%02d` reaches the screen unsubstituted.
 */
private val PLACEHOLDER = Regex("""%(\d+)\$[ds]""")
private val UNSUPPORTED_SPEC = Regex("""%[0-9.+#-]*[a-zA-Z]|%\d+\$""")

private class Strings(
    val values: Map<String, String>,
    val plurals: Map<String, List<Pair<String, String>>>,
)

class LocaleCompletenessTest {

    private val moduleDir = findModuleDir()
    private val composeResources = File(moduleDir, "src/commonMain/composeResources")
    private val androidRes = File(moduleDir, "src/androidMain/res")
    private val base = parse(File(composeResources, "values/strings.xml"))
    private val locales = SHIPPED_LOCALES.associateWith { locale ->
        File(composeResources, "values-$locale/strings.xml").takeIf { it.isFile }?.let(::parse)
    }

    @Test
    fun `every shipped locale has a compose resources file`() {
        assertEquals(
            emptyList(),
            locales.filterValues { it == null }.keys.toList(),
            "locales without a strings.xml",
        )
    }

    @Test
    fun `no qualifier directory ships that is not a shipped locale`() {
        val declared = (SHIPPED_LOCALES.map { "values-$it" } + "values").toSet()
        val found = composeResources.listFiles().orEmpty()
            .filter { it.isDirectory && it.name.startsWith("values") }
            .map { it.name }
            .toSet()
        assertEquals(emptySet(), found - declared, "unexpected qualifier directories")
    }

    @Test
    fun `every locale defines exactly the base key set`() {
        forEachLocale { locale, strings ->
            assertEquals(base.values.keys, strings.values.keys, "[$locale] string keys")
            assertEquals(base.plurals.keys, strings.plurals.keys, "[$locale] plural keys")
        }
    }

    @Test
    fun `every locale carries the plural categories its language needs`() {
        forEachLocale { locale, strings ->
            strings.plurals.forEach { (name, items) ->
                assertEquals(
                    PLURAL_CATEGORIES.getValue(locale),
                    items.map { it.first }.toSet(),
                    "[$locale] $name quantities",
                )
            }
        }
    }

    @Test
    fun `every locale keeps the placeholders of the base string`() {
        forEachLocale { locale, strings ->
            base.values.forEach { (key, english) ->
                assertEquals(
                    english.placeholders(),
                    strings.values.getValue(key).placeholders(),
                    "[$locale] $key placeholders",
                )
            }
        }
    }

    @Test
    fun `every plural item carries the count placeholder`() {
        forEachLocale { locale, strings ->
            strings.plurals.forEach { (name, items) ->
                items.forEach { (quantity, text) ->
                    assertTrue(
                        text.placeholders().isNotEmpty(),
                        "[$locale] $name/$quantity has no count placeholder",
                    )
                }
            }
        }
    }

    @Test
    fun `no locale uses a format specifier compose cannot substitute`() {
        forEachLocale { locale, strings ->
            val texts = strings.values.entries.map { it.key to it.value } +
                strings.plurals.flatMap { (name, items) -> items.map { "$name/${it.first}" to it.second } }
            texts.forEach { (key, text) ->
                val leftovers = UNSUPPORTED_SPEC.find(PLACEHOLDER.replace(text, ""))
                assertEquals(null, leftovers?.value, "[$locale] $key: $text")
            }
        }
    }

    /**
     * Compose Multiplatform's values converter does not process Android's
     * backslash escapes, so `\'` reaches the screen with the backslash intact.
     */
    @Test
    fun `no string escapes an apostrophe the android way`() {
        val offenders = (mapOf("en" to base) + locales.mapValues { checkNotNull(it.value) })
            .flatMap { (locale, strings) ->
                val texts = strings.values.entries.map { it.key to it.value } +
                    strings.plurals.flatMap { (name, items) -> items.map { name to it.second } }
                texts.filter { it.second.contains('\\') }.map { "[$locale] ${it.first}: ${it.second}" }
            }
        assertEquals(emptyList(), offenders, "values carrying a literal backslash")
    }

    @Test
    fun `marks and numerals are left untranslated`() {
        forEachLocale { locale, strings ->
            VERBATIM_KEYS.forEach { key ->
                assertEquals(base.values.getValue(key), strings.values.getValue(key), "[$locale] $key")
            }
            assertTrue(
                strings.values.getValue("history_stats_marks").contains("✓"),
                "[$locale] history_stats_marks lost the ✓",
            )
        }
    }

    @Test
    fun `the brand survives translation`() {
        forEachLocale { locale, strings ->
            BRAND_KEYS.forEach { key ->
                assertTrue(strings.values.getValue(key).contains("Snurko"), "[$locale] $key lost Snurko")
            }
        }
    }

    @Test
    fun `no locale is a copy of the english file`() {
        val suspect = mutableListOf<String>()
        forEachLocale { locale, strings ->
            val shared = strings.values.count { (key, text) ->
                key !in VERBATIM_KEYS && text == base.values[key]
            }
            if (shared > 25) suspect += "$locale ($shared strings identical to English)"
        }
        assertEquals(emptyList(), suspect, "locales that look partly untranslated")
    }

    @Test
    fun `the android widget description is localised for every locale`() {
        val baseAndroid = parse(File(androidRes, "values/strings.xml"))
        SHIPPED_LOCALES.forEach { locale ->
            val file = File(androidRes, "values-$locale/strings.xml")
            assertTrue(file.isFile, "[$locale] missing androidMain res strings.xml")
            assertEquals(baseAndroid.values.keys, parse(file).values.keys, "[$locale] android string keys")
        }
    }

    /** AAPT2 rejects a bare apostrophe, which is the opposite of what compose resources want. */
    @Test
    fun `android resource apostrophes are escaped`() {
        val offenders = (listOf("values") + SHIPPED_LOCALES.map { "values-$it" })
            .flatMap { dir ->
                val file = File(androidRes, "$dir/strings.xml")
                if (!file.isFile) return@flatMap emptyList()
                Regex("""(?<!\\)'""").findAll(file.readText())
                    .map { "[$dir] unescaped apostrophe at ${it.range.first}" }
                    .toList()
            }
        assertEquals(emptyList(), offenders, "android string values with a bare apostrophe")
    }

    private fun forEachLocale(check: (String, Strings) -> Unit) {
        locales.forEach { (locale, strings) ->
            check(locale, checkNotNull(strings) { "[$locale] strings.xml is missing" })
        }
    }
}

private fun String.placeholders(): Set<String> =
    PLACEHOLDER.findAll(this).map { it.groupValues[1] }.toSet()

private fun findModuleDir(): File {
    var dir: File? = File("").absoluteFile
    while (dir != null) {
        if (File(dir, "src/commonMain/composeResources").isDirectory) return dir
        if (File(dir, "composeApp/src/commonMain/composeResources").isDirectory) {
            return File(dir, "composeApp")
        }
        dir = dir.parentFile
    }
    error("composeResources not found from ${File("").absolutePath}")
}

private fun parse(file: File): Strings {
    val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
    val values = LinkedHashMap<String, String>()
    val plurals = LinkedHashMap<String, List<Pair<String, String>>>()
    root.elements().forEach { element ->
        val name = element.getAttribute("name")
        when (element.tagName) {
            "string" -> values[name] = element.textContent
            "plurals" -> plurals[name] = element.elements().map { it.getAttribute("quantity") to it.textContent }
        }
    }
    return Strings(values, plurals)
}

private fun Element.elements(): List<Element> =
    (0 until childNodes.length).mapNotNull { childNodes.item(it) as? Element }

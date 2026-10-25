package app.vocabflash.datasetbuilder.kaikki

import app.vocabflash.datasetbuilder.model.Example
import app.vocabflash.datasetbuilder.util.normalizeGender
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parses one Kaikki JSONL line (already decoded to a [JsonObject]) into a [CompactKaikki].
 *
 * Navigation is intentionally defensive: the extract's schema is large and drifts over time, so we
 * read fields by name from the loose JSON tree and tolerate anything missing rather than failing on
 * an unexpected shape.
 */
object KaikkiParser {

    private val GENDER_KEYS = listOf("g", "gen", "gender")

    fun parse(word: String, obj: JsonObject): CompactKaikki {
        val pos = obj.str("pos")
        val posKey = pos?.lowercase()
        val senseObjs = obj.arr("senses")?.mapNotNull { it as? JsonObject }.orEmpty()

        val gender = extractGender(obj, senseObjs)
        val forms = obj.arr("forms")?.mapNotNull { it as? JsonObject }.orEmpty()

        // Inflection fields only make sense for the matching part of speech.
        val plural = if (posKey == "noun") {
            forms.firstOrNull { f ->
                val tags = f.tags()
                "plural" in tags && "genitive" !in tags && "dative" !in tags
            }?.str("form")
        } else {
            null
        }

        val infinitive = if (posKey == "verb") {
            forms.firstOrNull { "infinitive" in it.tags() }?.str("form")
        } else {
            null
        }
        val praeteritum = if (posKey == "verb") {
            forms.firstOrNull { f ->
                val tags = f.tags()
                "past" in tags && "participle" !in tags && "singular" in tags &&
                    ("third-person" in tags || "first-person" in tags)
            }?.str("form") ?: forms.firstOrNull { f ->
                val tags = f.tags(); "past" in tags && "participle" !in tags
            }?.str("form")
        } else {
            null
        }
        val partizipII = if (posKey == "verb") {
            forms.firstOrNull { f ->
                val tags = f.tags(); "participle" in tags && "past" in tags
            }?.str("form")
        } else {
            null
        }
        // Present-tense principal parts and Partizip I (present participle). Kaikki tags these as
        // present + person/number, and participle + present respectively. Only meaningful for verbs.
        val presentDu = if (posKey == "verb") {
            forms.firstOrNull { f ->
                val tags = f.tags()
                "present" in tags && "second-person" in tags && "singular" in tags
            }?.str("form")
        } else {
            null
        }
        val presentEr = if (posKey == "verb") {
            forms.firstOrNull { f ->
                val tags = f.tags()
                "present" in tags && "third-person" in tags && "singular" in tags
            }?.str("form")
        } else {
            null
        }
        val presentIhr = if (posKey == "verb") {
            forms.firstOrNull { f ->
                val tags = f.tags()
                "present" in tags && "second-person" in tags && "plural" in tags
            }?.str("form")
        } else {
            null
        }
        val partizipI = if (posKey == "verb") {
            forms.firstOrNull { f ->
                val tags = f.tags(); "participle" in tags && "present" in tags
            }?.str("form")
        } else {
            null
        }

        // All surface forms, for widening Tatoeba search keys (inflected forms).
        val allForms = LinkedHashSet<String>().apply {
            for (f in forms) f.str("form")?.let { add(it) }
        }.toList()

        // German extracts usually have no `translations`; the English meaning is the sense gloss.
        val translations = LinkedHashSet<String>()
        for (sense in senseObjs) {
            for (t in sense.arr("translations")?.mapNotNull { it as? JsonObject }.orEmpty()) {
                if (t.str("lang") == "English") {
                    t.str("word")?.let { translations.add(it) }
                }
            }
        }
        if (translations.isEmpty()) {
            for (sense in senseObjs) {
                for (gloss in sense.strList("glosses")) {
                    val cleaned = gloss.substringBefore(" (").trim().trimEnd('.')
                    if (cleaned.isNotEmpty()) translations.add(cleaned)
                }
            }
        }

        val ipa = LinkedHashSet<String>()
        for (s in obj.arr("sounds")?.mapNotNull { it as? JsonObject }.orEmpty()) {
            s.str("ipa")?.let { ipa.add(it) }
        }

        val examples = ArrayList<Example>()
        for (sense in senseObjs) {
            for (e in sense.arr("examples")?.mapNotNull { it as? JsonObject }.orEmpty()) {
                val de = e.str("text") ?: continue
                examples.add(Example(de = de, en = e.str("english")))
            }
        }

        return CompactKaikki(
            word = word,
            pos = pos,
            gender = gender,
            plural = plural,
            infinitive = infinitive,
            praeteritum = praeteritum,
            partizipII = partizipII,
            partizipI = partizipI,
            presentDu = presentDu,
            presentEr = presentEr,
            presentIhr = presentIhr,
            translations = translations.toList(),
            ipa = ipa.toList(),
            examples = examples,
            forms = allForms,
        )
    }

    private fun extractGender(obj: JsonObject, senses: List<JsonObject>): String? {
        // 1) head_templates[].args[g], the most reliable spot for German nouns.
        for (ht in obj.arr("head_templates")?.mapNotNull { it as? JsonObject }.orEmpty()) {
            val args = ht["args"] as? JsonObject ?: continue
            for (key in GENDER_KEYS) {
                args[key]?.let { normalizeGender(it.jsonPrimitive.contentOrNull)?.let { g -> return g } }
            }
        }
        // 2) sense tags such as ["masculine"].
        for (sense in senses) {
            for (tag in sense.strList("tags")) {
                when (tag.lowercase()) {
                    "masculine" -> return "masculine"
                    "feminine" -> return "feminine"
                    "neuter" -> return "neuter"
                }
            }
        }
        return null
    }
}

private fun JsonObject.str(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }

private fun JsonObject.arr(key: String): JsonArray? = (this[key] as? JsonArray)

private fun JsonObject.tags(): Set<String> = strList("tags").toSet()

private fun JsonObject.strList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.orEmpty()

/** Default strict-enough JSON reader: unknown keys are fine, missing fields are fine. */
internal val KAIKKI_JSON: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
}

internal fun parseKaikkiLine(line: String): CompactKaikki? {
    val element: JsonElement = KAIKKI_JSON.parseToJsonElement(line)
    val obj = element as? JsonObject ?: return null
    val word = (obj["word"] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() } ?: return null
    val langCode = (obj["lang_code"] as? JsonPrimitive)?.contentOrNull
    if (langCode != null && langCode != "de") return null
    return KaikkiParser.parse(word, obj)
}

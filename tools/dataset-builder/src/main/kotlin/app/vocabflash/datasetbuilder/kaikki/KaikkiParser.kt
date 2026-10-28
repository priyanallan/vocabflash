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

    /**
     * Wiktextract glosses on inflected-form entries describe the base lemma instead of a meaning
     * ("inflection of erste.: …", "comparative degree of gut; better"). Group 2 captures the base.
     */
    private val inflectionBaseRegex =
        Regex("(?i)^((?:inflection of)|(?:alternative (?:spelling |form )?of)|(?:obsolete spelling of)|(?:comparative (?:form |degree )?of)|(?:superlative (?:form |degree )?of))\\s+([\\p{L}\\-]+)")

    /** "inflection of all (“all”)" — the quoted tail is the actual meaning. */
    private val quotedMeaningRegex = Regex("[“\"]([^”\"]+)[”\"]")

    /**
     * "dative masculine/neuter singular of der: the" / "comparative degree of gut; better" —
     * group 1 is the base lemma, group 2 the meaning that follows ":" or ";".
     */
    private val meaningAfterOfRegex =
        Regex("(?i).*\\bof\\s+([\\p{L}\\-]+)\\s*[:;]\\s*(.+)")

    /** True when a gloss consists only of declension keywords — a form description, not a meaning. */
    private val DECLENSION_WORDS = setOf(
        "strong", "weak", "mixed", "masculine", "feminine", "neuter",
        "nominative", "accusative", "genitive", "dative",
        "singular", "plural", "all", "gender", "case", "comparative", "superlative",
    )

    /**
     * "dative plural of der" / "genitive masculine/neuter singular of der" — a pure case-form
     * description; group 1 (when present) is the referenced base lemma.
     */
    private val caseFormRegex = Regex(
        "(?i)^(?:[a-z]+[ /])*(?:nominative|accusative|genitive|dative)(?:[ /][a-z]+)*(?:[ /](?:singular|plural))(?: of ([\\p{L}\\-]+))?$",
    )

    /** "Dative singular masculine and neutral gender forms of alles" — base in the trailing "of". */
    private val formsOfRegex = Regex("(?i)^(?:nominative|accusative|genitive|dative)\\b.*\\bforms? of ([\\p{L}\\-]+)\\b")

    private fun isDeclensionOnlyGloss(gloss: String): Boolean {
        val words = gloss.lowercase()
            .replace(',', ' ').replace('/', ' ').replace('-', ' ')
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (words.isEmpty()) return false
        return words.all { it in DECLENSION_WORDS }
    }

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
        var capturedInflectionBase: String? = null
        for (sense in senseObjs) {
            for (t in sense.arr("translations")?.mapNotNull { it as? JsonObject }.orEmpty()) {
                if (t.str("lang") == "English") {
                    t.str("word")?.let { translations.add(it) }
                }
            }
        }
        if (translations.isEmpty()) {
            var inflectionBase: String? = null
            for (sense in senseObjs) {
                val senseGlosses = sense.strList("glosses")
                if ("form-of" in sense.tags()) {
                    // Form-of senses (inflected forms, comparative/superlative forms, alt spellings)
                    // describe a base lemma, but often carry the real meaning after "of"/quotes:
                    //   "comparative degree of gut; better"            → "better"
                    //   "dative masculine/neuter singular of der: the" → "the"
                    //   "inflection of all (“all”)"                    → "all"
                    //   "inflection of gut:"                           → (nothing; base "gut")
                    for (gloss in senseGlosses) {
                        // Pure case-form descriptions inside form-of senses ("dative plural of der").
                        val formMatch = caseFormRegex.find(gloss) ?: formsOfRegex.find(gloss)
                        if (formMatch != null) {
                            val base = formMatch.groupValues[1]
                            if (inflectionBase == null && base.isNotEmpty()) inflectionBase = base
                            quotedMeaningRegex.find(gloss)?.let { q ->
                                q.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { translations.add(it) }
                            }
                            continue
                        }
                        inflectionBaseRegex.find(gloss)?.let { m ->
                            if (inflectionBase == null) inflectionBase = m.groupValues[2]
                        }
                        quotedMeaningRegex.find(gloss)?.let { m ->
                            m.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { translations.add(it) }
                        }
                        meaningAfterOfRegex.find(gloss)?.let { m ->
                            if (inflectionBase == null) inflectionBase = m.groupValues[1]
                            m.groupValues[2].trim().trimEnd('.').takeIf { it.isNotEmpty() }
                                ?.let { translations.add(it) }
                        }
                    }
                    continue
                }
                for (gloss in senseGlosses) {
                    // Form descriptions outside the form-of tag: capture the base, never the case text.
                    val inflection = inflectionBaseRegex.find(gloss)
                    if (inflection != null) {
                        if (inflectionBase == null) inflectionBase = inflection.groupValues[2]
                        continue
                    }
                    val formMatch = caseFormRegex.find(gloss) ?: formsOfRegex.find(gloss)
                    if (formMatch != null) {
                        val base = formMatch.groupValues[1]
                        if (inflectionBase == null && base.isNotEmpty()) inflectionBase = base
                        quotedMeaningRegex.find(gloss)?.let { q ->
                            q.groupValues[1].trim().takeIf { it.isNotEmpty() }?.let { translations.add(it) }
                        }
                        continue
                    }
                    // A gloss consisting only of declension keywords ("strong genitive masculine/neuter
                    // singular") is a form description, not a meaning.
                    if (isDeclensionOnlyGloss(gloss)) continue
                    val cleaned = gloss.substringBefore(" (").trim().trimEnd('.', ',', ';', ':')
                    if (cleaned.isNotEmpty()) translations.add(cleaned)
                }
            }
            capturedInflectionBase = inflectionBase
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
            inflectionBase = capturedInflectionBase,
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
        // 2) sense tags such as ["masculine"] — but NOT on form-of senses: their gender tags
        //    describe the inflected form's declension ("der erster"), not the word's gender.
        for (sense in senses) {
            if ("form-of" in sense.tags()) continue
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

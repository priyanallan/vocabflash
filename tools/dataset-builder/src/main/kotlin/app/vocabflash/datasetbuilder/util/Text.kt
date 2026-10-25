package app.vocabflash.datasetbuilder.util

import java.util.Locale

/**
 * Text normalization helpers shared by the matcher, the ID generator and the emitters.
 *
 * The normalization here is deliberately simple and stable: it must never change between runs
 * (stable IDs depend on it), so no locale-sensitive operations other than an explicit
 * [Locale.ROOT] lowercase are used.
 */

/** Case- and whitespace-normalized form used as a lookup key. */
fun normalizeLemma(raw: String): String = raw.trim().lowercase(Locale.ROOT)

/**
 * URL/file-safe slug. German umlauts and ß are transliterated (ä→ae, ö→oe, ü→ue, ß→ss) so that
 * `schön` and `schoen` intentionally collapse to the same slug, and any remaining non-alphanumeric
 * run collapses to a single `-`.
 */
fun slug(raw: String): String {
    val lower = raw.trim().lowercase(Locale.ROOT)
        .replace("ä", "ae")
        .replace("ö", "oe")
        .replace("ü", "ue")
        .replace("ß", "ss")
    val sb = StringBuilder(lower.length)
    var pendingDash = false
    for (c in lower) {
        if (c in 'a'..'z' || c in '0'..'9') {
            sb.append(c)
            pendingDash = false
        } else if (sb.isNotEmpty() && !pendingDash) {
            sb.append('-')
            pendingDash = true
        }
    }
    return sb.toString().trim('-')
}

/**
 * Stable, human-readable entry id derived from normalized lemma + level (+ pos when present).
 *
 * The id is independent of any source list so that the Dataset survives a future switch to an
 * openly-licensed source (ADR-0002 / D26).
 */
fun stableId(level: String, lemma: String, pos: String?): String {
    val base = "${level.trim().lowercase(Locale.ROOT)}-${slug(lemma)}"
    val posSlug = pos?.let { slug(it) }.orEmpty()
    return if (posSlug.isEmpty()) base else "$base-$posSlug"
}

/** Canonical German grammatical gender token, or `null` when unrecognized. */
fun normalizeGender(raw: String?): String? {
    val v = raw?.trim()?.lowercase(Locale.ROOT) ?: return null
    if (v.isEmpty()) return null
    return when {
        v.startsWith("m") -> "masculine" // m, mask, maskulin, masculine
        v.startsWith("f") -> "feminine"   // f, fem, feminin, feminine
        v.startsWith("n") -> "neuter"     // n, neu, neutrum, neuter
        v == "3" -> "feminine"            // some Kaikki head-template encodings use 3=f
        v == "2" -> "masculine"
        v == "1" -> "masculine"
        else -> null
    }
}

/** Definite article implied by a canonical gender token. */
fun articleForGender(gender: String?): String? = when (gender) {
    "masculine" -> "der"
    "feminine" -> "die"
    "neuter" -> "das"
    else -> null
}

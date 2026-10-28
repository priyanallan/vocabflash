package app.vocabflash.datasetbuilder.enrich

import app.vocabflash.datasetbuilder.kaikki.CompactKaikki
import app.vocabflash.datasetbuilder.match.KaikkiIndexLookup
import app.vocabflash.datasetbuilder.wordlist.WordListRow
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The inflected-form fallback: when a Kaikki entry's glosses were all "inflection of …" (so no
 * translation survives filtering), the enricher borrows the base lemma's translations.
 */
class EnricherTest {

    private val limits = EnrichLimits()

    private fun row(lemma: String, level: String = "A1") =
        WordListRow(lemma = lemma, level = level)

    @Test
    fun fallsBackToTheBaseLemmasTranslations() {
        val inflected = CompactKaikki(
            word = "erste", pos = "num", translations = emptyList(), inflectionBase = "erste",
        )
        val base = CompactKaikki(
            word = "erste", pos = "adj", translations = listOf("first (1st)"),
        )
        val lookup = KaikkiIndexLookup { lemma -> if (lemma == "erste") base else null }

        val entry = Enricher.enrich(row("erste"), inflected, limits, lookupBase = lookup)

        assertEquals(listOf("first (1st)"), entry.translations)
    }

    @Test
    fun wordlistTranslationsAlwaysWin() {
        val kaikki = CompactKaikki(word = "Haus", pos = "noun", translations = listOf("house"))
        val entry = Enricher.enrich(
            WordListRow(lemma = "Haus", level = "A1", translations = listOf("house, home")),
            kaikki,
            limits,
        )
        assertEquals(listOf("house, home", "house"), entry.translations)
    }

    @Test
    fun mechanicalStemFallbackDeinflectsTheWordlistLemma() {
        // "ersten" carries only declension-pattern glosses; stripping -en finds "erst" ("first").
        val inflected = CompactKaikki(
            word = "ersten", pos = "adj", translations = emptyList(),
        )
        val base = CompactKaikki(word = "erst", pos = "adv", translations = listOf("first; only"))
        val lookup = KaikkiIndexLookup { lemma -> if (lemma == "erst") base else null }

        val entry = Enricher.enrich(row("ersten"), inflected, limits, lookupBase = lookup)

        assertEquals(listOf("first; only"), entry.translations)
    }

    @Test
    fun stemFallbackDoesNotButcherShortLemmas() {
        // Short function words (der, das, ein) must not be de-inflected into garbage.
        val entry = Enricher.enrich(
            row("der"), CompactKaikki(word = "der", pos = "pron", translations = emptyList()),
            limits, lookupBase = KaikkiIndexLookup { null },
        )
        assertEquals(emptyList(), entry.translations)
    }

    @Test
    fun noBaseMeansNoTranslationRatherThanInflectionText() {
        val inflected = CompactKaikki(
            word = "ersten", pos = "adj", translations = emptyList(), inflectionBase = "erster",
        )
        val entry = Enricher.enrich(row("ersten"), inflected, limits, lookupBase = KaikkiIndexLookup { null })
        assertEquals(emptyList(), entry.translations)
    }
}

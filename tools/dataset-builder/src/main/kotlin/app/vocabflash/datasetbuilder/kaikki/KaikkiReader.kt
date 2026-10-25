package app.vocabflash.datasetbuilder.kaikki

import app.vocabflash.datasetbuilder.util.normalizeLemma
import java.io.BufferedReader
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.GZIPInputStream
import kotlin.io.path.name

/** Counters returned by [KaikkiReader.stream] so the QA report can explain skipped lines. */
data class StreamStats(
    val linesRead: Long,
    val entriesParsed: Long,
    val linesSkipped: Long,
)

/**
 * Streams a Kaikki/Wiktextract German extract (`.jsonl` or `.jsonl.gz`) line by line.
 *
 * The whole file is never loaded into memory; callers fold each [CompactKaikki] into whatever index
 * they need. Transparently decompresses gzip by filename suffix.
 */
object KaikkiReader {

    fun stream(path: Path, onEntry: (CompactKaikki) -> Unit): StreamStats {
        var lines = 0L
        var parsed = 0L
        var skipped = 0L

        openStream(path).bufferedReader(StandardCharsets.UTF_8).use { reader: BufferedReader ->
            reader.forEachLine { raw ->
                lines++
                val line = raw.trim()
                if (line.isEmpty()) return@forEachLine
                val entry = try {
                    parseKaikkiLine(line)
                } catch (_: Exception) {
                    null
                }
                if (entry == null) {
                    skipped++
                } else {
                    parsed++
                    onEntry(entry)
                }
            }
        }
        return StreamStats(linesRead = lines, entriesParsed = parsed, linesSkipped = skipped)
    }

    private fun openStream(path: Path): InputStream {
        val raw = Files.newInputStream(path)
        return if (path.name.endsWith(".gz")) GZIPInputStream(raw, 1 shl 16) else raw
    }
}

/**
 * Builds a lemma index from a stream of Kaikki entries, keeping the richest entry per normalized
 * lemma. Exact-cased keys are also retained so the matcher can prefer an exact match when both a
 * cased and a case-normalized lemma exist.
 */
class KaikkiIndex {
    private val byNormalized = HashMap<String, CompactKaikki>()
    private val byExact = HashMap<String, CompactKaikki>()

    var size: Int = 0
        private set

    fun add(entry: CompactKaikki) {
        val norm = normalizeLemma(entry.word)
        if (norm.isEmpty()) return
        size++

        val currentNorm = byNormalized[norm]
        if (currentNorm == null || entry.richness() > currentNorm.richness()) {
            byNormalized[norm] = entry
        }

        val exact = entry.word
        val currentExact = byExact[exact]
        if (currentExact == null || entry.richness() > currentExact.richness()) {
            byExact[exact] = entry
        }
    }

    /** Exact-cased match first, then case-normalized. */
    fun lookup(lemma: String): CompactKaikki? =
        byExact[lemma] ?: byNormalized[normalizeLemma(lemma)]
}

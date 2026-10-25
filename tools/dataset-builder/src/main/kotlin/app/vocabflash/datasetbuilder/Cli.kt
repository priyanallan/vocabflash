package app.vocabflash.datasetbuilder

import app.vocabflash.datasetbuilder.enrich.EnrichLimits
import java.nio.file.Path

/**
 * Tiny hand-rolled argument parser (no CLI framework, keeping the module dependency-light).
 *
 * Every path is supplied on the command line — nothing personal is hard-coded. See `--help`.
 */
object Cli {

    val USAGE: String = """
        dataset-builder — Wordlist→Kaikki dataset pipeline

        Usage:
          dataset-builder --wordlist <file.tsv> --kaikki <de-extract.jsonl[.gz]> --out <dir> [options]

        Required:
          --wordlist <path>          Normalized level-tagged wordlist (TSV/CSV).
          --kaikki <path>          Kaikki/Wiktextract German extract (.jsonl or .jsonl.gz).
          --out <dir>              Output directory for dataset.json / manifest.json / qa-report.md.

        Options:
          --tatoeba <path>         Prepared Tatoeba sentence-pair TSV (de<TAB>en). Optional.
          --odenet <path>          Prepared OdeNet relation TSV (lemma<TAB>relation<TAB>related). Optional.
          --simple-case <path>     Tatoeba-derived case-example TSV (lemma<TAB>case<TAB>sentence<TAB>gloss).
                                   Optional. Preferred over --ud when both are supplied.
          --ud <path>              Universal Dependencies German GSD CoNLL-U file. Optional; used as a
                                   fallback for case examples only.
          --dataset-version <s>    Version string stamped into the Dataset (default: "dev").
          --levels <list>          Comma-separated CEFR levels to include (default: A1,A2,B1).
          --max-translations <n>   Keep at most n translations per entry (default: 5).
          --max-examples <n>       Keep at most n Kaikki examples per entry (default: 3).
          --max-tatoeba-examples <n> Keep at most n Tatoeba examples per entry (default: 5).
          --max-related <n>        Keep at most n OdeNet relations per entry (default: 6).
          --max-ipa <n>            Keep at most n IPA strings per entry (default: 3).
          --limit <n>              Only process the first n wordlist rows (smoke testing).
          -h, --help               Show this help.

        Notes:
          • The optional enrichment sources (--tatoeba/--odenet/--simple-case/--ud) are absent-safe:
            if a path is missing or unreadable the pipeline still runs and simply omits those fields.
          • Case examples come from --simple-case (Tatoeba-derived, simpler language) when present;
            --ud is only a fallback for lemmas without simple-case evidence.
          • The wordlist is supplied by the caller; the repository ships none.
          • Only openly licensed datasets belong in the repository (see README).
    """.trimIndent()

    fun parse(args: Array<String>): PipelineConfig? {
        if (args.isEmpty() || args.any { it == "-h" || it == "--help" }) {
            println(USAGE)
            return null
        }

        val opts = HashMap<String, String>()
        var i = 0
        while (i < args.size) {
            val key = args[i]
            require(key.startsWith("--")) { "Unexpected argument: $key" }
            val value = args.getOrNull(i + 1) ?: error("Missing value for $key")
            opts[key.removePrefix("--")] = value
            i += 2
        }

        val wordlist = requirePath(opts, "wordlist")
        val kaikki = requirePath(opts, "kaikki")
        val out = requirePath(opts, "out")

        val limits = EnrichLimits(
            maxTranslations = opts["max-translations"]?.toIntOrNull() ?: 5,
            maxExamples = opts["max-examples"]?.toIntOrNull() ?: 3,
            maxIpa = opts["max-ipa"]?.toIntOrNull() ?: 3,
            maxTatoebaExamples = opts["max-tatoeba-examples"]?.toIntOrNull() ?: 5,
            maxRelated = opts["max-related"]?.toIntOrNull() ?: 6,
        )

        return PipelineConfig(
            wordlistPath = wordlist,
            kaikkiPath = kaikki,
            outputDir = out,
            datasetVersion = opts["dataset-version"] ?: "dev",
            levels = (opts["levels"] ?: "A1,A2,B1").split(',').map { it.trim().uppercase() }.filter { it.isNotEmpty() },
            limits = limits,
            rowLimit = opts["limit"]?.toIntOrNull(),
            tatoebaPath = opts["tatoeba"]?.let { Path.of(it) },
            odenetPath = opts["odenet"]?.let { Path.of(it) },
            udPath = opts["ud"]?.let { Path.of(it) },
            simpleCasePath = opts["simple-case"]?.let { Path.of(it) },
        )
    }

    private fun requirePath(opts: Map<String, String>, name: String): Path {
        val v = opts[name] ?: error("Missing required option --$name (see --help)")
        return Path.of(v)
    }
}

package app.vocabflash.datasetbuilder

import kotlin.system.exitProcess

/**
 * Entry point. Parses CLI args, runs the [Pipeline] and prints a short QA summary.
 *
 * Exit codes: 0 success, 2 usage error, 1 any other failure.
 */
fun main(args: Array<String>) {
    val config = try {
        Cli.parse(args) ?: return // help / no args
    } catch (e: IllegalArgumentException) {
        System.err.println("Argument error: ${e.message}")
        System.err.println(Cli.USAGE)
        exitProcess(2)
    }

    val result = try {
        Pipeline().run(config)
    } catch (e: Exception) {
        System.err.println("Pipeline failed: ${e.message}")
        exitProcess(1)
    }

    val qa = result.qa
    println("Dataset built: ${qa.emittedEntries} entries (version '${qa.datasetVersion}')")
    println("  matched:   ${qa.matched}/${qa.wordlistRows} (${"%.1f".format(qa.matchRate * 100)}%)")
    println("  unmatched: ${qa.unmatched}")
    println("  per level: ${qa.perLevel.toSortedMap().entries.joinToString { "${it.key}=${it.value}" }}")
    println("  Kaikki:    ${qa.kaikkiLinesRead} lines read, ${qa.kaikkiLinesSkipped} skipped")
    println("  written:")
    result.writtenFiles.forEach { println("    - $it") }
}

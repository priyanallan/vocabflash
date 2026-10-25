package com.priyanallan.vocabflash.db

import app.cash.sqldelight.db.SqlDriver

/** Creates the platform SQLite driver for [VocabDatabase] (D40 / ADR-0003 seam). */
expect class DatabaseDriverFactory {
    fun createDriver(): SqlDriver
}

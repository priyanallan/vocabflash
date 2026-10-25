package com.priyanallan.vocabflash

import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import com.priyanallan.vocabflash.db.DatabaseDriverFactory
import com.priyanallan.vocabflash.db.ProgressRepository

@Suppress("unused", "FunctionName")
fun MainViewController() = ComposeUIViewController {
    val repository = remember { ProgressRepository(DatabaseDriverFactory().createDriver()) }
    App(repository)
}

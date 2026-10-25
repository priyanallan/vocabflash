package com.priyanallan.vocabflash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.priyanallan.vocabflash.db.DatabaseDriverFactory
import com.priyanallan.vocabflash.db.ProgressRepository

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val repository = ProgressRepository(
            DatabaseDriverFactory(applicationContext).createDriver(),
        )

        setContent {
            App(repository)
        }
    }
}

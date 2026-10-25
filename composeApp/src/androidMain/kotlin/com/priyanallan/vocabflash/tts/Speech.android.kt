package com.priyanallan.vocabflash.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/** Android speech via the platform TextToSpeech engine (D42). */
private class AndroidSpeech(context: Context) : Speech {
    private var engine: TextToSpeech? = null
    private var ready = false
    private var pending: String? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.GERMAN
                ready = true
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    override fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    override fun stop() {
        engine?.stop()
    }
}

@Composable
actual fun rememberSpeech(): Speech {
    val context = LocalContext.current
    return remember { AndroidSpeech(context) }
}

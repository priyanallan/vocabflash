package com.priyanallan.vocabflash.tts

import androidx.compose.runtime.Composable

/**
 * Platform speech (D42). Android uses `TextToSpeech`, iOS uses `AVSpeechSynthesizer`.
 * Shared UI depends only on this interface.
 */
interface Speech {
    /** Speak [text] aloud; nouns are spoken with their article. */
    fun speak(text: String)
    fun stop()
}

/** The platform Speech implementation. */
@Composable
expect fun rememberSpeech(): Speech

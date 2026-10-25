package com.priyanallan.vocabflash.tts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechSynthesisVoice

/**
 * iOS speech via AVSpeechSynthesizer (D42).
 * NOTE: written for macOS/Xcode builds; not compiled on Linux. Verify bindings on a Mac.
 */
private class IosSpeech : Speech {
    private val synth = AVSpeechSynthesizer()

    override fun speak(text: String) {
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage("de-DE")
        synth.speakUtterance(utterance)
    }

    override fun stop() {
        synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}

@Composable
actual fun rememberSpeech(): Speech = remember { IosSpeech() }

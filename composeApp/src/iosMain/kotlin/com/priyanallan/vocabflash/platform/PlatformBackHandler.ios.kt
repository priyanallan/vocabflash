package com.priyanallan.vocabflash.platform

import androidx.compose.runtime.Composable

/** iOS has no system back button; the app's own navigation affordances handle it. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
    // no-op
}

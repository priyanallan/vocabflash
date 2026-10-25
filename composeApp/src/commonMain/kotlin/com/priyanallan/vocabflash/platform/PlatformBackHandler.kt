package com.priyanallan.vocabflash.platform

import androidx.compose.runtime.Composable

/**
 * Handles the system back gesture/button (D50). Declared in common so the app shell can intercept
 * back navigation; Android supplies the real implementation, iOS a no-op (no system back button).
 */
@Composable
expect fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit)

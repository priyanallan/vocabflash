package com.priyanallan.vocabflash.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.priyanallan.vocabflash.domain.Entry

/**
 * A small modal shown when a related word is tapped: its translation, a **More** button for the
 * full Word Details sheet, and an **X** to close. Sits above the Word Details sheet.
 */
@Composable
fun RelatedWordPeek(
    entry: Entry,
    onClose: () -> Unit,
    onMore: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
                // Consume taps so they never reach the Word Details scrim underneath.
                .pointerInput(Unit) { detectTapGestures { } },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            entry.level.code,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(entry.displayForm, style = MaterialTheme.typography.headlineSmall)
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(44.dp).clickable(onClick = onClose),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("\u00D7", style = MaterialTheme.typography.headlineSmall)
                        }
                    }
                }

                Text(
                    entry.translations.joinToString(", ").ifBlank { "—" },
                    style = MaterialTheme.typography.bodyLarge,
                )
                entry.ipa?.let {
                    Text(
                        "/$it/",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.alpha(0.65f),
                    )
                }

                Spacer(Modifier.height(4.dp))
                Button(onClick = onMore, modifier = Modifier.fillMaxWidth()) { Text("More") }
            }
        }
    }
}

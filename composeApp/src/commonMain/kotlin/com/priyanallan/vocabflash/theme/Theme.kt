package com.priyanallan.vocabflash.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.priyanallan.vocabflash.domain.Rating
import com.priyanallan.vocabflash.resources.Res
import com.priyanallan.vocabflash.resources.poppins_bold
import com.priyanallan.vocabflash.resources.poppins_medium
import com.priyanallan.vocabflash.resources.poppins_regular
import com.priyanallan.vocabflash.resources.poppins_semibold
import org.jetbrains.compose.resources.Font

// ── Klarr palette (D29) ────────────────────────────────────────────────────────
val Purple = Color(0xFF7C5CFF)
val Coral = Color(0xFFFFB199)
val Lavender = Color(0xFFC9B8FF)
val OffWhite = Color(0xFFFDFCFA)
val Ink = Color(0xFF1A1A1F)
private val DeepNeutral = Color(0xFF141317)
private val DeepNeutral2 = Color(0xFF1E1C22)

// Rating pill accents (Klarr reference colours the four buttons differently).
private val RatingAgain = Color(0xFFFF8A80)
private val RatingHard = Color(0xFFFFC46B)
private val RatingGood = Color(0xFF8BD8A5)
private val RatingEasy = Color(0xFF8FB8FF)

fun ratingColor(rating: Rating): Color = when (rating) {
    Rating.Again -> RatingAgain
    Rating.Hard -> RatingHard
    Rating.Good -> RatingGood
    Rating.Easy -> RatingEasy
}

fun difficultyColor(difficulty: com.priyanallan.vocabflash.domain.Difficulty): Color = when (difficulty) {
    com.priyanallan.vocabflash.domain.Difficulty.New -> Color(0xFF9E9AAE)
    com.priyanallan.vocabflash.domain.Difficulty.Easy -> RatingGood
    com.priyanallan.vocabflash.domain.Difficulty.Medium -> RatingHard
    com.priyanallan.vocabflash.domain.Difficulty.Hard -> RatingAgain
}

val KlarrGradient: Brush = Brush.linearGradient(listOf(Coral, Lavender))

val KlarrShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(28.dp),
)

private val LightScheme = lightColorScheme(
    primary = Purple,
    onPrimary = Color.White,
    secondary = Lavender,
    surface = OffWhite,
    onSurface = Ink,
    surfaceVariant = Color(0xFFF1EEFB),
    onSurfaceVariant = Color(0xFF5A5666),
    background = OffWhite,
    onBackground = Ink,
)

private val DarkScheme = darkColorScheme(
    primary = Lavender,
    onPrimary = Color(0xFF201A33),
    secondary = Purple,
    surface = DeepNeutral2,
    onSurface = Color(0xFFEDEDF2),
    surfaceVariant = Color(0xFF2A2733),
    onSurfaceVariant = Color(0xFFB9B5C6),
    background = DeepNeutral,
    onBackground = Color(0xFFEDEDF2),
)

/** Poppins for the whole type scale (D30). */
@Composable
private fun klarrTypography(): Typography {
    val poppins = FontFamily(
        Font(Res.font.poppins_regular, FontWeight.Normal),
        Font(Res.font.poppins_medium, FontWeight.Medium),
        Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        Font(Res.font.poppins_bold, FontWeight.Bold),
    )
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = poppins),
        displayMedium = base.displayMedium.copy(fontFamily = poppins),
        displaySmall = base.displaySmall.copy(fontFamily = poppins),
        headlineLarge = base.headlineLarge.copy(fontFamily = poppins, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = poppins, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = poppins, fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontFamily = poppins, fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontFamily = poppins),
        titleSmall = base.titleSmall.copy(fontFamily = poppins),
        bodyLarge = base.bodyLarge.copy(fontFamily = poppins),
        bodyMedium = base.bodyMedium.copy(fontFamily = poppins),
        bodySmall = base.bodySmall.copy(fontFamily = poppins),
        labelLarge = base.labelLarge.copy(fontFamily = poppins, fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium.copy(fontFamily = poppins),
        labelSmall = base.labelSmall.copy(fontFamily = poppins),
    )
}

@Composable
fun KlarrTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = klarrTypography(),
        shapes = KlarrShapes,
        content = content,
    )
}

/** Full-screen background. Light shows the pastel Klarr identity; dark stays clean. */
@Composable
fun KlarrBackground(darkTheme: Boolean, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val brush = if (darkTheme) {
        Brush.linearGradient(listOf(DeepNeutral, DeepNeutral2))
    } else {
        Brush.linearGradient(
            colors = listOf(
                Coral.copy(alpha = 0.70f),
                Lavender.copy(alpha = 0.62f),
                Color(0xFFDCE9FF).copy(alpha = 0.66f),
            ),
        )
    }
    Box(modifier.fillMaxSize().background(brush)) { content() }
}

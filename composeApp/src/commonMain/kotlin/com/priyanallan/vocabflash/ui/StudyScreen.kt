package com.priyanallan.vocabflash.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.priyanallan.vocabflash.db.ProgressRepository
import com.priyanallan.vocabflash.domain.DEFAULT_NEW_PER_DAY
import com.priyanallan.vocabflash.domain.Entry
import com.priyanallan.vocabflash.domain.Fsrs
import com.priyanallan.vocabflash.domain.QueueInfo
import com.priyanallan.vocabflash.domain.Rating
import com.priyanallan.vocabflash.domain.ScheduleState
import com.priyanallan.vocabflash.domain.StudyCard
import com.priyanallan.vocabflash.domain.StudyMode
import com.priyanallan.vocabflash.domain.buildStudyQueue
import com.priyanallan.vocabflash.domain.localDayKey
import com.priyanallan.vocabflash.domain.spaceOut
import com.priyanallan.vocabflash.theme.ratingColor
import com.priyanallan.vocabflash.tts.rememberSpeech
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val DAY_MS = 86_400_000L
private const val SWIPE_PX = 140f
private val ARTICLES = listOf("der", "die", "das")

/**
 * Distraction-free study flow.
 *
 * - Recognition / Production: tap the Card to flip and reveal the answer.
 * - Gender drill: a 3-button der/die/das quiz, then rate.
 * - Swipe **left = next**, **right = previous** (clamped) — D53.
 * - The progress bar is **position-based** and the counter shows Cards still unrated.
 * - Ratings are **editable**: navigate back and the chosen rating is shown selected (D52).
 */
@OptIn(ExperimentalTime::class)
@Composable
fun StudyScreen(
    cards: List<StudyCard>,
    repository: ProgressRepository?,
    applyDailyLimit: Boolean = true,
    lookupEntry: (String) -> Entry? = { null },
    onDone: () -> Unit,
) {
    val speech = rememberSpeech()

    val now = remember { Clock.System.now().toEpochMilliseconds() }
    val day = remember(now) { localDayKey(now) }
    val newPerDay = remember(repository) { repository?.newPerDay() ?: DEFAULT_NEW_PER_DAY }
    val introducedToday = remember(repository, day) { repository?.newIntroduced(day) ?: 0 }

    val scheduleByCard = remember(cards) {
        cards.associateWith { repository?.load(it.entry.id, it.mode.name) ?: ScheduleState() }
    }

    // D14 daily queue, then D49 spacing so an Entry's modes are never adjacent.
    val session = remember(cards, applyDailyLimit, now) {
        // Randomise the presentation order so a session does not always start with "aber".
        val shuffled = cards.shuffled()
        val queue = if (!applyDailyLimit) {
            shuffled
        } else {
            buildStudyQueue(
                cards = shuffled,
                infoOf = { card ->
                    scheduleByCard[card]?.let { QueueInfo(it.reps, it.due) } ?: QueueInfo.New
                },
                now = now,
                newPerDay = newPerDay,
                newIntroducedToday = introducedToday,
            ).all
        }
        spaceOut(queue)
    }

    if (session.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Text(
                if (applyDailyLimit) "You're done for today — come back tomorrow."
                else "Nothing to review.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
        return
    }

    var index by remember(session) { mutableIntStateOf(0) }
    var revealed by remember(session) { mutableStateOf(false) }
    var newToday by remember(session) { mutableIntStateOf(introducedToday) }

    // Pre-rating state per Card, so a changed rating is always derived from the original (D52).
    val baseStates = remember(session) { session.map { scheduleByCard[it] ?: ScheduleState() } }
    val ratings = remember(session) { mutableStateListOf<Rating?>().apply { repeat(session.size) { add(null) } } }
    val currentStates = remember(session) { mutableStateListOf<ScheduleState>().apply { addAll(baseStates) } }
    val countedNew = remember(session) { mutableStateListOf<Boolean>().apply { repeat(session.size) { add(false) } } }

    var revealedReset by remember(session) { mutableIntStateOf(0) }

    val card = session[index]
    val entry = card.entry
    val current = currentStates[index]
    val elapsedDays = remember(current) {
        current.lastReview?.let { ((Clock.System.now().toEpochMilliseconds() - it) / DAY_MS).toInt() } ?: 0
    }
    val previews = remember(current) { Fsrs.previewIntervals(current, elapsedDays) }

    val isQuiz = card.mode == StudyMode.GenderDrill
    var chosen by remember(index) { mutableStateOf<String?>(null) }

    // Word Details (D51): a stack of visited sheets. Opening a related word's sheet pushes;
    // × pops back to the previous sheet, and only the last × returns to the cards.
    val detailsStack = remember { mutableStateListOf<Entry>() }
    // Related-word peek modal, shown above the top details sheet.
    var peekEntry by remember { mutableStateOf<Entry?>(null) }

    // D53: progress reflects position; counter shows Cards still unrated.
    val unrated = ratings.count { it == null }

    val justRated = remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(index, revealed, revealedReset) {
        when {
            isQuiz -> rotation.snapTo(0f)
            revealed -> rotation.animateTo(180f, tween(durationMillis = 420))
            justRated.value -> {
                rotation.snapTo(0f)
                justRated.value = false
            }
            else -> rotation.animateTo(0f, tween(durationMillis = 420))
        }
    }

    if (unrated == 0) {
        SessionDone(sessionSize = session.size, onDone = onDone)
        return
    }

    val submit: (Rating) -> Unit = { rating ->
        val base = baseStates[index]
        val baseElapsed = base.lastReview
            ?.let { ((Clock.System.now().toEpochMilliseconds() - it) / DAY_MS).toInt() } ?: 0
        val next = Fsrs.nextState(base, baseElapsed, rating)
        val nowMs = Clock.System.now().toEpochMilliseconds()
        val stored = next.copy(lastReview = nowMs, due = nowMs + next.scheduledDays * DAY_MS)

        currentStates[index] = stored
        ratings[index] = rating
        // First-ever review of this Card counts against today's new-Card allowance (D14).
        if (base.reps == 0 && !countedNew[index] && repository != null) {
            countedNew[index] = true
            newToday += 1
            repository.setNewIntroduced(day, newToday)
        }
        repository?.let { repo ->
            repo.save(entry.id, card.mode.name, stored)
            when (rating) {
                Rating.Again -> repo.setFlagged(entry.id, card.mode.name, true)
                Rating.Good, Rating.Easy -> repo.setFlagged(entry.id, card.mode.name, false)
                Rating.Hard -> Unit
            }
        }
        justRated.value = true
        revealed = false
        revealedReset += 1
        // Advance to the next Card that is still unrated (if any).
        val nextUnrated = session.indices.firstOrNull { it > index && ratings[it] == null }
            ?: session.indices.firstOrNull { ratings[it] == null }
        if (nextUnrated != null) index = nextUnrated
    }

    var dragX by remember { mutableFloatStateOf(0f) }
    val settle = remember { Animatable(0f) }
    var animating by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val slideDistance = with(LocalDensity.current) { 460.dp.toPx() }
    // Live offset: during a drag it is dragX, during the settle animation the animated value.
    val shownOffset = if (animating) settle.value else dragX
    // "More" belongs to the answer side only: after the flip lands, or after the quiz is answered.
    val moreVisible = if (isQuiz) chosen != null else rotation.value > 90f

    /** Animate the current card out, move to [target], then slide the new card in.
     *  The incoming card always starts face-down: [revealed] is shared across the session,
     *  so without the reset a card swiped away while revealed would leave the next card
     *  revealed too. */
    fun slideTo(target: Int, direction: Float) {
        scope.launch {
            animating = true
            settle.snapTo(dragX)
            settle.animateTo(direction * slideDistance, tween(durationMillis = 150))
            revealed = false
            rotation.snapTo(0f)
            index = target
            settle.snapTo(-direction * slideDistance)
            settle.animateTo(0f, tween(durationMillis = 220))
            dragX = 0f
            animating = false
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Daily progress (D44/D53): counter top-right + position bar.
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "card ${index + 1} of ${session.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.alpha(0.55f),
            )
            Spacer(Modifier.weight(1f))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    "$unrated left",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        LinearProgressIndicator(
            progress = { (index + 1).toFloat() / session.size },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // Translation lives in the OUTER layer on purpose: a card rotated 180° mirrors
                    // its X axis, which would otherwise reverse the drag direction after flipping.
                    translationX = shownOffset
                    alpha = 1f - (abs(shownOffset) / slideDistance).coerceIn(0f, 1f) * 0.35f
                }
                // Gestures live here too — NOT on the rotated card: a rotated node receives
                // mirrored X deltas, so a left-swipe read as a right-swipe once flipped.
                .pointerInput(session) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val travelled = dragX
                            animating = false
                            when {
                                travelled <= -SWIPE_PX && index < session.size - 1 ->
                                    slideTo(index + 1, direction = -1f)
                                travelled >= SWIPE_PX && index > 0 ->
                                    slideTo(index - 1, direction = 1f)
                                else -> scope.launch {
                                    animating = true
                                    settle.snapTo(travelled)
                                    settle.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                    dragX = 0f
                                    animating = false
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                animating = true
                                settle.snapTo(dragX)
                                settle.animateTo(0f, spring(stiffness = Spring.StiffnessMedium))
                                dragX = 0f
                                animating = false
                            }
                        },
                        onHorizontalDrag = { _, dragAmount -> dragX += dragAmount },
                    )
                }
                .pointerInput(isQuiz, revealed) {
                    if (!isQuiz) detectTapGestures { revealed = !revealed }
                },
        ) {
            Card(
                shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    // Rotation only — translation and gestures live on the outer layer.
                    rotationY = if (isQuiz) 0f else rotation.value
                    cameraDistance = 12f * density
                },
        ) {
            Box(
                Modifier.background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    when {
                        isQuiz -> GenderPrompt(entry)
                        rotation.value <= 90f -> CardFront(entry, card.mode)
                        else -> Column(
                            modifier = Modifier.graphicsLayer { rotationY = 180f },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            CardBack(entry, card.mode, onSpeak = { speech.speak(entry.displayForm) })
                        }
                    }
                }
            }
            }

            // More (D51/Q2): on the ANSWER side only, overlaid bottom-right and OUTSIDE the rotated
            // layer so the label never appears mirrored when the card flips.
            if (moreVisible) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.92f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        // Comfortable touch target: the pill itself plus breathing room, so a
                        // slightly-off tap opens details instead of flipping the card.
                        .padding(end = 10.dp, bottom = 10.dp)
                        .sizeIn(minWidth = 56.dp, minHeight = 44.dp)
                        .clickable { detailsStack.add(entry) },
                ) {
                    Text(
                        "More",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    )
                }
            }
        }

        Spacer(Modifier.weight(1f))

        when {
            isQuiz && chosen == null -> ArticleChoices(onChoose = { chosen = it })
            isQuiz -> {
                GenderFeedback(entry, chosen!!)
                RatingRow(previews = previews, selected = ratings[index], onRate = submit)
            }
            revealed -> RatingRow(previews = previews, selected = ratings[index], onRate = submit)
            else -> Text(
                when {
                    shownOffset <= -SWIPE_PX -> "Release for the next card →"
                    shownOffset >= SWIPE_PX -> "← Release to go back"
                    else -> "Tap to reveal · swipe left/right to move"
                },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.alpha(if (shownOffset != 0f) 0.9f else 0.55f),
            )
        }
    }

    detailsStack.lastOrNull()?.let { shown ->
        WordDetailsSheet(
            entry = shown,
            // While a related-word peek is open, the sheet must not close underneath it.
            onClose = {
                if (peekEntry == null) {
                    if (detailsStack.size > 1) detailsStack.removeAt(detailsStack.lastIndex)
                    else detailsStack.clear()
                }
            },
            onOpenRelated = { word -> lookupEntry(word)?.let { peekEntry = it } },
        )
    }

    peekEntry?.let { related ->
        RelatedWordPeek(
            entry = related,
            onClose = { peekEntry = null },
            onMore = {
                peekEntry = null
                detailsStack.add(related)
            },
        )
    }
}

/** The whole daily queue is finished. */
@Composable
private fun SessionDone(sessionSize: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Done for today", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "You finished all $sessionSize cards in today's queue.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f),
        )
        Spacer(Modifier.height(24.dp))
        Button(onClick = onDone) { Text("Back to decks") }
    }
}

/** D55: shrink the display word as it gets longer instead of truncating it. */
@Composable
private fun displayStyle(word: String): TextStyle = when {
    word.length > 16 -> MaterialTheme.typography.headlineMedium.copy(letterSpacing = 0.sp)
    word.length > 12 -> MaterialTheme.typography.headlineLarge.copy(letterSpacing = 0.sp)
    else -> MaterialTheme.typography.headlineLarge.copy(letterSpacing = 2.5.sp)
}

/** The quiz prompt: the noun, its meaning, and a nudge to pick the article. */
@Composable
private fun GenderPrompt(entry: Entry) {
    MicroLabel(entry, StudyMode.GenderDrill)
    Text(
        entry.lemma,
        style = displayStyle(entry.lemma),
        textAlign = TextAlign.Center,
    )
    if (entry.translations.isNotEmpty()) {
        Text(
            entry.translations.joinToString(", "),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.7f),
        )
    }
    Text("Which article?", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.55f))
}

/** der / die / das — the learner's answer. */
@Composable
private fun ArticleChoices(onChoose: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ARTICLES.forEach { article ->
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).clickable { onChoose(article) },
            ) {
                Text(
                    article,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                )
            }
        }
    }
}

/** Right/wrong feedback, then the rating pills. */
@Composable
private fun GenderFeedback(entry: Entry, chosen: String) {
    val correct = entry.article
    val isRight = chosen == correct
    val colour = ratingColor(if (isRight) Rating.Good else Rating.Again)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = colour.copy(alpha = 0.30f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                if (isRight) "Correct!" else "Not quite",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "$correct ${entry.lemma}",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
            )
            entry.plural?.let { Text("plural: $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun MicroLabel(entry: Entry, mode: StudyMode) {
    val text = buildString {
        append(entry.level.code)
        entry.topic?.let { append(" · ").append(it) }
        append(" · ").append(mode.label)
    }
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.6.sp),
        modifier = Modifier.alpha(0.55f),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CardFront(entry: Entry, mode: StudyMode) {
    MicroLabel(entry, mode)
    when (mode) {
        StudyMode.Production -> {
            Text(
                entry.translations.firstOrNull() ?: entry.lemma,
                style = displayStyle(entry.translations.firstOrNull() ?: entry.lemma),
                textAlign = TextAlign.Center,
            )
            Text("In German?", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.6f))
        }
        else -> Text(
            entry.displayForm,
            style = displayStyle(entry.displayForm),
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun CardBack(entry: Entry, mode: StudyMode, onSpeak: () -> Unit) {
    MicroLabel(entry, mode)
    val headline = if (mode == StudyMode.Recognition) {
        entry.translations.joinToString(", ")
    } else {
        entry.displayForm
    }
    Text(headline, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)

    // The answer side stays minimal: meaning, plural, pronunciation, audio.
    // Examples and the full word table live in Word Details (behind "More").
    if (mode != StudyMode.Recognition && entry.translations.isNotEmpty()) {
        Text(entry.translations.joinToString(", "), style = MaterialTheme.typography.bodyLarge)
    }
    entry.ipa?.let {
        Text("/$it/", style = MaterialTheme.typography.bodySmall, modifier = Modifier.alpha(0.65f))
    }
    TextButton(onClick = onSpeak) { Text("\uD83D\uDD0A Listen") }
    Text(
        entry.partOfSpeech.name,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.alpha(0.6f),
    )
}

/**
 * Four colour-coded rating pills, each showing its predicted interval (D31).
 * [selected] marks the rating already chosen for this Card, so it can be changed (D52).
 */
@Composable
private fun RatingRow(previews: Map<Rating, Int>, selected: Rating?, onRate: (Rating) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Rating.entries.forEach { rating ->
            val isSelected = rating == selected
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = ratingColor(rating).copy(alpha = if (isSelected) 0.75f else 0.30f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).clickable { onRate(rating) },
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(rating.label, style = MaterialTheme.typography.labelLarge)
                    Text(
                        formatInterval(previews[rating] ?: 0),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.alpha(0.7f),
                    )
                }
            }
        }
    }
}

private fun formatInterval(days: Int): String = when {
    days <= 0 -> "now"
    days == 1 -> "1d"
    days < 30 -> "${days}d"
    days < 365 -> "${days / 30}mo"
    else -> "${days / 365}y"
}

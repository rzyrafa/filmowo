package pl.filmowo.core.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Ruch w Filmowo: krótko i miękko. Drobne zmiany stanu 120–200 ms,
 * przejścia ekranów i przesuwane wskaźniki ~320 ms z wyraźnym wyhamowaniem.
 */
object FilmowoMotion {
    const val SHORT = 120
    const val MEDIUM = 200
    const val LONG = 320

    /** Szybki start, długie wyhamowanie – domyślna krzywa dla ruchu w przestrzeni. */
    val Emphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Łagodne wejście/wyjście dla kolorów i przezroczystości. */
    val Standard = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

    fun <T> color(): FiniteAnimationSpec<T> = tween(MEDIUM, easing = Standard)

    fun <T> fade(duration: Int = MEDIUM, delay: Int = 0): FiniteAnimationSpec<T> =
        tween(duration, delayMillis = delay, easing = Standard)

    fun <T> move(duration: Int = LONG): FiniteAnimationSpec<T> = tween(duration, easing = Emphasized)

    /** Delikatne sprężynowanie dla małych elementów (✓, wciśnięcie) – bez widocznego bujania. */
    fun <T> pop(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * Wejście kaskadowe: element wyłania się z przezroczystości i podjeżdża o 24dp,
 * z opóźnieniem zależnym od [index] – kolejne kafelki pojawiają się po sobie.
 */
@Composable
fun Modifier.staggeredAppear(index: Int, stepMillis: Long = 70L): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * stepMillis)
        progress.animateTo(1f, FilmowoMotion.move())
    }
    return graphicsLayer {
        alpha = progress.value
        translationY = (1f - progress.value) * 24.dp.toPx()
    }
}

/** Lekkie zmniejszenie (do 0.97) przy wciśnięciu – jak w design systemie. */
@Composable
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.97f): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = FilmowoMotion.pop(),
        label = "pressScale",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

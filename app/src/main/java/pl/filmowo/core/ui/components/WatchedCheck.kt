package pl.filmowo.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.ui.theme.FilmowoMotion
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme

/**
 * Pole odhaczenia 32dp: puste z obrysem line-strong, po obejrzeniu miętowe z ✓,
 * który „wskakuje” ze sprężynką. Tylko wygląd – kliknięcie obsługuje cały wiersz.
 */
@Composable
fun WatchedCheck(checked: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = FilmowoTheme.colors
    val background by animateColorAsState(if (checked) colors.mint else colors.surfaceRaised, FilmowoMotion.color(), label = "checkBg")
    val border by animateColorAsState(if (checked) colors.onMint else colors.lineStrong, FilmowoMotion.color(), label = "checkBorder")
    val alpha by animateFloatAsState(if (enabled) 1f else 0.4f, FilmowoMotion.fade(), label = "checkAlpha")
    Box(
        modifier = modifier
            .size(32.dp)
            .alpha(alpha)
            .clip(FilmowoRadius.Sm)
            .background(background)
            .border(2.dp, border, FilmowoRadius.Sm),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            visible = checked,
            enter = scaleIn(FilmowoMotion.pop(), initialScale = 0.4f) + fadeIn(FilmowoMotion.fade(FilmowoMotion.SHORT)),
            exit = scaleOut(FilmowoMotion.fade(FilmowoMotion.SHORT), targetScale = 0.6f) + fadeOut(FilmowoMotion.fade(FilmowoMotion.SHORT)),
        ) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onMint)
        }
    }
}

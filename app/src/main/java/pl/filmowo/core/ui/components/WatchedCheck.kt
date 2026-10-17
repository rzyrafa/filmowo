package pl.filmowo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme

/**
 * Pole odhaczenia 32dp: puste z obrysem line-strong, po obejrzeniu miętowe z ✓.
 * Tylko wygląd – obsługę kliknięcia (toggleable) daje cały wiersz.
 */
@Composable
fun WatchedCheck(checked: Boolean, enabled: Boolean, modifier: Modifier = Modifier) {
    val colors = FilmowoTheme.colors
    Box(
        modifier = modifier
            .size(32.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clip(FilmowoRadius.Sm)
            .background(if (checked) colors.mint else colors.surfaceRaised)
            .border(2.dp, if (checked) colors.onMint else colors.lineStrong, FilmowoRadius.Sm),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(18.dp), tint = colors.onMint)
        }
    }
}

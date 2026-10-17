package pl.filmowo.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.ToneName
import pl.filmowo.core.ui.theme.tone

/**
 * Pusty stan: przechylony pastelowy kafelek z ikoną, jedno zdanie
 * i opcjonalna akcja pod spodem ([action]).
 */
@Composable
fun PlaceholderContent(
    @DrawableRes iconRes: Int,
    text: String,
    modifier: Modifier = Modifier,
    tone: ToneName = ToneName.BUTTER,
    action: (@Composable () -> Unit)? = null,
) {
    val colors = FilmowoTheme.colors
    val t = colors.tone(tone)
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .rotate(-6f)
                .background(t.container, FilmowoRadius.Lg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = t.content,
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = colors.inkMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp),
        )
        action?.invoke()
    }
}

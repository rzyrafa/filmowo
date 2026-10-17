package pl.filmowo.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.filmowo.core.ui.theme.FilmowoMotion
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.pressScale

data class SegmentedTab(val label: String, val count: Int? = null)

private val TabSpacing = 4.dp

/**
 * Przełącznik segmentowy (zakładki Biblioteki): biały segment przesuwa się
 * pod wybraną zakładkę, a jej licznik płynnie przechodzi w kolor brand.
 */
@Composable
fun SegmentedTabs(
    tabs: List<SegmentedTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FilmowoTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, FilmowoRadius.Pill)
            .padding(4.dp),
    ) {
        val tabWidth = (maxWidth - TabSpacing * (tabs.size - 1)) / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = (tabWidth + TabSpacing) * selectedIndex,
            animationSpec = FilmowoMotion.move(),
            label = "tabIndicator",
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(40.dp)
                .shadow(2.dp, FilmowoRadius.Pill)
                .background(colors.surfaceRaised, FilmowoRadius.Pill),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(TabSpacing),
        ) {
            tabs.forEachIndexed { index, tab ->
                TabItem(
                    tab = tab,
                    selected = index == selectedIndex,
                    onClick = { onSelect(index) },
                    modifier = Modifier.width(tabWidth),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    tab: SegmentedTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FilmowoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val labelColor by animateColorAsState(if (selected) colors.ink else colors.inkMuted, FilmowoMotion.color(), label = "tabLabel")
    val countBg by animateColorAsState(if (selected) colors.brand else colors.surfaceRaised, FilmowoMotion.color(), label = "tabCountBg")
    val countFg by animateColorAsState(if (selected) colors.onBrand else colors.inkMuted, FilmowoMotion.color(), label = "tabCountFg")
    Row(
        modifier = modifier
            .height(40.dp)
            .pressScale(interaction)
            .clip(FilmowoRadius.Pill)
            .selectable(selected = selected, interactionSource = interaction, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            tab.label,
            style = MaterialTheme.typography.labelLarge,
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (tab.count != null) {
            Box(
                modifier = Modifier
                    .defaultMinSize(minWidth = 20.dp)
                    .background(countBg, FilmowoRadius.Pill)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                // Zmiana licznika: nowa liczba wjeżdża z dołu (przy wzroście) albo z góry.
                AnimatedContent(
                    targetState = tab.count,
                    transitionSpec = {
                        val up = targetState > initialState
                        (slideInVertically(FilmowoMotion.move()) { if (up) it else -it } + fadeIn(FilmowoMotion.fade()))
                            .togetherWith(slideOutVertically(FilmowoMotion.move()) { if (up) -it else it } + fadeOut(FilmowoMotion.fade()))
                    },
                    label = "tabCount",
                ) { count ->
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = countFg)
                }
            }
        }
    }
}

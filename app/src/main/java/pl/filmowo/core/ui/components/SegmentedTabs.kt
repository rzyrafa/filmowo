package pl.filmowo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme

data class SegmentedTab(val label: String, val count: Int? = null)

/**
 * Przełącznik segmentowy (zakładki Biblioteki): aktywny segment unosi się
 * na surface-raised, a jego licznik dostaje kolor brand.
 */
@Composable
fun SegmentedTabs(
    tabs: List<SegmentedTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FilmowoTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surfaceSunken, FilmowoRadius.Pill)
            .padding(4.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = index == selectedIndex
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .then(if (selected) Modifier.shadow(2.dp, FilmowoRadius.Pill) else Modifier)
                    .clip(FilmowoRadius.Pill)
                    .background(if (selected) colors.surfaceRaised else colors.surfaceSunken)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(index) })
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) colors.ink else colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (tab.count != null) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 20.dp)
                            .background(if (selected) colors.brand else colors.surfaceRaised, FilmowoRadius.Pill)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            tab.count.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (selected) colors.onBrand else colors.inkMuted,
                        )
                    }
                }
            }
        }
    }
}

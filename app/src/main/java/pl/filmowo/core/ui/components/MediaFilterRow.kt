package pl.filmowo.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.ui.theme.FilmowoMotion
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.domain.model.MediaFilter

/** „Wszystko / Filmy / Seriale” – wybrany chip ma wypełnienie ink i ✓. */
@Composable
fun MediaFilterRow(
    selected: MediaFilter,
    onSelect: (MediaFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FilmowoTheme.colors
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MediaFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            // Kolory przechodzą płynnie zamiast przeskakiwać przy zmianie filtra.
            val container by animateColorAsState(if (isSelected) colors.ink else colors.surfaceRaised, FilmowoMotion.color(), label = "chipBg")
            val label by animateColorAsState(if (isSelected) colors.onInk else colors.ink, FilmowoMotion.color(), label = "chipFg")
            val border by animateColorAsState(if (isSelected) colors.ink else colors.line, FilmowoMotion.color(), label = "chipBorder")
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(filter) },
                shape = FilmowoRadius.Pill,
                leadingIcon = if (isSelected) {
                    { Icon(painterResource(R.drawable.ic_check), contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else {
                    null
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = container,
                    labelColor = label,
                    selectedContainerColor = container,
                    selectedLabelColor = label,
                    selectedLeadingIconColor = label,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = border,
                    selectedBorderColor = border,
                ),
                label = {
                    Text(
                        stringResource(
                            when (filter) {
                                MediaFilter.ALL -> R.string.filter_all
                                MediaFilter.MOVIES -> R.string.filter_movies
                                MediaFilter.SHOWS -> R.string.filter_shows
                            },
                        ),
                    )
                },
            )
        }
    }
}

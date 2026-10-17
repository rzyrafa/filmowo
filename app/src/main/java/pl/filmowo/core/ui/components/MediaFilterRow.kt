package pl.filmowo.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.filmowo.R
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
                    containerColor = colors.surfaceRaised,
                    labelColor = colors.ink,
                    selectedContainerColor = colors.ink,
                    selectedLabelColor = colors.onInk,
                    selectedLeadingIconColor = colors.onInk,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = colors.line,
                    selectedBorderColor = colors.ink,
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

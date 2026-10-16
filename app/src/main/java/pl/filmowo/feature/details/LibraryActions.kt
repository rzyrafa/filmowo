package pl.filmowo.feature.details

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.domain.model.WatchStatus

/**
 * Przyciski „Do obejrzenia” / „Obejrzane” i ocena. Ponowne kliknięcie
 * wybranego statusu pyta o usunięcie z biblioteki.
 */
@Composable
internal fun LibraryActions(
    entry: LibraryEntryState?,
    onStatusSelected: (WatchStatus) -> Unit,
    onRemove: () -> Unit,
    onRatingChange: (Int?) -> Unit,
) {
    var confirmRemove by rememberSaveable { mutableStateOf(false) }
    var showRatingDialog by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusButton(
                selected = entry?.status == WatchStatus.PLANNED,
                iconRes = R.drawable.ic_bookmark,
                labelRes = R.string.status_planned,
                onClick = {
                    if (entry?.status == WatchStatus.PLANNED) confirmRemove = true else onStatusSelected(WatchStatus.PLANNED)
                },
            )
            StatusButton(
                selected = entry?.status == WatchStatus.WATCHED,
                iconRes = R.drawable.ic_check,
                labelRes = R.string.status_watched,
                onClick = {
                    if (entry?.status == WatchStatus.WATCHED) confirmRemove = true else onStatusSelected(WatchStatus.WATCHED)
                },
            )
        }
        if (entry?.status == WatchStatus.WATCHING) {
            Text(
                stringResource(R.string.status_currently_watching),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_star),
                contentDescription = null,
                tint = if (entry?.rating != null) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.width(8.dp))
            if (entry == null) {
                Text(
                    stringResource(R.string.rating_requires_library),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = entry.rating?.let { stringResource(R.string.rating_value, it) }
                        ?: stringResource(R.string.rating_none),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { showRatingDialog = true }) {
                    Text(stringResource(if (entry.rating == null) R.string.rating_add else R.string.rating_change))
                }
            }
        }
    }

    if (confirmRemove) {
        AlertDialog(
            onDismissRequest = { confirmRemove = false },
            title = { Text(stringResource(R.string.remove_title)) },
            text = { Text(stringResource(R.string.remove_message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmRemove = false
                    onRemove()
                }) { Text(stringResource(R.string.action_remove)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemove = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (showRatingDialog && entry != null) {
        RatingDialog(
            current = entry.rating,
            onDismiss = { showRatingDialog = false },
            onSelect = {
                showRatingDialog = false
                onRatingChange(it)
            },
        )
    }
}

@Composable
private fun RowScope.StatusButton(
    selected: Boolean,
    @DrawableRes iconRes: Int,
    @StringRes labelRes: Int,
    onClick: () -> Unit,
) {
    val content: @Composable RowScope.() -> Unit = {
        Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
        Spacer(Modifier.width(ButtonDefaults.IconSpacing))
        Text(stringResource(labelRes))
    }
    if (selected) {
        Button(onClick = onClick, modifier = Modifier.weight(1f), content = content)
    } else {
        OutlinedButton(onClick = onClick, modifier = Modifier.weight(1f), content = content)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingDialog(
    current: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rating_dialog_title)) },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                (1..10).forEach { value ->
                    FilterChip(
                        selected = value == current,
                        onClick = { onSelect(value) },
                        label = { Text(value.toString()) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
        dismissButton = {
            if (current != null) {
                TextButton(onClick = { onSelect(null) }) { Text(stringResource(R.string.rating_remove)) }
            }
        },
    )
}

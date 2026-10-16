package pl.filmowo.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.MediaFilterRow
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.Poster
import pl.filmowo.core.ui.components.TopLevelTopBar
import pl.filmowo.data.remote.TmdbImages
import pl.filmowo.domain.model.LibraryItem
import pl.filmowo.domain.model.MediaType
import java.io.File

@Composable
fun LibraryScreen(
    onOpenAbout: () -> Unit,
    onOpenItem: (id: Int, type: MediaType) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopLevelTopBar(
                title = stringResource(R.string.nav_library),
                onOpenAbout = onOpenAbout,
                actions = { SortMenu(state.sort, viewModel::onSortSelected) },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
                LibraryTab.entries.forEach { tab ->
                    Tab(
                        selected = tab == state.tab,
                        onClick = { viewModel.onTabSelected(tab) },
                        text = {
                            Text(
                                stringResource(R.string.library_tab_with_count, stringResource(tab.labelRes), state.counts[tab] ?: 0),
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
            MediaFilterRow(selected = state.filter, onSelect = viewModel::onFilterSelected)
            when {
                state.isLoading -> LoadingContent()
                state.items.isEmpty() -> PlaceholderContent(
                    iconRes = R.drawable.ic_video_library,
                    text = stringResource(state.tab.emptyRes),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.items, key = { "${it.mediaType}-${it.tmdbId}" }) { item ->
                        LibraryRow(item, onClick = { onOpenItem(item.tmdbId, item.mediaType) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SortMenu(current: LibrarySort, onSelect: (LibrarySort) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(painterResource(R.drawable.ic_sort), stringResource(R.string.sort_title))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LibrarySort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes)) },
                    onClick = {
                        expanded = false
                        onSelect(sort)
                    },
                    trailingIcon = {
                        if (sort == current) Icon(painterResource(R.drawable.ic_check), contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun LibraryRow(item: LibraryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(
            model = item.localPosterFile?.let(::File) ?: TmdbImages.posterSmall(item.metadata.posterPath),
            contentDescription = null,
            modifier = Modifier.width(56.dp),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.metadata.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val type = stringResource(if (item.mediaType == MediaType.MOVIE) R.string.media_movie else R.string.media_show)
            Text(
                text = listOfNotNull(type, item.metadata.year?.toString()).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (item.rating != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painterResource(R.drawable.ic_star),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    stringResource(R.string.rating_short, item.rating),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

private val LibraryTab.labelRes: Int
    get() = when (this) {
        LibraryTab.WATCHING -> R.string.library_tab_watching
        LibraryTab.PLANNED -> R.string.library_tab_planned
        LibraryTab.WATCHED -> R.string.library_tab_watched
    }

private val LibraryTab.emptyRes: Int
    get() = when (this) {
        LibraryTab.WATCHING -> R.string.library_empty_watching
        LibraryTab.PLANNED -> R.string.library_empty_planned
        LibraryTab.WATCHED -> R.string.library_empty_watched
    }

private val LibrarySort.labelRes: Int
    get() = when (this) {
        LibrarySort.RECENT -> R.string.sort_recent
        LibrarySort.TITLE -> R.string.sort_title_az
        LibrarySort.YEAR -> R.string.sort_year
        LibrarySort.RATING -> R.string.sort_rating
    }

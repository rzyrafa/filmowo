package pl.filmowo.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.MediaFilterRow
import pl.filmowo.core.ui.components.MediaTile
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.SegmentedTab
import pl.filmowo.core.ui.components.SegmentedTabs
import pl.filmowo.core.ui.components.StateTransition
import pl.filmowo.core.ui.components.TileProgress
import pl.filmowo.core.ui.components.TopLevelTopBar
import pl.filmowo.core.ui.components.progressLabel
import pl.filmowo.core.ui.theme.ToneName
import pl.filmowo.data.remote.TmdbImages
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
            SegmentedTabs(
                tabs = LibraryTab.entries.map { SegmentedTab(stringResource(it.labelRes), state.counts[it] ?: 0) },
                selectedIndex = state.tab.ordinal,
                onSelect = { viewModel.onTabSelected(LibraryTab.entries[it]) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            MediaFilterRow(selected = state.filter, onSelect = viewModel::onFilterSelected)
            StateTransition(
                targetState = when {
                    state.isLoading -> LibraryContent.LOADING
                    state.entries.isEmpty() -> LibraryContent.EMPTY
                    else -> LibraryContent.GRID
                },
            ) { content ->
                when {
                    content == LibraryContent.LOADING -> LoadingContent()
                    content == LibraryContent.EMPTY -> PlaceholderContent(
                        iconRes = state.tab.emptyIconRes,
                        text = stringResource(state.tab.emptyRes),
                        tone = state.tab.toneName,
                    )
                    else -> LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 150.dp),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(state.entries, key = { "${it.item.mediaType}-${it.item.tmdbId}" }) { entry ->
                            LibraryTile(
                                entry = entry,
                                onClick = { onOpenItem(entry.item.tmdbId, entry.item.mediaType) },
                                // Sortowanie i filtrowanie przesuwają kafelki płynnie na nowe miejsca.
                                modifier = Modifier.animateItem(),
                            )
                        }
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
private fun LibraryTile(entry: LibraryEntry, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val item = entry.item
    val progress = entry.progress
    MediaTile(
        title = item.metadata.title,
        mediaType = item.mediaType,
        year = item.metadata.year,
        posterModel = item.localPosterFile?.let(::File) ?: TmdbImages.posterSmall(item.metadata.posterPath),
        onClick = onClick,
        rating = item.rating,
        status = item.status,
        progress = progress?.let { TileProgress(it.watchedCount, it.airedCount, progressLabel(it)) },
        modifier = modifier,
    )
}

private enum class LibraryContent { LOADING, EMPTY, GRID }

private val LibraryTab.labelRes: Int
    get() = when (this) {
        LibraryTab.WATCHING -> R.string.library_tab_watching
        LibraryTab.PLANNED -> R.string.library_tab_planned
        LibraryTab.WATCHED -> R.string.library_tab_watched
    }

private val LibraryTab.toneName: ToneName
    get() = when (this) {
        LibraryTab.WATCHING -> ToneName.LILAC
        LibraryTab.PLANNED -> ToneName.BUTTER
        LibraryTab.WATCHED -> ToneName.MINT
    }

private val LibraryTab.emptyIconRes: Int
    get() = when (this) {
        LibraryTab.WATCHING -> R.drawable.ic_video_library
        LibraryTab.PLANNED -> R.drawable.ic_bookmark
        LibraryTab.WATCHED -> R.drawable.ic_check
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

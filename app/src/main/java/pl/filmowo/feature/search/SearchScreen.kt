package pl.filmowo.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import pl.filmowo.R
import pl.filmowo.core.ui.components.ErrorContent
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.Poster
import pl.filmowo.core.ui.components.TopLevelTopBar
import pl.filmowo.core.ui.components.messageRes
import pl.filmowo.data.remote.TmdbImages
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.SearchFilter
import pl.filmowo.domain.model.SearchResult

@Composable
fun SearchScreen(
    onOpenAbout: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopLevelTopBar(stringResource(R.string.nav_search), onOpenAbout) },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SearchField(query = state.query, onQueryChange = viewModel::onQueryChange)
            FilterRow(selected = state.filter, onSelect = viewModel::onFilterChange)
            SearchResults(
                results = state.results,
                onRetry = viewModel::retry,
                onLoadMore = viewModel::loadMore,
                onOpenResult = onOpenResult,
            )
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear))
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

@Composable
private fun FilterRow(selected: SearchFilter, onSelect: (SearchFilter) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SearchFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelect(filter) },
                label = {
                    Text(
                        stringResource(
                            when (filter) {
                                SearchFilter.ALL -> R.string.filter_all
                                SearchFilter.MOVIES -> R.string.filter_movies
                                SearchFilter.SHOWS -> R.string.filter_shows
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun SearchResults(
    results: SearchResultsState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
) {
    when (results) {
        SearchResultsState.Idle -> PlaceholderContent(R.drawable.ic_search, stringResource(R.string.search_placeholder))
        SearchResultsState.Loading -> LoadingContent()
        SearchResultsState.Empty -> PlaceholderContent(R.drawable.ic_search, stringResource(R.string.search_empty))
        is SearchResultsState.Error -> ErrorContent(results.error, onRetry)
        is SearchResultsState.Success -> ResultList(results, onLoadMore, onOpenResult)
    }
}

@Composable
private fun ResultList(
    results: SearchResultsState.Success,
    onLoadMore: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
) {
    val listState = rememberLazyListState()

    // Doładuj kolejną stronę, gdy użytkownik zbliża się do końca listy.
    LaunchedEffect(listState, results.items.size) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= listState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(results.items, key = { "${it.mediaType}-${it.id}" }) { item ->
            SearchResultRow(item, onClick = { onOpenResult(item) })
            HorizontalDivider()
        }
        if (results.isLoadingMore || results.loadMoreError != null) {
            item(key = "footer") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (results.isLoadingMore) {
                        CircularProgressIndicator()
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(results.loadMoreError!!.messageRes), style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = onLoadMore) { Text(stringResource(R.string.action_retry)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(item: SearchResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Poster(
            url = TmdbImages.posterSmall(item.posterPath),
            contentDescription = null,
            modifier = Modifier.width(64.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = mediaMeta(item.mediaType, item.year),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.overview.isNotBlank()) {
                Text(
                    text = item.overview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun mediaMeta(type: MediaType, year: Int?): String {
    val typeLabel = stringResource(if (type == MediaType.MOVIE) R.string.media_movie else R.string.media_show)
    return if (year != null) "$typeLabel · $year" else typeLabel
}

private const val LOAD_MORE_THRESHOLD = 5

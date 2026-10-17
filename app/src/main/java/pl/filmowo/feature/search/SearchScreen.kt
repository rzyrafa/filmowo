package pl.filmowo.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import pl.filmowo.R
import pl.filmowo.core.ui.components.ErrorContent
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.MediaFilterRow
import pl.filmowo.core.ui.components.MediaTile
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.TopLevelTopBar
import pl.filmowo.core.ui.components.messageRes
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.ToneName
import pl.filmowo.data.remote.TmdbImages
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
            Spacer(Modifier.height(4.dp))
            MediaFilterRow(selected = state.filter, onSelect = viewModel::onFilterChange)
            SearchResults(
                results = state.results,
                onRetry = viewModel::retry,
                onLoadMore = viewModel::loadMore,
                onOpenResult = onOpenResult,
            )
        }
    }
}

/** Biała wyspa z cieniem na tle ekranu; „Wyczyść” tylko przy niepustym polu. */
@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val colors = FilmowoTheme.colors
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(6.dp, FilmowoRadius.Md, ambientColor = colors.ink.copy(alpha = 0.2f), spotColor = colors.ink.copy(alpha = 0.2f)),
        shape = FilmowoRadius.Md,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(containerColor = colors.surfaceSunken, contentColor = colors.ink),
                ) {
                    Icon(painterResource(R.drawable.ic_close), stringResource(R.string.action_clear), Modifier.size(18.dp))
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceRaised,
            unfocusedContainerColor = colors.surfaceRaised,
            focusedIndicatorColor = colors.surfaceRaised,
            unfocusedIndicatorColor = colors.surfaceRaised,
            cursorColor = colors.ink,
            focusedLeadingIconColor = colors.inkMuted,
            unfocusedLeadingIconColor = colors.inkMuted,
            focusedPlaceholderColor = colors.inkMuted,
            unfocusedPlaceholderColor = colors.inkMuted,
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    )
}

@Composable
private fun SearchResults(
    results: SearchResultsState,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenResult: (SearchResult) -> Unit,
) {
    when (results) {
        SearchResultsState.Idle -> PlaceholderContent(R.drawable.ic_search, stringResource(R.string.search_placeholder), tone = ToneName.SKY)
        SearchResultsState.Loading -> LoadingContent()
        SearchResultsState.Empty -> PlaceholderContent(R.drawable.ic_search, stringResource(R.string.search_empty), tone = ToneName.NEUTRAL)
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
    val gridState = rememberLazyGridState()

    // Doładuj kolejną stronę, gdy użytkownik zbliża się do końca siatki.
    LaunchedEffect(gridState, results.items.size) {
        snapshotFlow {
            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= gridState.layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
            .distinctUntilChanged()
            .filter { it }
            .collect { onLoadMore() }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(results.items, key = { "${it.mediaType}-${it.id}" }) { item ->
            MediaTile(
                title = item.title,
                mediaType = item.mediaType,
                year = item.year,
                posterModel = TmdbImages.posterSmall(item.posterPath),
                onClick = { onOpenResult(item) },
            )
        }
        if (results.isLoadingMore || results.loadMoreError != null) {
            item(key = "footer", span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (results.isLoadingMore) {
                        CircularProgressIndicator(color = FilmowoTheme.colors.brand)
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

private const val LOAD_MORE_THRESHOLD = 5

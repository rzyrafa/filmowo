package pl.filmowo.feature.stats

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.TopLevelTopBar
import pl.filmowo.domain.model.WatchStats
import pl.filmowo.domain.model.splitDuration

@Composable
fun StatsScreen(
    onOpenAbout: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopLevelTopBar(stringResource(R.string.nav_stats), onOpenAbout) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        val stats = state.stats
        when {
            state.isLoading -> LoadingContent(modifier)
            stats == null || stats.isEmpty -> PlaceholderContent(
                iconRes = R.drawable.ic_bar_chart,
                text = stringResource(R.string.stats_empty),
                modifier = modifier,
            )
            else -> StatsContent(stats, modifier)
        }
    }
}

@Composable
private fun StatsContent(stats: WatchStats, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(R.string.stats_total_time),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    formatDuration(stats.totalMinutes),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(R.string.stats_total_hours, stats.totalMinutes / 60),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        StatCard(
            iconRes = R.drawable.ic_movie,
            title = stringResource(R.string.stats_movies),
            value = stats.moviesWatched.toString(),
            detail = stringResource(R.string.stats_time, formatDuration(stats.movieMinutes)),
        )
        StatCard(
            iconRes = R.drawable.ic_video_library,
            title = stringResource(R.string.stats_episodes),
            value = stats.episodesWatched.toString(),
            detail = stringResource(R.string.stats_time, formatDuration(stats.episodeMinutes)),
        )
    }
}

@Composable
private fun StatCard(
    @DrawableRes iconRes: Int,
    title: String,
    value: String,
    detail: String,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

/** Np. „2 dni 5 godz. 30 min”; zerowe części są pomijane, ale „0 min” zostaje dla pustego czasu. */
@Composable
private fun formatDuration(totalMinutes: Long): String {
    val parts = splitDuration(totalMinutes)
    val pieces = buildList {
        if (parts.days > 0) add(pluralStringResource(R.plurals.days_count, parts.days.toInt(), parts.days.toInt()))
        if (parts.hours > 0) add(stringResource(R.string.runtime_hours, parts.hours.toInt()))
        if (parts.minutes > 0 || isEmpty()) add(stringResource(R.string.runtime_minutes, parts.minutes.toInt()))
    }
    return pieces.joinToString(" ")
}

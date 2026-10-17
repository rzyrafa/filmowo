package pl.filmowo.feature.stats

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.ToneName
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
                tone = ToneName.LILAC,
                modifier = modifier,
            )
            else -> StatsContent(stats, modifier)
        }
    }
}

@Composable
private fun StatsContent(stats: WatchStats, modifier: Modifier = Modifier) {
    val colors = FilmowoTheme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ciemna kotwica ekranu: łączny czas oglądania.
        StatTile(
            iconRes = R.drawable.ic_bar_chart,
            value = formatDuration(stats.totalMinutes),
            label = stringResource(R.string.stats_total_time),
            detail = stringResource(R.string.stats_total_hours, stats.totalMinutes / 60),
            container = colors.inverse,
            content = colors.onInverse,
            muted = colors.onInverseMuted,
            iconContainer = colors.brand,
            iconContent = colors.onBrand,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                iconRes = R.drawable.ic_movie,
                value = stats.moviesWatched.toString(),
                label = stringResource(R.string.stats_movies),
                detail = stringResource(R.string.stats_time, formatDuration(stats.movieMinutes)),
                container = colors.peach,
                content = colors.onPeach,
                muted = colors.onPeach,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            StatTile(
                iconRes = R.drawable.ic_video_library,
                value = stats.episodesWatched.toString(),
                label = stringResource(R.string.stats_episodes),
                detail = stringResource(R.string.stats_time, formatDuration(stats.episodeMinutes)),
                container = colors.sky,
                content = colors.onSky,
                muted = colors.onSky,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
        }
    }
}

/** Kafelek liczby: ikona w kółku, duża wartość w kroju display, podpis i szczegół. */
@Composable
private fun StatTile(
    @DrawableRes iconRes: Int,
    value: String,
    label: String,
    detail: String,
    container: Color,
    content: Color,
    muted: Color,
    modifier: Modifier = Modifier,
    iconContainer: Color = FilmowoTheme.colors.surfaceRaised,
    iconContent: Color = FilmowoTheme.colors.ink,
) {
    Column(
        modifier = modifier
            .background(container, FilmowoRadius.Lg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconContainer, FilmowoRadius.Pill),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp), tint = iconContent)
        }
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = content)
        Text(label, style = MaterialTheme.typography.labelLarge, color = content)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = muted)
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

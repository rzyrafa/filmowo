package pl.filmowo.feature.upcoming

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.filmowo.R
import pl.filmowo.core.ui.components.LoadingContent
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.Poster
import pl.filmowo.core.ui.components.StateTransition
import pl.filmowo.core.ui.components.TopLevelTopBar
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.ToneName
import pl.filmowo.core.ui.theme.pressScale
import pl.filmowo.data.remote.TmdbImages
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.UpcomingEpisode
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun UpcomingScreen(
    onOpenAbout: () -> Unit,
    onOpenItem: (id: Int, type: MediaType) -> Unit,
    viewModel: UpcomingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = { TopLevelTopBar(stringResource(R.string.nav_upcoming), onOpenAbout) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        StateTransition(state, contentKey = { it.isLoading to it.days.isEmpty() }) { current ->
            when {
                current.isLoading -> LoadingContent(modifier)
                current.days.isEmpty() -> PlaceholderContent(
                    iconRes = R.drawable.ic_calendar,
                    text = stringResource(R.string.upcoming_empty),
                    tone = ToneName.SKY,
                    modifier = modifier,
                )
                else -> LazyColumn(
                    modifier = modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    current.days.forEach { day ->
                        item(key = "day-${day.date}") {
                            Text(
                                text = dayLabel(day.date, current.today),
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier
                                    .padding(top = 12.dp, bottom = 4.dp)
                                    .semantics { heading() },
                            )
                        }
                        items(day.episodes, key = { "${it.showId}-${it.seasonNumber}-${it.episodeNumber}" }) { episode ->
                            UpcomingRow(episode, onClick = { onOpenItem(episode.showId, MediaType.TV) })
                        }
                    }
                    item(key = "hint") {
                        Text(
                            stringResource(R.string.upcoming_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = FilmowoTheme.colors.inkMuted,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingRow(episode: UpcomingEpisode, onClick: () -> Unit) {
    val colors = FilmowoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.98f)
            .clip(FilmowoRadius.Lg)
            .background(colors.surfaceRaised)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Poster(
            model = episode.localPosterFile?.let(::File) ?: TmdbImages.posterSmall(episode.posterPath),
            contentDescription = null,
            mediaType = MediaType.TV,
            shape = FilmowoRadius.Sm,
            modifier = Modifier.width(48.dp),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                episode.showTitle,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = episode.name?.let {
                    stringResource(R.string.upcoming_episode, episode.seasonNumber, episode.episodeNumber, it)
                } ?: stringResource(R.string.upcoming_episode_no_name, episode.seasonNumber, episode.episodeNumber),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.inkMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (episode.isWatched) {
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = stringResource(R.string.status_watched),
                tint = colors.inkMuted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** „Dziś”, „Jutro” albo np. „sobota, 24 października”. */
@Composable
private fun dayLabel(date: String, today: String): String {
    val parsed = try {
        LocalDate.parse(date)
    } catch (e: DateTimeParseException) {
        return date
    }
    val todayDate = runCatching { LocalDate.parse(today) }.getOrNull()
    return when (parsed) {
        todayDate -> stringResource(R.string.upcoming_today)
        todayDate?.plusDays(1) -> stringResource(R.string.upcoming_tomorrow)
        else -> parsed.format(dayFormatter(parsed.year == todayDate?.year))
    }
}

private val polish = Locale.forLanguageTag("pl-PL")

private fun dayFormatter(sameYear: Boolean): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (sameYear) "EEEE, d MMMM" else "EEEE, d MMMM yyyy", polish)

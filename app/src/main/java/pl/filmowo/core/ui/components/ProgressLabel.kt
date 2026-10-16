package pl.filmowo.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import pl.filmowo.R
import pl.filmowo.domain.model.ShowProgress

/** Np. „S2E5 · 60%”, a przed obejrzeniem pierwszego odcinka „Nie rozpoczęto · 0%”. */
@Composable
fun progressLabel(progress: ShowProgress): String {
    val last = progress.lastWatched
    return if (last != null) {
        stringResource(R.string.progress_label, last.seasonNumber, last.episodeNumber, progress.percent)
    } else {
        stringResource(R.string.progress_not_started, progress.percent)
    }
}

package pl.filmowo.feature.library

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pl.filmowo.R
import pl.filmowo.core.ui.components.PlaceholderContent
import pl.filmowo.core.ui.components.TopLevelTopBar

@Composable
fun LibraryScreen(onOpenAbout: () -> Unit) {
    Scaffold(
        topBar = { TopLevelTopBar(stringResource(R.string.nav_library), onOpenAbout) },
    ) { padding ->
        PlaceholderContent(
            iconRes = R.drawable.ic_video_library,
            text = stringResource(R.string.library_placeholder),
            modifier = Modifier.padding(padding),
        )
    }
}

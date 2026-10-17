package pl.filmowo.core.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import pl.filmowo.R
import pl.filmowo.core.ui.theme.FilmowoTheme

/** Górny pasek ekranów głównych: duży tytuł w kroju display i przycisk „O aplikacji”. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevelTopBar(
    title: String,
    onOpenAbout: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.headlineMedium) },
        actions = {
            actions()
            IconButton(onClick = onOpenAbout) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.about_title),
                )
            }
        },
        colors = filmowoTopBarColors(),
    )
}

/** Pasek w kolorze tła ekranu – bez zmiany odcienia przy przewijaniu. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun filmowoTopBarColors(): TopAppBarColors {
    val colors = FilmowoTheme.colors
    return TopAppBarDefaults.topAppBarColors(
        containerColor = colors.surface,
        scrolledContainerColor = colors.surface,
        titleContentColor = colors.ink,
        navigationIconContentColor = colors.ink,
        actionIconContentColor = colors.ink,
    )
}

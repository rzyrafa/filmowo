package pl.filmowo.core.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import pl.filmowo.R

/** Górny pasek ekranów głównych z przyciskiem „O aplikacji”. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopLevelTopBar(
    title: String,
    onOpenAbout: () -> Unit,
) {
    TopAppBar(
        title = { Text(title) },
        actions = {
            IconButton(onClick = onOpenAbout) {
                Icon(
                    painter = painterResource(R.drawable.ic_info),
                    contentDescription = stringResource(R.string.about_title),
                )
            }
        },
    )
}

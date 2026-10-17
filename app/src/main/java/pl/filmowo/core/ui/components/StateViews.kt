package pl.filmowo.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import pl.filmowo.R
import pl.filmowo.core.network.AppError
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.ToneName

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FilmowoTheme.colors.brand, trackColor = FilmowoTheme.colors.surfaceSunken)
    }
}

@Composable
fun ErrorContent(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PlaceholderContent(
        iconRes = if (error == AppError.NETWORK) R.drawable.ic_cloud_off else R.drawable.ic_error,
        text = stringResource(error.messageRes),
        tone = ToneName.NEUTRAL,
        modifier = modifier,
        action = if (error.isRetryable) {
            { Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) } }
        } else {
            null
        },
    )
}

@get:StringRes
val AppError.messageRes: Int
    get() = when (this) {
        AppError.NETWORK -> R.string.error_network
        AppError.UNAUTHORIZED -> R.string.error_unauthorized
        AppError.MISSING_API_KEY -> R.string.error_missing_api_key
        AppError.NOT_FOUND -> R.string.error_not_found
        AppError.SERVER -> R.string.error_server
        AppError.UNKNOWN -> R.string.error_unknown
    }

/** Błędy konfiguracji nie znikną po ponowieniu – nie pokazujemy wtedy przycisku. */
val AppError.isRetryable: Boolean
    get() = this != AppError.MISSING_API_KEY && this != AppError.UNAUTHORIZED

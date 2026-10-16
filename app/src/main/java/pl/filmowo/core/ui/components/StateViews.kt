package pl.filmowo.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.filmowo.R
import pl.filmowo.core.network.AppError

@Composable
fun LoadingContent(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun ErrorContent(
    error: AppError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painter = painterResource(if (error == AppError.NETWORK) R.drawable.ic_cloud_off else R.drawable.ic_error),
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(error.messageRes),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (error.isRetryable) {
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }
    }
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

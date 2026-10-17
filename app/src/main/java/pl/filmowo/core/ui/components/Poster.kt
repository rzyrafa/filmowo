package pl.filmowo.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import pl.filmowo.R
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.ToneName
import pl.filmowo.core.ui.theme.tone
import pl.filmowo.domain.model.MediaType
import pl.filmowo.domain.model.WatchStatus

/**
 * Plakat w proporcjach 2:3. Bez obrazka (lub przy błędzie) pokazuje pastelowe tło –
 * peach dla filmu, sky dla serialu – z ikoną. Ocena (prawy górny róg) i status
 * (lewy dolny) leżą na obrazie jako pływające znaczniki.
 * [model] to adres URL albo zapisany lokalnie [java.io.File].
 */
@Composable
fun Poster(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    mediaType: MediaType = MediaType.MOVIE,
    rating: Int? = null,
    status: WatchStatus? = null,
    shape: Shape = FilmowoRadius.Md,
) {
    val tone = FilmowoTheme.colors.tone(if (mediaType == MediaType.TV) ToneName.SKY else ToneName.PEACH)
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(shape)
            .background(tone.container),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_movie),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            tint = tone.content,
        )
        if (model != null) {
            AsyncImage(
                model = model,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (rating != null) {
            Badge(
                text = rating.toString(),
                tone = ToneName.BUTTER,
                iconRes = R.drawable.ic_star,
                onImage = true,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
        }
        if (status != null) {
            StatusBadge(
                status = status,
                onImage = true,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 8.dp, bottom = 10.dp),
            )
        }
    }
}

package pl.filmowo.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import pl.filmowo.R
import kotlin.reflect.KClass

/** Zakładki dolnego paska nawigacji. */
enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
) {
    SEARCH(SearchRoute, SearchRoute::class, R.drawable.ic_search, R.string.nav_search),
    LIBRARY(LibraryRoute, LibraryRoute::class, R.drawable.ic_video_library, R.string.nav_library),
    UPCOMING(UpcomingRoute, UpcomingRoute::class, R.drawable.ic_calendar, R.string.nav_upcoming),
    STATS(StatsRoute, StatsRoute::class, R.drawable.ic_bar_chart, R.string.nav_stats),
}

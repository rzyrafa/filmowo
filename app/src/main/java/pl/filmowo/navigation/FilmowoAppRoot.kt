package pl.filmowo.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.domain.model.MediaType
import pl.filmowo.feature.about.AboutScreen
import pl.filmowo.feature.details.MovieDetailsScreen
import pl.filmowo.feature.details.ShowDetailsScreen
import pl.filmowo.feature.library.LibraryScreen
import pl.filmowo.feature.search.SearchScreen
import pl.filmowo.feature.season.SeasonScreen
import pl.filmowo.feature.stats.StatsScreen

@Composable
fun FilmowoAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = TopLevelDestination.entries.any { currentDestination.isOn(it) }

    Scaffold(
        // Wcięcia systemowe obsługują paski ekranów; tu tylko dolna nawigacja.
        contentWindowInsets = WindowInsets(0),
        containerColor = FilmowoTheme.colors.surface,
        bottomBar = {
            if (showBottomBar) {
                FilmowoBottomBar(
                    currentDestination = currentDestination,
                    onSelect = navController::navigateToTopLevel,
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = SearchRoute,
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
        ) {
            val openAbout = { navController.navigate(AboutRoute) }
            composable<SearchRoute> {
                SearchScreen(
                    onOpenAbout = openAbout,
                    onOpenResult = { navController.navigateToDetails(it.id, it.mediaType) },
                )
            }
            composable<LibraryRoute> {
                LibraryScreen(
                    onOpenAbout = openAbout,
                    onOpenItem = { id, type -> navController.navigateToDetails(id, type) },
                )
            }
            composable<StatsRoute> { StatsScreen(onOpenAbout = openAbout) }
            composable<AboutRoute> { AboutScreen(onBack = navController::navigateUp) }
            composable<MovieDetailsRoute> { MovieDetailsScreen(onBack = navController::navigateUp) }
            composable<ShowDetailsRoute> {
                ShowDetailsScreen(
                    onBack = navController::navigateUp,
                    onOpenSeason = { showId, season, title -> navController.navigate(SeasonRoute(showId, season, title)) },
                )
            }
            composable<SeasonRoute> { entry ->
                SeasonScreen(title = entry.toRoute<SeasonRoute>().title, onBack = navController::navigateUp)
            }
        }
    }
}

/** Pływająca ciemna wyspa (inverse) z koralową pigułką pod aktywną ikoną. */
@Composable
private fun FilmowoBottomBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val colors = FilmowoTheme.colors
    Row(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(12.dp, FilmowoRadius.Xl)
            .background(colors.inverse, FilmowoRadius.Xl)
            .padding(8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TopLevelDestination.entries.forEach { destination ->
            val selected = currentDestination.isOn(destination)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(FilmowoRadius.Lg)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(destination) })
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 56.dp, height = 32.dp)
                        .background(if (selected) colors.brand else colors.inverse, FilmowoRadius.Pill),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painterResource(destination.iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp),
                        tint = if (selected) colors.onBrand else colors.onInverseMuted,
                    )
                }
                Text(
                    stringResource(destination.labelRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selected) colors.onInverse else colors.onInverseMuted,
                )
            }
        }
    }
}

private fun NavDestination?.isOn(destination: TopLevelDestination): Boolean =
    this?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.navigateToDetails(id: Int, type: MediaType) {
    when (type) {
        MediaType.MOVIE -> navigate(MovieDetailsRoute(id))
        MediaType.TV -> navigate(ShowDetailsRoute(id))
    }
}

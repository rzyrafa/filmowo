package pl.filmowo.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
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
import pl.filmowo.core.ui.theme.FilmowoMotion
import pl.filmowo.core.ui.theme.FilmowoRadius
import pl.filmowo.core.ui.theme.FilmowoTheme
import pl.filmowo.core.ui.theme.pressScale
import pl.filmowo.domain.model.MediaType
import pl.filmowo.feature.about.AboutScreen
import pl.filmowo.feature.details.MovieDetailsScreen
import pl.filmowo.feature.details.ShowDetailsScreen
import pl.filmowo.feature.library.LibraryScreen
import pl.filmowo.feature.search.SearchScreen
import pl.filmowo.feature.season.SeasonScreen
import pl.filmowo.feature.stats.StatsScreen
import pl.filmowo.feature.upcoming.UpcomingScreen

@Composable
fun FilmowoAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination == null || currentDestination.isTopLevel()

    Scaffold(
        // Wcięcia systemowe obsługują paski ekranów; tu tylko dolna nawigacja.
        contentWindowInsets = WindowInsets(0),
        containerColor = FilmowoTheme.colors.surface,
        bottomBar = {
            // Pasek zjeżdża w dół przy wejściu w szczegóły i wraca po powrocie.
            AnimatedVisibility(
                visible = showBottomBar,
                enter = slideInVertically(FilmowoMotion.move()) { it } + fadeIn(FilmowoMotion.fade()),
                exit = slideOutVertically(FilmowoMotion.move()) { it } + fadeOut(FilmowoMotion.fade(FilmowoMotion.SHORT)),
            ) {
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
            enterTransition = { screenEnter(forward = true) },
            exitTransition = { screenExit(forward = true) },
            popEnterTransition = { screenEnter(forward = false) },
            popExitTransition = { screenExit(forward = false) },
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
            composable<UpcomingRoute> {
                UpcomingScreen(
                    onOpenAbout = openAbout,
                    onOpenItem = { id, type -> navController.navigateToDetails(id, type) },
                )
            }
            composable<StatsRoute> { StatsScreen(onOpenAbout = openAbout) }
            composable<AboutRoute> { AboutScreen(onBack = navController::navigateUp) }
            val openDetails = { id: Int, type: MediaType -> navController.navigateToDetails(id, type) }
            composable<MovieDetailsRoute> {
                MovieDetailsScreen(onBack = navController::navigateUp, onOpenDetails = openDetails)
            }
            composable<ShowDetailsRoute> {
                ShowDetailsScreen(
                    onBack = navController::navigateUp,
                    onOpenDetails = openDetails,
                    onOpenSeason = { showId, season, title -> navController.navigate(SeasonRoute(showId, season, title)) },
                )
            }
            composable<SeasonRoute> { entry ->
                SeasonScreen(title = entry.toRoute<SeasonRoute>().title, onBack = navController::navigateUp)
            }
        }
    }
}

// --- Przejścia ekranów ---------------------------------------------------------------------------
// Między zakładkami: szybkie przenikanie z lekkim powiększeniem (fade through).
// W głąb (szczegóły, sezon): nowy ekran wsuwa się z prawej, poprzedni lekko odjeżdża w lewo.

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabSwitch(): Boolean =
    initialState.destination.isTopLevel() && targetState.destination.isTopLevel()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.screenEnter(forward: Boolean): EnterTransition =
    if (isTabSwitch()) {
        fadeIn(FilmowoMotion.fade(FilmowoMotion.MEDIUM, delay = 60)) +
            scaleIn(FilmowoMotion.move(), initialScale = 0.97f)
    } else {
        slideIntoContainer(
            towards = if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = FilmowoMotion.move(),
            initialOffset = { if (forward) it / 3 else it / 6 },
        ) + fadeIn(FilmowoMotion.fade())
    }

private fun AnimatedContentTransitionScope<NavBackStackEntry>.screenExit(forward: Boolean): ExitTransition =
    if (isTabSwitch()) {
        fadeOut(FilmowoMotion.fade(FilmowoMotion.SHORT))
    } else {
        slideOutOfContainer(
            towards = if (forward) AnimatedContentTransitionScope.SlideDirection.Start else AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = FilmowoMotion.move(),
            targetOffset = { if (forward) it / 6 else it / 3 },
        ) + fadeOut(FilmowoMotion.fade(FilmowoMotion.SHORT))
    }

// --- Dolna nawigacja -----------------------------------------------------------------------------

private val NavItemSpacing = 8.dp
private val NavPillWidth = 56.dp

/**
 * Pływająca ciemna wyspa (inverse). Koralowa pigułka przesuwa się pod wybraną
 * ikonę, ikona lekko podskakuje, a kolory etykiet płynnie się zmieniają.
 */
@Composable
private fun FilmowoBottomBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    val colors = FilmowoTheme.colors
    val destinations = TopLevelDestination.entries
    val selectedIndex = destinations.indexOfFirst { currentDestination.isOn(it) }.coerceAtLeast(0)

    BoxWithConstraints(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .shadow(12.dp, FilmowoRadius.Xl)
            .background(colors.inverse, FilmowoRadius.Xl)
            .padding(8.dp),
    ) {
        val itemWidth = (maxWidth - NavItemSpacing * (destinations.size - 1)) / destinations.size
        val pillOffset by animateDpAsState(
            targetValue = (itemWidth + NavItemSpacing) * selectedIndex + (itemWidth - NavPillWidth) / 2,
            animationSpec = FilmowoMotion.move(),
            label = "navPill",
        )
        // Wskaźnik pod spodem – jeden element, który jedzie między pozycjami.
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                // Przesunięcie w fazie układu – animacja nie wymusza ponownej kompozycji co klatkę.
                .offset { IntOffset(pillOffset.roundToPx(), 0) }
                .size(width = NavPillWidth, height = 32.dp)
                .background(colors.brand, FilmowoRadius.Pill),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(NavItemSpacing),
        ) {
            destinations.forEachIndexed { index, destination ->
                NavItem(
                    destination = destination,
                    selected = index == selectedIndex,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.width(itemWidth),
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FilmowoTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val iconTint by animateColorAsState(if (selected) colors.onBrand else colors.onInverseMuted, FilmowoMotion.color(), label = "navIcon")
    val labelColor by animateColorAsState(if (selected) colors.onInverse else colors.onInverseMuted, FilmowoMotion.color(), label = "navLabel")
    val iconScale by animateFloatAsState(if (selected) 1.1f else 1f, FilmowoMotion.pop(), label = "navIconScale")

    Column(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.92f)
            .clip(FilmowoRadius.Lg)
            .selectable(
                selected = selected,
                role = Role.Tab,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.size(width = NavPillWidth, height = 32.dp), contentAlignment = Alignment.Center) {
            Icon(
                painterResource(destination.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    },
                tint = iconTint,
            )
        }
        Text(
            stringResource(destination.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
    }
}

private fun NavDestination.isTopLevel(): Boolean =
    TopLevelDestination.entries.any { destination -> hierarchy.any { it.hasRoute(destination.routeClass) } }

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

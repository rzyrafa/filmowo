package pl.filmowo.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import pl.filmowo.feature.about.AboutScreen
import pl.filmowo.feature.library.LibraryScreen
import pl.filmowo.feature.search.SearchScreen
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
            composable<SearchRoute> { SearchScreen(onOpenAbout = openAbout) }
            composable<LibraryRoute> { LibraryScreen(onOpenAbout = openAbout) }
            composable<StatsRoute> { StatsScreen(onOpenAbout = openAbout) }
            composable<AboutRoute> { AboutScreen(onBack = navController::navigateUp) }
        }
    }
}

@Composable
private fun FilmowoBottomBar(
    currentDestination: NavDestination?,
    onSelect: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = currentDestination.isOn(destination),
                onClick = { onSelect(destination) },
                icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                label = { Text(label) },
            )
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

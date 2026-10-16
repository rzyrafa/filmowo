package pl.filmowo.navigation

import kotlinx.serialization.Serializable

@Serializable
data object SearchRoute

@Serializable
data object LibraryRoute

@Serializable
data object StatsRoute

@Serializable
data object AboutRoute

/** Argument `id` czytają ViewModele z SavedStateHandle pod tą samą nazwą. */
@Serializable
data class MovieDetailsRoute(val id: Int)

@Serializable
data class ShowDetailsRoute(val id: Int)

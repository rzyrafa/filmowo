package pl.filmowo.data.local

/** Wynik zapytań agregujących: liczba pozycji i suma ich czasu trwania. */
data class WatchTotals(
    val count: Int,
    val minutes: Long,
)

package pl.filmowo.core

/** Źródło bieżącego czasu (ms) – podmieniane w testach. */
fun interface TimeProvider {
    fun now(): Long
}

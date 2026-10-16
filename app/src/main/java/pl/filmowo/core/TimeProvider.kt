package pl.filmowo.core

import java.time.Instant
import java.time.ZoneId

/** Źródło bieżącego czasu (ms) – podmieniane w testach. */
fun interface TimeProvider {
    fun now(): Long
}

/** Dzisiejsza data w strefie urządzenia, w formacie ISO (yyyy-MM-dd). */
fun TimeProvider.today(zone: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(now()).atZone(zone).toLocalDate().toString()

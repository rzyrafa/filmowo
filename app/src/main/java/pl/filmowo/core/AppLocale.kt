package pl.filmowo.core

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** Aplikacja jest tylko po polsku. */
val AppLocale: Locale = Locale.forLanguageTag("pl-PL")

/**
 * Kontekst z polskim locale w konfiguracji zasobów. Bez tego na telefonie z innym językiem
 * Android wybiera formy liczby mnogiej według reguł tamtego języka (np. „5 sezonu” zamiast „5 sezonów”).
 */
fun Context.withAppLocale(): Context {
    val config = Configuration(resources.configuration).apply { setLocale(AppLocale) }
    return createConfigurationContext(config)
}

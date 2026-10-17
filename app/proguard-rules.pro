# Reguły R8 dla wersji release.

# Klasy @Serializable z aplikacji (trasy nawigacji, DTO z TMDB, format kopii zapasowej) zostają w całości:
# nazwy pól są częścią formatu JSON i tras, a biblioteki dostarczają reguły tylko dla samych serializerów.
-keep @kotlinx.serialization.Serializable class pl.filmowo.** { *; }
-keepclassmembers class pl.filmowo.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

# Enumy zapisywane w bazie i w kopii zapasowej po nazwie.
-keepclassmembers enum pl.filmowo.domain.model.** { *; }

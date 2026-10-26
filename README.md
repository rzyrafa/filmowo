<div align="center">

# 🎬 Filmowo

**Twój prywatny dziennik filmów i seriali na Androida.**

Szukaj tytułów, odhaczaj obejrzane odcinki, śledź premiery i sprawdzaj, ile czasu spędziłeś przed ekranem — bez kont, reklam i analityki.

![Android CI](https://github.com/rzyrafa/filmowo/actions/workflows/android.yml/badge.svg)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)
![Min SDK](https://img.shields.io/badge/minSdk-26-3DDC84?logo=android&logoColor=white)
![Wersja](https://img.shields.io/badge/wersja-0.2.0-orange)

</div>

---

## ✨ Funkcje

| | |
|---|---|
| 🔍 **Wyszukiwarka** | Filmy i seriale z bazy TMDB, filtr Film/Serial oraz lista „Popularne w tym tygodniu”. |
| 📚 **Biblioteka** | Zakładki *Oglądam*, *Do obejrzenia* i *Obejrzane*, sortowanie, wyszukiwanie w bibliotece. |
| ✅ **Śledzenie odcinków** | Odhaczanie pojedynczych odcinków lub całych sezonów, pasek postępu i szybkie „Odhacz S1E5” prosto z listy. |
| ⭐ **Oceny, notatki, powtórki** | Własna ocena 1–10, notatka do każdego tytułu i licznik ponownych seansów. |
| 📅 **Premiery** | Kalendarz nadchodzących odcinków seriali z biblioteki i codzienne powiadomienie o dzisiejszych premierach. |
| 📊 **Statystyki** | Łączny czas oglądania, liczba filmów i odcinków, ukończone seriale, średnia ocena, ulubione gatunki i aktywność z ostatnich 6 miesięcy. |
| 💾 **Kopia zapasowa** | Eksport i import biblioteki, ocen, notatek i postępu do pliku JSON. |
| 🔒 **Prywatność** | Wszystkie dane zostają na urządzeniu. Działa offline — plakaty są zapisywane lokalnie. |

## 🛠️ Technologie

- **Kotlin** + **Jetpack Compose** (Material 3) z własnym design systemem, animacjami i trybem ciemnym
- **Architektura**: MVVM, warstwy `data` / `domain` / `feature`, `StateFlow` + korutyny
- **Hilt** — wstrzykiwanie zależności
- **Room** — lokalna baza danych (z eksportowanymi schematami i migracjami)
- **Retrofit** + **OkHttp** + **kotlinx.serialization** — komunikacja z TMDB API
- **Coil 3** — ładowanie obrazów
- **WorkManager** — synchronizacja odcinków co 12 godzin i powiadomienia o premierach
- **DataStore** — ustawienia użytkownika
- **Navigation Compose** z bezpiecznymi typami tras
- **Testy**: JUnit, MockK, Turbine, Robolectric, MockWebServer
- **Jakość**: ktlint, Android Lint, GitHub Actions CI

## 🚀 Uruchomienie

### Wymagania

- Android Studio (najnowsza stabilna wersja) lub JDK 17+
- Darmowy klucz API z [TMDB](https://www.themoviedb.org/settings/api)

### Konfiguracja

1. Sklonuj repozytorium:

   ```bash
   git clone https://github.com/rzyrafa/filmowo.git
   ```

2. W katalogu głównym projektu utwórz (lub uzupełnij) plik `local.properties` i dodaj swój klucz TMDB (API v3):

   ```properties
   TMDB_API_KEY=twoj_klucz_api
   ```

   > Plik `local.properties` jest w `.gitignore`, więc klucz nie trafi do repozytorium.

3. Zbuduj i zainstaluj aplikację:

   ```bash
   ./gradlew installDebug
   ```

### Przydatne polecenia

```bash
./gradlew testDebugUnitTest   # testy jednostkowe
./gradlew ktlintCheck         # sprawdzenie stylu kodu
./gradlew lintDebug           # Android Lint
./gradlew assembleRelease     # wersja release (zminifikowana)
```

## 📁 Struktura projektu

```
app/src/main/java/pl/filmowo/
├── core/          # DI, sieć, motyw i wspólne komponenty UI
├── data/          # Room, TMDB API, repozytoria, kopia zapasowa, ustawienia
├── domain/        # modele domenowe (biblioteka, odcinki, statystyki)
├── feature/       # ekrany: search, library, details, season, upcoming, stats, about
├── navigation/    # trasy i główny szkielet nawigacji
└── work/          # WorkManager: synchronizacja i powiadomienia o premierach
```

## 🙏 Źródło danych

<img src="https://www.themoviedb.org/assets/2/v4/logos/v2/blue_short-8e7b30f73a4020692ccca9c88bafe5dcb6f8a62a4c6bc55cd9ba82bb2cd95f6c.svg" alt="TMDB" height="16">

Ta aplikacja korzysta z API TMDB, ale nie jest wspierana ani certyfikowana przez TMDB.

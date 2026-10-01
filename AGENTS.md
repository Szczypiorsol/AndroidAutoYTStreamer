# AGENTS.md

Ten plik definiuje sposób pracy dla agentów AI w tym repozytorium.

## Projekt

AndroidAutoYTStreamer to aplikacja Android do odtwarzania długich playlist YouTube z poziomu Android Auto z minimalną liczbą interakcji w czasie jazdy.

Główne technologie:
- Kotlin
- Jetpack Compose
- Media3 / ExoPlayer
- Google Sign-In
- YouTube Data API
- lokalna kolejka odtwarzania i historia oglądania

## Zasady pracy agentów

1. Zachowuj prostotę i czytelność kodu.
2. Preferuj małe, dobrze nazwane komponenty.
3. Nie dodawaj nieużywanych zależności.
4. Nie zmieniaj publicznych kontraktów bez potrzeby.
5. Dla Androida dbaj o bezpieczeństwo, lifecycle i kompatybilność z API 30+.
6. Gdy w projekcie są dane konfiguracyjne, używaj `local.properties` i nie commituj prawdziwych kluczy.

## Szybkie komendy

```bash
./gradlew test
./gradlew assembleDebug
./gradlew assembleRelease
```

## Architektura repo

- `app/src/main/java/...` – kod aplikacji
- `PlaylistQueueManager.kt` – kolejka odtwarzania
- `PlaylistHistoryStore.kt` – historia oglądania
- `GoogleAuthManager.kt` – logowanie Google
- `YouTubePlaylistRepository.kt` i `YouTubePrivatePlaylistRepository.kt` – integracja z YouTube
- `PlaybackService.kt` / `MediaSessionController.kt` – obsługa odtwarzania i Android Auto
- `MainActivity.kt` – ekran główny aplikacji

## Rekomendowane role agentów

### 1. android-developer
Odpowiada za:
- implementację feature'ów w Kotlin/Compose,
- poprawność lifecycle i ViewModel,
- integrację z Media3 i Android Auto.

### 2. youtube-integration
Odpowiada za:
- parsowanie linków do playlist,
- obsługę API YouTube,
- logowanie Google i prywatne playlisty.

### 3. qa-agent
Odpowiada za:
- testy jednostkowe i integracyjne,
- sprawdzanie edge cases dla kolejki i historii,
- walidację błędów autoryzacji i fallbacków.

### 4. docs-agent
Odpowiada za:
- utrzymywanie README i dokumentacji,
- aktualizację planu rozwoju,
- opis zmian i statusu projektu.

## Dodatkowe wytyczne

- Przy pracy z API/kluczami stosuj fallbacki, szczególnie dla `YOUTUBE_API_KEY`.
- W przypadku problemów z `local.properties`, nie wpisuj sekretów do repo.
- Jeśli feature dotyczy Android Auto, zwracaj uwagę na bezpieczeństwo podczas jazdy i prostotę interfejsu.
- Nie dodawaj dużych, niepotrzebnych abstrakcji do prostego MVP.

## Priorytet pracy

1. stabilność aplikacji,
2. poprawność kolejki i historii,
3. integracja z YouTube,
4. UX pod Android Auto,
5. dokumentacja i utrzymanie projektu.


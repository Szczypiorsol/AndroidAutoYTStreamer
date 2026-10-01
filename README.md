# AndroidAutoYTStreamer

Aplikacja na Androida do bezpiecznego sterowania długą playlistą z YouTube z poziomu Android Auto, bez sięgania po telefon w czasie jazdy.

## Repozytorium GitHub

To repo jest przygotowywane jako punkt startowy projektu. Najważniejsze informacje organizacyjne i plan publikacji znajdują się w `docs/GITHUB.md`.

## Cel projektu

Najważniejszy cel:
- sterowanie z ekranu samochodu przez Android Auto,
- minimalna liczba kliknięć,
- odtwarzanie playlisty po kolei,
- zapamiętywanie postępu odtwarzania,
- pomijanie już obejrzanych materiałów,
- wznowienie ostatniego niedokończonego filmu.

## Założenia funkcjonalne

- użytkownik podaje link do playlisty YouTube,
- aplikacja rozpoznaje ID lub link playlisty,
- lista jest odtwarzana po kolei,
- obejrzane filmy są automatycznie pomijane lub oznaczane jako zakończone,
- ostatni niedokończony film może zostać wznowiony,
- sterowanie odbywa się głównie z Android Auto.

## Priorytety

1. Bezpieczeństwo podczas jazdy.
2. Minimalny i czytelny interfejs.
3. Kolejkowanie długich playlist bez powtórek.
4. Zapamiętywanie postępu i wznowienie od miejsca przerwania.

## Konfiguracja YouTube API

Aplikacja ma wbudowany mechanizm fallbacku, ale do pracy z prawdziwą playlistą YouTube najlepiej dodać klucz API w pliku `local.properties`:

```properties
YOUTUBE_API_KEY=twoj_klucz_api
```

Jeżeli klucz nie zostanie ustawiony, aplikacja użyje bezpiecznego trybu fallbackowego z generowaną listą demo, dzięki czemu projekt nadal może być uruchamiany i testowany.

## Dokumentacja

- plan rozwoju: `docs/PLAN.md`
- informacje o GitHub: `docs/GITHUB.md`

## Status repo

- dokumentacja startowa została zapisana,
- działa lokalna kolejka z historią i wznowieniem,
- działa wybór prywatnych playlist po Google Sign-In,
- działa `MediaSession` i `MediaLibraryService` pod Android Auto,
- browse tree Android Auto jest spięte z realną kolejką i odświeżane po zmianach,
- dodano cold-start fallback kolejki z TTL (24h),
- dodano konfigurację `YOUTUBE_API_KEY` z fallbackiem demo dla środowiska bez klucza.

## Stan obecny

Projekt jest na etapie rozbudowanego MVP: ma działającą lokalną kolejkę, trwałą historię postępu, sterowanie play/pause/next/previous, obsługę prywatnych playlist oraz endpoint Android Auto (`MediaLibraryService`) z live browse tree i fallbackiem po cold starcie.

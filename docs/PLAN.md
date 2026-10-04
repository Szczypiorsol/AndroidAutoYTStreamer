# Plan aplikacji AndroidAutoYTStreamer

## Cel

Stworzyć aplikację Android do bezpiecznego odtwarzania długich playlist z YouTube w dwóch trybach: telefon i Android Auto, z minimalną liczbą interakcji podczas jazdy.

## Najważniejsze wymagania

- sterowanie z ekranu samochodu w Android Auto,
- pełna obsługa na telefonie do konfiguracji, testów i codziennego użycia,
- odtwarzanie playlisty YouTube po kolei,
- zapamiętywanie postępu odtwarzania,
- pomijanie już obejrzanych materiałów,
- zachowanie ostatniego niedokończonego filmu do wznowienia,
- minimalny interfejs i mała liczba kliknięć.

## Założenia działania

### Playlisty
- [x] użytkownik podaje link do playlisty YouTube,
- [x] aplikacja pobiera listę filmów,
- [x] aplikacja buduje z nich lokalną kolejkę,
- [x] kolejka jest odtwarzana po kolei.

### Konta Google i prywatne playlisty
- [x] jeśli playlista ma być prywatna, konieczne jest zalogowanie użytkownika do Google / YouTube w aplikacji,
- [x] aplikacja pobiera listę playlist użytkownika przez YouTube Data API,
- [x] po zalogowaniu użytkownik może wybrać swoje playlisty w aplikacji,
- [x] dane z prywatnych playlist są przechowywane lokalnie i wykorzystywane w kolejce odtwarzania.

### Historia i postęp
- [x] każdy film może mieć status: `nieobejrzany`, `w trakcie`, `obejrzany`,
- [x] filmy obejrzane do końca są usuwane z lokalnej kolejki,
- [x] film przerwany przed końcem pozostaje jako ostatni niedokończony,
- [x] przy starcie aplikacja powinna wznowić od pierwszego nieobejrzanego lub od niedokończonego filmu.

### Bezpieczeństwo
- [x] brak potrzeby używania telefonu podczas jazdy,
- [x] główne sterowanie z Android Auto,
- [x] duże i proste elementy UI,
- [x] preferowane automatyczne wznawianie po podłączeniu do auta.

## Proponowany zakres MVP

### Etap 0 - logowanie Google i prywatne playlisty
- [x] Google Sign-In w aplikacji,
- [x] autoryzacja do YouTube Data API,
- [x] pobieranie listy prywatnych playlist użytkownika,
- [x] wybór playlisty z poziomu aplikacji,
- [x] podstawowa obsługa błędów autoryzacji i ładowania.

### Etap 1 - fundament
- [x] ekran startowy w aplikacji,
- [x] zapis linku do playlisty,
- [x] lokalna lista filmów,
- [x] podstawowy stan odtwarzania.

### Etap 2 - Android Auto
- [x] integracja z ekranem samochodu,
- [x] sterowanie play/pause/next/previous,
- [x] widok odtwarzania zoptymalizowany pod samochód,
- [x] endpoint `MediaLibraryService` dla browse tree Android Auto,
- [x] live queue w browse tree (podpięta pod realną kolejkę),
- [x] odświeżanie browse tree po zmianach kolejki z debounce,
- [x] fallback cold-start z lokalnego cache kolejki (TTL 24h).

### Etap 3 - kolejka i historia
- [x] odtwarzanie po kolei,
- [x] usuwanie obejrzanych filmów z kolejki,
- [x] wznowienie niedokończonego filmu,
- [x] pomijanie filmów już zaliczonych.

### Etap 4 - wygoda
- [ ] ulubione playlisty,
- [x] automatyczne wznowienie po połączeniu z autem,
- [x] zapamiętywanie ostatniego punktu odtwarzania,
- [ ] dopracowanie UX pod jazdę.

## Ograniczenia i ryzyka

- [x] pełna automatyzacja sterowania samą aplikacją YouTube może być ograniczona,
- [x] najlepiej projektować własny, prosty model odtwarzania i historii,
- [x] wszystkie funkcje muszą być zgodne z zasadami bezpieczeństwa i ograniczeniami Android Auto,
- [x] prywatne playlisty wymagają uwierzytelnienia Google i odpowiednich uprawnień.

## Realizacja planu w praktyce

1. [x] Najpierw zbudować stabilny MVP z lokalną kolejką i historią oglądania.
2. [x] Następnie dodać logowanie Google i pobieranie prywatnych playlist użytkownika.
3. [x] Potem wpiąć Android Auto i sterowanie z ekranu samochodu.
4. [ ] Na końcu dopracować UX, automatyczne wznowienie i pamięć postępu.

## Dual-mode (telefon + Android Auto)

- [ ] ten sam silnik kolejki i historii działa w obu trybach,
- [ ] tryb telefonu jest podstawową ścieżką konfiguracji konta i playlist,
- [ ] tryb Android Auto pozostaje uproszczony i bezpieczny podczas jazdy,
- [ ] regresja Android Auto jest wykonywana po przejściu testów funkcjonalnych na telefonie.

## Następny krok

Jeśli projekt będzie kontynuowany, kolejnym krokiem powinno być:
Uwaga: ekran telefonu ma już pusty stan startowy, activity-scoped `PlaylistViewModel`, retry dla błędów ładowania, bezpieczny `LazyColumn` oraz testy edge-case dla pustej kolejki i pustego inputu playlisty.
1. [ ] dopracowanie UX telefonu jako głównej ścieżki testowej,
2. [ ] dopracowanie UX Android Auto pod minimalną liczbę interakcji,
3. [x] automatyczne wznowienie po połączeniu z autem,
4. [ ] rozszerzenie testów edge-case dla integracji playback + browse tree,
5. [x] przygotowanie checklisty testów na realnym urządzeniu i w Android Auto.

## Checklista testów auto-resume (telefon + Android Auto)

- [ ] Wejście do auta z pauzowanym materiałem (`IN_PROGRESS`) wznawia odtwarzanie bez dotykania telefonu.
- [ ] Wejście do auta przy pustej kolejce niczego nie uruchamia.
- [ ] Wejście do auta przy `queueEnded=true` nie wznawia playback.
- [ ] Gdy odtwarzanie już trwa, ponowne wejście w car mode nie wywołuje podwójnego startu.
- [ ] Wyjście z car mode nie resetuje pozycji wznowienia i statusów historii.

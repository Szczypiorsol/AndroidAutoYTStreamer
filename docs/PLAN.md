# Plan aplikacji AndroidAutoYTStreamer

## Cel

Stworzyć aplikację Android do bezpiecznego sterowania odtwarzaniem długich playlist z YouTube z poziomu Android Auto, bez konieczności sięgania po telefon podczas jazdy.

## Najważniejsze wymagania

- sterowanie z ekranu samochodu w Android Auto,
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
- [ ] preferowane automatyczne wznawianie po podłączeniu do auta.

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
- [ ] automatyczne wznowienie po połączeniu z autem,
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

## Następny krok

Jeśli projekt będzie kontynuowany, kolejnym krokiem powinno być:
1. [ ] dopracowanie UX Android Auto pod minimalną liczbę interakcji,
2. [ ] automatyczne wznowienie po połączeniu z autem,
3. [ ] rozszerzenie testów edge-case dla integracji playback + browse tree,
4. [ ] przygotowanie checklisty testów na realnym urządzeniu i w Android Auto.

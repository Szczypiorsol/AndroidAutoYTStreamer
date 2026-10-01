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
- użytkownik podaje link do playlisty YouTube,
- aplikacja pobiera listę filmów,
- aplikacja buduje z nich lokalną kolejkę,
- kolejka jest odtwarzana po kolei.

### Konta Google i prywatne playlisty
- jeśli playlista ma być prywatna, konieczne jest zalogowanie użytkownika do Google / YouTube w aplikacji,
- aplikacja pobiera listę playlist użytkownika przez YouTube Data API,
- po zalogowaniu użytkownik może wybrać swoje playlisty w aplikacji,
- dane z prywatnych playlist są przechowywane lokalnie i wykorzystywane w kolejce odtwarzania.

### Historia i postęp
- każdy film może mieć status: `nieobejrzany`, `w trakcie`, `obejrzany`,
- filmy obejrzane do końca są usuwane z lokalnej kolejki,
- film przerwany przed końcem pozostaje jako ostatni niedokończony,
- przy starcie aplikacja powinna wznowić od pierwszego nieobejrzanego lub od niedokończonego filmu.

### Bezpieczeństwo
- brak potrzeby używania telefonu podczas jazdy,
- główne sterowanie z Android Auto,
- duże i proste elementy UI,
- preferowane automatyczne wznawianie po podłączeniu do auta.

## Proponowany zakres MVP

### Etap 0 - logowanie Google i prywatne playlisty
- Google Sign-In w aplikacji,
- autoryzacja do YouTube Data API,
- pobieranie listy prywatnych playlist użytkownika,
- wybór playlisty z poziomu aplikacji,
- obsługa błędów autoryzacji.

### Etap 1 - fundament
- ekran startowy w aplikacji,
- zapis linku do playlisty,
- lokalna lista filmów,
- podstawowy stan odtwarzania.

### Etap 2 - Android Auto
- integracja z ekranem samochodu,
- sterowanie play/pause/next/previous,
- widok odtwarzania zoptymalizowany pod samochód.

### Etap 3 - kolejka i historia
- odtwarzanie po kolei,
- usuwanie obejrzanych filmów z kolejki,
- wznowienie niedokończonego filmu,
- pomijanie filmów już zaliczonych.

### Etap 4 - wygoda
- ulubione playlisty,
- automatyczne wznowienie po połączeniu z autem,
- zapamiętywanie ostatniego punktu odtwarzania,
- dopracowanie UX pod jazdę.

## Ograniczenia i ryzyka

- pełna automatyzacja sterowania samą aplikacją YouTube może być ograniczona,
- najlepiej projektować własny, prosty model odtwarzania i historii,
- wszystkie funkcje muszą być zgodne z zasadami bezpieczeństwa i ograniczeniami Android Auto,
- prywatne playlisty wymagają uwierzytelnienia Google i odpowiednich uprawnień.

## Realizacja planu w praktyce

1. Najpierw zbudować stabilny MVP z lokalną kolejką i historią oglądania.
2. Następnie dodać logowanie Google i pobieranie prywatnych playlist użytkownika.
3. Potem wpiąć Android Auto i sterowanie z ekranu samochodu.
4. Na końcu dopracować UX, automatyczne wznowienie i pamięć postępu.

## Następny krok

Jeśli projekt będzie kontynuowany, kolejnym krokiem powinno być:
1. doprecyzowanie architektury,
2. wybór sposobu pobierania danych z YouTube,
3. przygotowanie logowania Google dla prywatnych playlist,
4. przygotowanie szkieletu `MediaSession` i obsługi Android Auto,
5. dodanie lokalnej historii oglądania.

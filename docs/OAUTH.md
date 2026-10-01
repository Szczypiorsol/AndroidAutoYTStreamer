# Google OAuth dla prywatnych playlist YouTube

## Cel

Dla prywatnych playlist YouTube aplikacja musi mieć dostęp do konta użytkownika z poziomu Google. Sama aplikacja YouTube zalogowana na telefonie nie przekazuje tego dostępu automatycznie do innej aplikacji.

## Co jest potrzebne

- projekt w Google Cloud Console,
- aktywne konto Google,
- utworzenie OAuth Client ID dla Androida,
- włączenie YouTube Data API v3,
- wybór odpowiednich scope’ów,
- implementacja Google Sign-In w aplikacji.

## Scope’y

Dla odczytu prywatnych playlist najlepiej użyć scope:

- `https://www.googleapis.com/auth/youtube.readonly`

Dodatkowo przy niektórych operacjach przydatny może być:

- `https://www.googleapis.com/auth/youtube.force-ssl`

W praktyce do listy playlist, ich nazw i listy filmów zwykle wystarcza `youtube.readonly`.

## Flow działania

1. Użytkownik uruchamia aplikację.
2. Aplikacja pokazuje ekran logowania Google.
3. Użytkownik wybiera swoje konto Google.
4. Aplikacja pobiera token dostępu.
5. Aplikacja wywołuje YouTube Data API:
   - `playlists.list`
   - `playlistItems.list`
6. Lista prywatnych playlist jest zapisywana lokalnie.
7. Z listy wybierana jest konkretna playlista do kolejki.
8. Kolejka jest odtwarzana według lokalnej logiki aplikacji.

## Wymagania techniczne

### 1. Google Cloud Console

- utworzyć projekt,
- włączyć YouTube Data API v3,
- utworzyć OAuth Client ID,
- dodać SHA-1 podpisu aplikacji w debug/release,
- pobrać `google-services.json` dla Firebase / Google Sign-In.

### 2. Android

- dodać zależności Google Play Services,
- dodać `GoogleSignInOptions` z `requestScopes` dla YouTube,
- użyć `GoogleSignIn.getLastSignedInAccount(context)`,
- pobrać token dostępu i wysyłać zapytania do YouTube API.

## Rekomendacja dla MVP

Nie zaczynaj od pełnego OAuth od razu. Najbardziej sensowne jest:

1. najpierw MVP bez logowania,
2. potem Google Sign-In,
3. potem prywatne playlisty,
4. potem Android Auto i poprawki UX.

## Wniosek

Jeżeli chcesz odtwarzać własną prywatną playlistę, logowanie do Google jest niezbędne. Samo zalogowanie YouTube na telefonie nie wystarczy, bo aplikacje Android nie dzielą kont między sobą bez explicit OAuth.

## Dalszy krok

Następnie trzeba dodać:
- `Google Sign-In` do aplikacji,
- `YouTube Data API` client,
- listę prywatnych playlist po zalogowaniu,
- wybór playlisty z poziomu aplikacji.


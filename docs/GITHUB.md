# GitHub setup for AndroidAutoYTStreamer

## Proponowana nazwa repozytorium

- `AndroidAutoYTStreamer`

## Krótki opis repo

Aplikacja Android do bezpiecznego odtwarzania długiej playlisty YouTube w trybie telefonu i Android Auto, z lokalną historią odtwarzania i wznowieniem ostatniego niedokończonego filmu.

## Opis do wklejenia na GitHuba

AndroidAutoYTStreamer to aplikacja Android do bezpiecznego odtwarzania długiej playlisty YouTube w dwóch trybach: telefon i Android Auto. Projekt zakłada odtwarzanie materiałów po kolei, zapamiętywanie postępu, pomijanie już obejrzanych filmów i wznowienie ostatniego niedokończonego filmu. W samochodzie interfejs jest uproszczony pod bezpieczne, minimalne interakcje.

## Tagi / topics

- android
- android-auto
- kotlin
- youtube
- media-session
- car-app
- playlist
- video-player

## Co powinno być widoczne na GitHubie

- `README.md` z krótkim opisem projektu,
- `docs/PLAN.md` z planem rozwoju,
- `docs/GITHUB.md` z decyzjami organizacyjnymi,
- czytelna historia commitów,
- pierwszy commit z dokumentacją i szkieletem projektu.

## Proponowana kolejność pierwszych commitów

1. `docs: add project plan and GitHub setup notes` — dodaje `README.md`, `docs/PLAN.md` i `docs/GITHUB.md`.

2. `chore: prepare Android project baseline` — porządkuje strukturę projektu, sprawdza `.gitignore` i dopina drobne poprawki szkieletu aplikacji.

3. `feat: add playback architecture skeleton` — przygotowuje podstawy pod `MediaSession` i rozpoczyna integrację z Android Auto.

4. `feat: add playlist history model` — dodaje lokalny stan odtwarzania, oznaczanie obejrzanych filmów i wznowienie niedokończonego filmu.

## Pierwsze komendy do wypchnięcia na GitHuba

```bash
git init
git add .
git commit -m "docs: add project plan and GitHub setup notes"
git branch -M main
git remote add origin <URL_REPOZYTORIUM>
git push -u origin main
```

## Checklist przed publikacją

- [ ] repo ma `README.md`,
- [ ] plan rozwoju jest zapisany,
- [ ] `.gitignore` nie przepuszcza plików tymczasowych,
- [ ] nazwa repo i opis są spójne,
- [ ] pierwszy commit zawiera dokumentację startową,
- [ ] projekt można łatwo rozwijać etapami.


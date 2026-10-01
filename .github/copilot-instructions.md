# Copilot Instructions for AndroidAutoYTStreamer

## Project context
This repository contains an Android app for safe YouTube playlist playback in Android Auto. Keep the project focused on driving safety, minimal interaction, and stable playback flow.

## Coding rules
- Prefer Kotlin and Compose idioms already used in this project.
- Keep code simple, readable, and small in scope.
- Do not add dependencies unless they are clearly needed.
- Preserve lifecycle correctness and avoid leaking Activities/Contexts.
- When handling YouTube API keys, use `local.properties` and avoid committing secrets.
- Prefer graceful fallback behavior when external API configuration is missing.

## Domain guidance
- Treat playlist management, watch history, and resume state as core app logic.
- Keep Android Auto UX minimal and safe for drivers.
- Respect secure authentication boundaries for Google/YouTube access.
- Focus on deterministic queue behavior and resume-after-interruption logic.

## Preferred output
- Propose small patches rather than broad rewrites.
- Include tests for queue/history edge cases when changing playback logic.
- Update docs when behavior or setup changes materially.

## Repo hotspots
- `app/src/main/java/...` for app logic
- `PlaylistQueueManager.kt` for queue logic
- `PlaylistHistoryStore.kt` for watch-state persistence
- `GoogleAuthManager.kt` for Google auth flows
- `YouTubePlaylistRepository.kt` and `YouTubePrivatePlaylistRepository.kt` for YouTube integration
- `PlaybackService.kt` and `MediaSessionController.kt` for playback and Android Auto controls


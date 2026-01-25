# Family Space

Monorepo with Android app (Jetpack Compose, Material 3) and Ktor backend REST API.

## Getting started

1. Ensure Java 17 is available.
2. Generate the Gradle wrapper JAR once if absent: `gradle wrapper --gradle-version 8.6`.
3. Build Android app: `./gradlew :androidApp:assembleDebug`.
4. Run backend: `./gradlew :backend:run` (server on port 8080).

## Modules

- `androidApp`: Compose Material 3 app with navigation placeholders for auth, feed, post creation, comments, and profile.
- `backend`: Ktor REST API with in-memory data and endpoints for auth, posts, comments, and users.

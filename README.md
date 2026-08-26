# Family Space

Monorepo avec application Android (Jetpack Compose, Material 3) et API REST Ktor.

## Démarrage rapide

1. Java 17+ requis.
2. Configurer le SDK Android : créer `local.properties` avec `sdk.dir=/chemin/vers/Android/Sdk`.
3. Build debug : `./gradlew :androidApp:assembleDebug`
4. Lancer le backend : `./gradlew :backend:run` (port 8080)

## Publier sur Google Play (track interne / dev)

Cette release **v1.1.0 (versionCode 2)** inclut les mises à jour requises pour Google Play :

- `targetSdk 35` (Android 15) — obligation Play Console
- Écran **Politique de confidentialité** (Profil → Privacy Policy)
- Affichage de la version dans le profil
- Support edge-to-edge et retour prédictif

### 1. Configurer la signature

```bash
cp keystore.properties.example keystore.properties
# Éditer keystore.properties avec votre clé upload Play Console
```

### 2. Générer l'AAB release

```bash
./gradlew :androidApp:bundleRelease
```

Le fichier se trouve dans :
`androidApp/build/outputs/bundle/release/androidApp-release.aab`

### 3. Uploader sur Play Console

1. [Google Play Console](https://play.google.com/console) → votre app **Family Space**
2. **Testing** → **Internal testing** (ou Closed/Open testing)
3. **Create new release** → uploader l'AAB
4. Notes de version (exemple) :
   > v1.1.0 — Mise à jour de conformité Play Store : targetSdk 35, politique de confidentialité, améliorations UI.
5. **Review release** → **Start rollout**

> **Important** : chaque nouvelle release doit incrémenter `versionCode` dans `androidApp/build.gradle.kts`.

## Modules

- `androidApp` : app Compose avec auth, fil d'actualité, création de posts, commentaires et profil.
- `backend` : API Ktor en mémoire (auth, posts, commentaires, utilisateurs).

## Améliorations recommandées (roadmap)

| Priorité | Amélioration | Impact |
|----------|-------------|--------|
| Haute | Connecter l'app au backend Ktor (Retrofit/Ktor Client) | Données réelles au lieu des mocks |
| Haute | Authentification JWT complète côté backend | Sécurité production |
| Haute | Héberger la politique de confidentialité en URL publique | Obligation Play Console (Data safety) |
| Moyenne | Upload photos (CameraX + stockage cloud) | Fonctionnalité centrale |
| Moyenne | Notifications push (FCM) pour nouveaux posts/commentaires | Engagement |
| Moyenne | Persistance locale (Room) + mode hors-ligne | UX |
| Basse | Thème Material 3 personnalisé (couleurs famille) | Identité visuelle |
| Basse | Tests instrumentés Compose | Qualité CI |

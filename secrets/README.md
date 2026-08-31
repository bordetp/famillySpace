# Secrets — à déplacer à la main, jamais Git

Ce dossier est dans `.gitignore`. Copiez-le sur l’autre PC (clé USB, disque, etc.), puis replacez chaque fichier.

## Destination sur l’autre machine

| Fichier dans `secrets/` | Où le mettre |
|---|---|
| `keystore.properties` | Racine du repo |
| `keystore_familly` | Racine du repo |
| `play-store-credentials.json` | `deploy/play-store-credentials.json` |
| `firebase-admin.json` | `deploy/firebase-admin.json` |
| `oracle.key` | `%USERPROFILE%\.ssh\oracle.key` |

PowerShell, depuis la racine du repo cloné :

```powershell
Copy-Item secrets\keystore.properties .\keystore.properties
Copy-Item secrets\keystore_familly .\keystore_familly
Copy-Item secrets\play-store-credentials.json .\deploy\play-store-credentials.json
Copy-Item secrets\firebase-admin.json .\deploy\firebase-admin.json

New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh" | Out-Null
Copy-Item secrets\oracle.key "$env:USERPROFILE\.ssh\oracle.key"
icacls "$env:USERPROFILE\.ssh\oracle.key" /inheritance:r
icacls "$env:USERPROFILE\.ssh\oracle.key" /grant:r "$($env:USERNAME):(R)"
```

Config SSH (créer `%USERPROFILE%\.ssh\config`) : voir `famillyspace/ssh/config.example`.

## Absents ici (pas trouvés en local)

- `androidApp/google-services.json` — FCM, optionnel
- `deploy/duckdns.token` — seulement pour recréer DuckDNS
- `.env` de la VM — déjà sur `opc@141.253.105.251:/home/opc/familyspace/.env`

## Vérifications

```powershell
.\deploy\publish-internal.ps1
.\deploy\deploy-from-windows.ps1
```

Linux :

```bash
chmod +x deploy/publish-internal.sh
./deploy/publish-internal.sh
```

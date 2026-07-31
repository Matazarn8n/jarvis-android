# jarvis-android

App Android « Jarvis » (sideload, usage personnel). Coquille Kotlin minimale :
WebView sur l'origine HTTPS Tailscale Serve `https://nuveo-server.tail438ad2.ts.net`
+ service micro wake-word + abonné ntfy. **L'UI est la SPA hermes-os** — l'app
n'ajoute que ce qu'un navigateur ne sait pas faire.

- Architecture : `/home/nuveo/hermes-os/docs/architecture-jarvis-android-2026-08.md`
- Plan : `/home/nuveo/hermes-os/docs/superpowers/plans/2026-07-31-jarvis-android.md`
- Runbook d'exécution P0/P1 : `/home/nuveo/hermes-os/runbooks/jarvis-android-p0p1.runbook.yaml`

## Build

```bash
cd /home/nuveo/projects/jarvis-android
./gradlew --console=plain assembleDebug     # -> app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Toolchain du GEEKOM (constatée le 2026-07-31) : SDK `/home/nuveo/.local/android-sdk`
(platforms android-35/36, build-tools 34.0.0 + 35.0.0), JDK 21, wrapper Gradle
8.14.3, AGP 8.13.0, Kotlin 2.0.21. `local.properties` (non versionné) porte
`sdk.dir=/home/nuveo/.local/android-sdk`. `android-34` n'est pas installé :
`compileSdk 35` + `targetSdk 34`.

## Terrain téléphone

Relevé sur l'appareil par le ticket JA-T5 (`adb shell getprop`), pas supposé.
L'architecture porte l'hypothèse « très probablement un Galaxy A16 » — à
remplacer ici par les valeurs réelles, une ligne par propriété :

```
ro.product.model=
ro.build.version.release=
ro.build.version.sdk=
```

## Assets wake-word

Les modèles ONNX et leur configuration sont dans `app/src/main/assets/wake/` :

- `hey_jarvis_v0.1.onnx` — graphe de détection du mot-clé
- `melspectrogram.onnx` — extracteur de features audio
- `embedding_model.onnx` — embeddings partagés
- `wake.json` — seuil, période réfractaire et métadonnées

Pour remplacer le modèle : déposer un nouveau `.onnx` dans ce dossier et mettre
à jour `wake.json` (champs `model`, `threshold`, `refractory_ms`). Rien d'autre
dans le code ne connaît le modèle. Licence des poids : CC BY-NC-SA 4.0 (voir
`NOTICE.md` et `docs/licences-wake.md`).

## État

- **P0** (spike détection + licences) : en cours — tickets JA-T2 … JA-T7.
- **P1** (coquille WebView v0) : à venir — tickets JA-T8 … JA-T11.

## Sécurité

Aucun secret ni keystore dans ce dépôt. Le keystore de signature vivra dans
`~/.openclaw/keystores/` (phase P5), couvert par le backup GPG maison.

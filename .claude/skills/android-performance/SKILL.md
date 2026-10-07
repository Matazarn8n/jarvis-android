---
name: android-performance
description: Optimisation des performances d'une app Android (Kotlin / Jetpack Compose) — cinq fiches de référence prêtes : démarrage (TTID/TTFD, Baseline et Startup Profiles), rendu/jank (budget 16 ms, overdraw), ANR (timeouts par type), mémoire (PSS, onTrimMemory, R8), batterie/background (JobScheduler, broadcasts implicites). Déclencher sur — « l'app est lente au démarrage », « ça rame / jank / saccade au scroll », « ANR », « OutOfMemoryError », « fuite mémoire », « consomme la batterie », « cold start », « baseline profile », « optimise les perfs Android ». NE PAS utiliser pour enregistrer ou lire une trace / heap dump (android-profiler), ni pour les keep rules R8 (r8-analyzer), ni pour les insets système (edge-to-edge).
---

# Android Performance

Lire UNE fiche selon le symptôme, jamais les cinq. Chaque fiche donne
seuils chiffrés, diagnostic, corrections Kotlin.

| Symptôme | Fiche |
|---|---|
| Lancement lent, cold/warm start, TTID/TTFD, Baseline Profiles | [references/demarrage.md](references/demarrage.md) |
| Saccades, scroll qui accroche, overdraw, frames > 16 ms | [references/rendu-jank.md](references/rendu-jank.md) |
| Dialogue « ne répond pas », input bloqué, receiver/service trop lent | [references/anr.md](references/anr.md) |
| OutOfMemoryError, fuite, GC pendant animation, PSS élevé | [references/memoire.md](references/memoire.md) |
| Batterie, travail en arrière-plan, broadcasts perdus après API 24 | [references/batterie.md](references/batterie.md) |

Méthode : mesurer d'abord (Macrobenchmark, Perfetto via android-profiler),
appliquer la correction de la fiche, remesurer. Aucune optimisation sans
chiffre avant/après.

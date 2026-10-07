# Démarrage (startup)

Sources : appstartup/best-practices, appstartup/analysis-optimization, baselineprofiles/overview, baselineprofiles/create-baselineprofile, startupprofiles/overview.

## Métriques et seuils

- **TTID** (Time to Initial Display) : premier frame affiché. Mesure la vitesse perçue.
- **TTFD** (Time to Full Display) : app réellement utilisable. Appeler `reportFullyDrawn()` quand atteint, sinon le contenu chargé en async (listes, images) n'est jamais compté.
- Seuils cibles mesurés (measuring-performance) : **cold start < 500ms, warm start < 200ms, hot start < 150ms**. P95/P99 doivent rester proches de la médiane — une longue traîne signale un problème d'environnement (I/O contention, autre app), pas juste du code lent.
- **Toute opération main-thread > 20ms** au démarrage est à investiguer (Choreographer draw, composition initiale, chargement lib, transactions Binder, chargement ressources).

## Actions à fort ROI (dans l'ordre)

1. **Baseline Profiles** — ~30% de gain sur la vitesse d'exécution dès le premier lancement (évite la compilation JIT à froid, déclenche l'AOT via PGO). Gain disponible immédiatement pour nouveaux users/updates (contrairement aux Cloud Profiles qui prennent des jours).
2. **Startup Profiles** — optimisation DEX layout au moment de la compilation (regroupe les classes utilisées au démarrage dans le DEX primaire) → réduit les page faults. ~15% de gain additionnel. Combiner avec Baseline Profiles. Augmente la taille APK — mesurer en A/B.
3. **App Startup library** — remplace les multiples ContentProvider de démarrage par des initializers partageant un seul provider.
4. **Lazy load** — désactiver l'auto-init des libs (ex. WorkManager on-demand init), différer après le premier frame.
5. **Compose conditionnel** — ne composer que ce qui est visible au lancement :
```kotlin
var shouldLoad by remember { mutableStateOf(false) }
if (shouldLoad) { MyComposable() }  // composant lourd différé
LaunchedEffect(Unit) { shouldLoad = true }
```
6. **Splash screen système** (API 31+) — conçu pour la perf, ne pas le réinventer.
7. **Images** : vectoriel > WebP > PNG/JPEG. Minimiser nombre et taille des images chargées au démarrage.

## Baseline Profiles — implémentation

Config minimale : AGP 8.0+, `androidx.benchmark:benchmark-macro-junit4:1.4.1+`, `androidx.profileinstaller:profileinstaller:1.4.1+`.

Piège critique : générer le profil **sans** obfuscation (`isMinifyEnabled = false` sur le build de benchmark) ; le build de release, lui, **doit** avoir R8 activé (`isMinifyEnabled = true`) — R8 réécrit les règles du profil non-obfusqué pour matcher le code obfusqué. Inverser les deux = signatures de méthode incorrectes.

```kotlin
class BaselineProfileGenerator {
    @get:Rule val baselineProfileRule = BaselineProfileRule()

    @Test
    fun appStartupAndUserJourneys() {
        baselineProfileRule.collect(packageName = PACKAGE_NAME) {
            uiAutomator {
                startApp(PACKAGE_NAME)
                // interactions critiques : scroll, navigation, login...
            }
        }
    }
}
```

Inclure dans le profil : démarrage, navigation entre écrans, scroll de listes, flux critiques (login, paiement). Fichier généré `baseline.prof` (binaire), limite **1.5 MB**.

Modules multi-variantes (AGP 8.0+) : `src/variant/baselineProfiles/baseline-prof.txt` au lieu d'un seul fichier `src/main/`.

Template recommandé : File > New Module > "Baseline Profile Generator" (AGP 8.2+), génère le module de benchmark automatiquement.

Device : rooté OU API 33+ requis pour Macrobenchmark connecté ; sinon Gradle-managed device avec `systemImageSource = "aosp"`.

Pièges :
- Oublier `reportFullyDrawn()` → contenu async exclu du profil.
- Mélanger tests startup et non-startup dans `includeInStartupProfile = true` → bloat du Startup Profile.
- OnePlus : désactiver "Disable permission monitoring" dans Developer Options, sinon erreurs de permission pendant la génération.
- Huawei : désactiver l'optimisation batterie sur l'appareil de benchmark.
- Distribution hors Play Store : les Baseline Profiles ne s'appliquent qu'après le dexopt en arrière-plan.

## Analyse et diagnostic (Macrobenchmark + Perfetto)

- Outil recommandé : **Macrobenchmark** avec `StartupTimingMetric` → temps de lancement, frames perdus, temps de layout/mesure, temps de chargement des assets.
- Visualiser les traces sur `ui.perfetto.dev`.
- **StrictMode** en debug pour choper les violations main-thread :
```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder().detectAll().penaltyDeath().build()
    )
}
```
- Chercher : thread principal bloqué/en sommeil (indicateurs orange dans Perfetto), I/O bloquant, appels Binder redondants (cacher au lieu de répéter).

## Pièges nommés

- Bloquer le main thread sur I/O fichier/réseau.
- Opérations lourdes dans `Application.onCreate()`.
- Binder calls redondants non cachés.
- Bitmaps larges chargés de façon synchrone au démarrage.
- Ne pas activer StrictMode en debug.
- **Trampoline Activity** (measuring-performance) : deux `activityStart` consécutifs sans frame du premier — refactoriser en composant réutilisable. Android 12+ interdit de lancer une activité depuis un service/broadcast receiver comme trampoline.

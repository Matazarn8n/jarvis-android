# ANR (App Not Responding)

Sources : anrs/keep-your-app-responsive, anrs/diagnose-and-fix-anrs.

## Timeouts par type

| Type ANR | Timeout | Détail |
|---|---|---|
| Input dispatch | 5s | touche/tap sans réponse ; toujours visible utilisateur |
| No focused window | 5s | pas de fenêtre focalisée pour l'event clavier |
| Broadcast receiver | 10-20s (foreground, A14+) / 60-120s (background, A14+) | sync : timeout `onReceive()` ; async : `PendingResult.finish()` jamais appelé |
| Execute service | 20s (foreground) / 200s (background) | `onCreate()`/`onStartCommand()`/`onBind()` trop lents |
| Content provider | configurable via `ContentProviderClient.setDetectNotResponding()` | requête distante trop longue |
| Slow job response | implicite | `JobService.onStartJob()`/`onStopJob()`/`setNotification()` lent |

Seuil de perception utilisateur : **100-200ms** au-delà duquel la lenteur est perçue.

## Stratégies générales

```kotlin
// ❌ bloque le thread principal
val data = fetchDataFromNetwork()
// ✅ coroutines background
viewModelScope.launch(Dispatchers.IO) {
    val data = fetchDataFromNetwork()
    withContext(Dispatchers.Main) { updateUI(data) }
}
```
- Ne jamais bloquer dans un `@Composable` — utiliser `LaunchedEffect`.
- Ne pas partager un thread pool entre opérations longues-bloquantes et tâches sensibles au temps.
- Défaut = pas de synchronized blocks sur le thread UI.
- Startup : minimiser le travail dans `onCreate()`/`onResume()`, Baseline/Startup Profiles, App Startup library pour la DI, différer l'init non critique.

## Broadcast receivers

```kotlin
// ❌ traitement lourd synchrone
override fun onReceive(context: Context?, intent: Intent?) { heavyComputation() }

// ✅ goAsync() + coroutine, MUST appeler finish()
override fun onReceive(context: Context?, intent: Intent?) {
    val result = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try { heavyComputation() } finally { result.finish() }
    }
}
```
Timeout détecté via le flag intent (`flg=`) : bit `0x10000000` (`FLAG_RECEIVER_FOREGROUND`) présent → 10-20s ; sinon 60-120s.

Piège de pool : ne pas partager le thread pool des receivers async avec d'autres tâches longues — un pool épuisé par ailleurs = `finish()` jamais appelé = ANR même si le code du receiver est correct. Dédier un pool.

## Services

Le chrono démarre à la dispatch de l'intent système, s'arrête au retour de `onCreate()`/`onStartCommand()`/`onBind()`. Pour un foreground service : appeler `startForeground()` avec la notification **avant** tout traitement long, pas après.

## Content provider

Optimiser la requête (WHERE, LIMIT), ne pas faire d'appel Binder synchrone bloquant dans `query()` (épuisement des threads Binder).

## Violation de threading vue (sync barrier leak) — critique

Modifier une View depuis un thread background peut **laisser fuir une barrière de synchronisation dans la MessageQueue** et geler l'UI thread **de façon permanente** — pas juste un crash ponctuel.
```kotlin
// ❌ fuite de sync barrier
lifecycleScope.launch(Dispatchers.IO) {
    val data = repo.getData()
    myTextView.text = data.title
}
// ✅ toujours revenir sur Main
lifecycleScope.launch(Dispatchers.IO) {
    val data = repo.getData()
    withContext(Dispatchers.Main) { myTextView.text = data.title }
}
```
Alternatives sûres : `ContextCompat.getMainExecutor()`, `View.post {}`, `Activity.runOnUiThread {}`.

Debug (Android 17+) :
```bash
adb shell am compat enable ENFORCE_THREAD_CHECKS_ON_VIEW_ROOT_IMPL_APIS com.example.app
```

## nativePollOnce — ANR avec main thread apparemment idle

Causes possibles : charge système globale, stack dump pris trop tard (latence 100ms-1s), ou violation de threading vue (voir ci-dessus). Action par type : input dispatch → généralement rien à faire côté app ; les autres types → chercher une violation de threading vue.

## Workflow de diagnostic

1. Vérifier la signature du cluster (Play Console / Firebase Crashlytics).
2. Ignorer si "nativePollOnce" + input dispatch isolé (probable souci système).
3. Chercher dans la stack du thread principal : `ActivityThread.handleBindApplication` (démarrage lent), `<Service>.onCreate()`+`handleCreateService` (init service lente), `<Service>.onStartCommand()`+`handleServiceArgs` (exécution service lente), `ContentProvider$Transport.query` (requête provider lente).
4. Vérifier les autres threads : lock contention, I/O bloquant, mutation de vue hors main thread.
5. Perfetto pour l'état des threads (running vs blocked).

## Top 3 règles de prévention

1. Jamais de DB/réseau/I/O/calcul lourd sur le main thread.
2. Toujours revenir sur le dispatcher Main avant de toucher une vue.
3. Démarrage, init de service, traitement de broadcast : rester sous le timeout — lazy init, différer le travail, optimiser.

## StrictMode
```kotlin
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(
        StrictMode.ThreadPolicy.Builder()
            .detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build()
    )
}
```

# Mémoire

Sources : memory-overview, memory/manage-app-memory.

## Principes

- Seul moyen de libérer de la mémoire gérée : relâcher les références d'objets → le GC récupère. Exception : fichiers mmapped non modifiés (code, ressources) peuvent être paginés sans intervention.
- GC générationnel (young → old → permanent) ; chaque génération a une limite, le GC se déclenche quand elle est pleine.
- **Seuil critique de rendu : 16ms/frame (60 FPS)**. Un GC déclenché pendant une animation dépasse facilement ce budget → jank. Cause typique : allocations répétées dans une boucle (alpha blending, animations).
- Heap size limité par device (`ActivityManager.getMemoryClass()` en MB) ; dépassement → `OutOfMemoryError`. Varie fortement entre devices (critique sur low-RAM / Android Go).
- Mesurer en **PSS** (Proportional Set Size), pas la taille logique du heap — PSS reflète le partage mémoire réel (Zygote fork, code framework mmapped et partagé entre apps).
- Pas de compaction du heap logique : les petites allocations/désallocations sont peu efficaces à récupérer physiquement (page potentiellement partagée) ; les grandes le sont bien. → pooler les objets pour les allocations fréquentes en boucle.

## Actions concrètes

### R8 réduit l'empreinte mémoire directement
Suppression de code mort, fusion de classes, inlining, minification des noms → moins de bytecode chargé en RAM. Ne jamais utiliser `-dontoptimize`, `-dontshrink`, `-dontobfuscate`, ni `-keep class pkg.** { *; }` sur un package entier (voir taille-apk-r8.md).

### Implémenter `onTrimMemory` (ComponentCallbacks2)
```kotlin
override fun onTrimMemory(level: Int) {
    if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
        imageLoader.clearMemory()   // Coil/Glide/Picasso
        videoPlayer.release()
    }
    if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
        database.close()
    }
}
```
Libérer les ressources reconstructibles dans `onStop()`, pas `onDestroy()`.

### Vérifier la mémoire avant une opération lourde
```kotlin
val memInfo = ActivityManager.MemoryInfo().also {
    (getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(it)
}
if (!memInfo.lowMemory) { /* proceed */ }
```

### Bitmaps
- ARGB_8888 = 4 octets/pixel. Un JPEG de 100 Ko à 1000×1000 px pèse **4 Mo en mémoire décodé**.
- Utiliser Coil/Glide/Picasso avec appel explicite à `clearMemory()`.

### Containers optimisés
```kotlin
❌ HashMap<Int, String>        // autoboxing des clés
✅ SparseArray<String> / SparseBooleanArray / LongSparseArray
```

### Éviter l'allocation en boucle chaude
```kotlin
// ❌ 1000 allocations
for (i in 0..1000) { val path = Path(); canvas.drawPath(path, brush) }
// ✅ réutiliser
val path = Path()
for (i in 0..1000) { path.reset(); canvas.drawPath(path, brush) }

// Compose : remember, pas de réallocation à chaque recomposition
@Composable fun MyComposable() {
    val brush = remember { Brush.linearGradient(...) }
}
```

### Injection de dépendances
Hilt (compile-time, sans réflexion) plutôt qu'un framework DI par réflexion (scan au démarrage = coût CPU/RAM).

### Éviter les Services persistants
Un service qui tourne empêche le process d'être mis en cache. Utiliser `WorkManager` à la place.

### Protobuf lite
Toujours la version *lite* côté client (les protobufs complets génèrent du code verbeux, RAM + APK size).

## Diagnostic & seuils Android Vitals

- **LMK rate (Low Memory Kills) > 1% = urgent**. Un taux bas ne garantit pas la santé : les kills en arrière-plan dégradent quand même les warm starts suivants.
- Seuils Vitals par RAM totale de l'appareil (foreground / user-perceived service / background), ex. 4 Go : 2.00 / 1.00 / 1.00 Go ; 8 Go : 2.25 / 1.50 / 1.50 Go ; 16 Go : 4.25 / 2.00 / 2.00 Go.
- Bitmap memory : seuil **200 Mo (foreground/services)**, **400 Mo (background/cached)**.

## Commandes de diagnostic
```bash
adb shell ps -A | grep your.package.name
adb shell cat /proc/<pid>/oom_score_adj      # 0-199 foreground, 900+ cached/at-risk
adb shell dumpsys meminfo <pid>
adb shell cat /proc/<pid>/status | grep -E "VmRSS|RssAnon|VmSwap"
adb shell dumpsys activity processes your.package.name

# Stress test mémoire (low-RAM / Android Go)
adb push stressapptest /data/local/tmp/
adb shell /data/local/tmp/stressapptest -s 20 -M 990
```
Signaux : `SIGABRT` = crash natif fatal (allocation > mémoire libre) ; `SIGQUIT` = core dump + terminaison.

## Pièges nommés

- Logique de libération dans le ViewModel plutôt que Activity/Fragment → fuite de Context.
- Ignorer le partage mémoire (Zygote, mmap) → mesures de RAM fausses si on regarde la taille logique du heap au lieu du PSS.
- Multiplier les libs externes sans vérifier les implémentations dupliquées (deux versions de protobuf, plusieurs frameworks de logs).

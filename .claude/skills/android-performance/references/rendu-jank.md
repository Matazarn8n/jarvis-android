# Rendu / jank

Sources : rendering, rendering/overdraw, rendering/optimizing-view-hierarchies, hardware-accel.

## Budget de frame

- **16ms par frame pour tenir 60 FPS** ; measuring-performance vise **90Hz minimum** sur devices modernes (60/90/120Hz).
- Jank = à-coup visuel quand le système ne fournit pas les frames à temps (visible surtout au scroll).

## Overdraw

Définition : dessiner le même pixel plusieurs fois dans une frame (ex. cartes empilées, background caché par son contenu). Rendu back-to-front ("algorithme du peintre") nécessaire pour l'alpha blending correct, mais coûte du fill-rate GPU.

Diagnostic :
- **Debug GPU Overdraw** (Developer Options) : code couleur = nb de fois qu'un pixel est dessiné.
- **Profile GPU Rendering** : histogramme défilant par étape du pipeline ; un pic sur la barre **Process (orange)** + overdraw lourd = problème confirmé. Ne fonctionne pas avec les apps NDK (OpenGL prend le contexte plein écran).
- **Layout Inspector** : repérer les backgrounds non visibles.

Corrections :
```kotlin
// ❌ background du parent jamais visible (couvert par l'enfant)
Box(Modifier.background(Color.White)) {
    Box(Modifier.background(Color.Blue).fillMaxSize()) { /* contenu */ }
}
// ✅ retirer le background du conteneur
Box {
    Box(Modifier.background(Color.Blue).fillMaxSize()) { /* contenu */ }
}
```
```kotlin
// ❌ alpha sur tout le composant = double coût
Text("Gray text", modifier = Modifier.alpha(0.5f))
// ✅ couleur opaque directement
Text("Gray text", color = Color.Black.copy(alpha = 0.5f))
```
Points de vigilance : animations transparentes, fade-outs, drop shadows. Sur devices bas de gamme, prioriser l'optimisation UI thread plutôt que le fill-rate GPU.

Règle pratique hardware-accel : viser **max ~2.5× pixels dessinés par frame**.

## Hiérarchies de vues

- Layout-and-measure doit tenir dans les 16ms.
- **"Double taxation"** = le framework fait plusieurs passes de layout au lieu d'une seule. Causes : `RelativeLayout` (2+ passes), `LinearLayout` horizontal avec poids, `LinearLayout` vertical + `measureWithLargestChild`, `GridLayout` avec weights+Gravity.
- Classement perf (meilleur → pire) : `FrameLayout` > `ConstraintLayout` > `LinearLayout` non pondéré > `RelativeLayout` > `LinearLayout` pondéré imbriqué.
- Remplacer `RelativeLayout`/`ListView` par `ConstraintLayout`/`RecyclerView`.
- Aplatir la hiérarchie : `<merge>` au lieu d'imbriquer un layout du même type dans un `<include>`.
```xml
<!-- reusable_layout.xml -->
<merge xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- contenu -->
</merge>
```
- Diagnostic : Profile GPU Rendering (barre bleue = temps de layout), Lint (Analyze > Inspect Code > Android > Lint > Performance), Layout Inspector, Perfetto.
- Note Compose : la hiérarchie aplatie est un non-sujet (single-pass layout) — cible surtout les vues XML legacy.

## Hardware acceleration

Activation : manifest `android:hardwareAccelerated="true"` (défaut si targetSdk ≥ 14), configurable par Activity, pas désactivable au niveau Window, pas activable au niveau View seul (`setLayerType(LAYER_TYPE_SOFTWARE, null)` désactive seulement).

Vérifier : `canvas.isHardwareAccelerated()` (plus fiable que sur la View).

Opérations non supportées : `setLinearText()`, `setMaskFilter()` (jamais) ; `drawPicture()` depuis API 23 ; `drawVertices()` depuis API 29 ; `clipPath()` depuis API 18.

Bonnes pratiques :
- Créer `Paint`/`Path` une seule fois, jamais dans `onDraw()`.
- Éviter de modifier des bitmaps fréquemment (re-upload de texture GPU à chaque fois).
- Pour une animation d'alpha sur une grande vue : `setLayerType(LAYER_TYPE_HARDWARE, null)` avant, `LAYER_TYPE_NONE` après (libère la VRAM) :
```kotlin
view.setLayerType(View.LAYER_TYPE_HARDWARE, null)
ObjectAnimator.ofFloat(view, "rotationY", 180f).apply {
    addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(a: Animator) { view.setLayerType(View.LAYER_TYPE_NONE, null) }
    })
    start()
}
```
- Propriétés animables sans redraw complet : `alpha, x, y, translationX/Y, scaleX/Y, rotation*, pivotX/Y`.
- Hardware acceleration consomme plus de RAM → tester sur device réel, profiler.

## Pièges nommés

- Nested scrolling : lazy layout scrollable dans un container scrollable sans contraintes explicites → jank.
- Lazy layout sans `item keys` → invalide tout l'état backing au lieu de muter juste ce qui change → recompositions excessives.
- Prefetch de données insuffisant/trop tardif en fin de liste → attente utilisateur visible.

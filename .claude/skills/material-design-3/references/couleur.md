# Couleur — Material 3

## Rôles de couleur (color roles)

M3 définit des couleurs par RÔLE, pas par valeur. Chaque rôle a une paire `X`/`onX` (contenu à
poser dessus) :

- `primary` / `onPrimary` — action principale (bouton FAB, bouton rempli)
- `primaryContainer` / `onPrimaryContainer` — conteneur moins saturé pour la même famille
- `secondary` / `onSecondary`, `secondaryContainer` / `onSecondaryContainer` — accents moins
  prioritaires (chips, filtres)
- `tertiary` / `onTertiary` — accent de contraste, usage éditorial (rare, décoratif)
- `error` / `onError`, `errorContainer` / `onErrorContainer` — états d'erreur uniquement
- `surface` / `onSurface` — fond par défaut d'un écran/carte
- `surfaceVariant` / `onSurfaceVariant` — fond légèrement différencié (champ de texte, séparateur)
- `outline` — bordures, diviseurs à faible emphase
- `outlineVariant` — diviseurs encore plus discrets

Ne jamais assigner une couleur `onX` à un fond qui n'est pas `X` — c'est la cause n°1 de textes
illisibles en dark mode (le rôle garantit le contraste, une couleur codée en dur ne le garantit
pas quand le thème change).

## Dynamic color (Material You)

Sur Android 12+ (API 31+), extraire une palette depuis le fond d'écran de l'utilisateur :

```kotlin
val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
} else {
    if (isDark) darkColorScheme(...) else lightColorScheme(...)
}
```

Toujours prévoir le repli codé en dur pour API < 31 — dynamic color n'est pas rétro-compatible et
il n'existe pas de polyfill fiable.

## Contraste — seuils WCAG appliqués par M3

- Texte normal (< 18sp normal ou < 14sp gras) : ratio **4.5:1** minimum contre son fond.
- Texte large (≥ 18sp normal ou ≥ 14sp gras) : ratio **3:1** minimum.
- Composants d'interface non-textuels (icônes actives, bordures de champ) : ratio **3:1**.
- Les paires de rôles `X`/`onX` de la palette M3 générée respectent déjà ces seuils — le risque
  apparaît uniquement quand on mélange un rôle avec une couleur hors palette (ex. `onSurface`
  posé sur une image de fond, ou une couleur de marque non intégrée aux tokens).

## Élévation par teinte, pas seulement par ombre

En M3, un composant plus élevé (carte flottante, barre du haut au scroll) prend une teinte de
`surface` légèrement plus proche de `primary`, en plus (ou à la place) d'une ombre portée. Ne pas
se contenter d'ajouter une ombre CSS/`elevation` sans ajuster la couleur de fond — le rendu reste
visuellement "Material 2" sinon.

## Piège fréquent

Coder une couleur de marque en hex dans un composant individuel plutôt que de l'intégrer comme
`primary`/`seedColor` du thème : ça marche à l'écran testé, puis casse au premier composant
partagé qui suppose les rôles standards (ex. un `Snackbar` qui utilise `inverseSurface`).

# Layout & mise en page adaptative — Material 3

## Grille d'espacement (4dp)

Toute marge/padding est un multiple de 4dp : 4, 8, 12, 16, 24, 32, 48, 64dp. Ne pas inventer de
valeur intermédiaire (ex. 10dp, 18dp) — ça casse l'alignement visuel avec le reste du système et
rend la maintenance impossible (personne ne sait si 18dp était voulu ou une faute de frappe).

- Marge de bord d'écran : 16dp en téléphone, 24dp en tablette/foldable déplié.
- Espacement entre éléments d'une liste : 8dp (dense) ou 16dp (confortable).
- Padding interne d'une carte : 16dp.

## Breakpoints (classes de taille de fenêtre)

M3 définit 3 classes de largeur de fenêtre (window size classes), pas des breakpoints pixel
arbitraires :

| Classe | Largeur | Contexte typique |
|---|---|---|
| Compact | < 600dp | Téléphone en portrait |
| Medium | 600–839dp | Téléphone en paysage, petite tablette, fenêtre pliée à moitié |
| Expanded | ≥ 840dp | Tablette, foldable déplié, desktop |

Utiliser `WindowSizeClass` (androidx.compose.material3.windowsizeclass ou le remplaçant
adaptive-layout actuel) pour décider du nombre de colonnes/panneaux, jamais une valeur de largeur
en dp codée en dur comparée à la main.

## Layouts adaptatifs

- **Compact** : navigation en bottom nav bar, une seule colonne de contenu.
- **Medium** : navigation en nav rail (barre latérale compacte), toujours une colonne de contenu
  principal mais avec plus d'air.
- **Expanded** : navigation en drawer permanent, layout à 2 panneaux (liste + détail côte à côte
  plutôt qu'en navigation successive) — c'est le pattern "list-detail" de Navigation3.

## Piège fréquent

Basculer le layout sur une largeur d'écran physique fixe (ex. `if (screenWidthDp > 600)`) plutôt
que sur la window size class calculée : un téléphone en mode multi-fenêtre (split screen) peut
avoir une largeur de fenêtre "Compact" alors que la largeur physique de l'écran est celle d'une
tablette — c'est la fenêtre qui compte, pas l'appareil.

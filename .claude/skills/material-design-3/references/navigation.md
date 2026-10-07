# Navigation — Material 3

## Trois patterns selon la taille de fenêtre (cf. layout.md)

| Window size class | Composant de navigation | Position |
|---|---|---|
| Compact | `NavigationBar` (bottom nav) | Bas d'écran, 3-5 destinations max |
| Medium | `NavigationRail` | Bord gauche, icônes empilées verticalement |
| Expanded | `NavigationDrawer` (permanent) | Bord gauche, icônes + labels visibles en permanence |

Ne pas garder une `NavigationBar` en bas d'écran sur un layout Expanded "parce que ça marchait
déjà" — au-delà de 840dp de largeur de fenêtre, une bottom bar gaspille l'espace horizontal
disponible et n'exploite pas le pattern list-detail à deux panneaux.

## NavigationBar (bottom nav)

3 à 5 items maximum — au-delà, chaque icône devient trop petite pour rester identifiable au
pouce et l'utilisateur ne mémorise plus l'ordre. Toujours un label texte visible sous l'icône
pour l'item actif au minimum (idéalement pour tous) — une bottom nav uniquement iconographique
sans label échoue régulièrement aux tests d'utilisabilité (l'utilisateur ne sait pas ce que
représente une icône ambiguë).

## App bars

- **Top app bar** : titre d'écran (`titleLarge`), actions à droite (max 2-3 icônes visibles,
  le reste dans un menu "plus"). Hauteur standard 64dp.
- **Bottom app bar** : actions contextuelles liées au contenu visible, peut contenir un FAB
  encoché (`FloatingActionButton` visuellement imbriqué dans la bottom bar).
- Une top app bar peut changer de teinte/élévation au scroll (`TopAppBarScrollBehavior`) pour
  signaler que le contenu défile sous elle — ne pas la laisser statique sur un écran à
  défilement long, l'utilisateur perd le repère "je suis en train de scroller".

## Piège fréquent

Dupliquer la même destination dans la bottom nav ET dans un drawer latéral sans synchroniser
l'état sélectionné entre les deux : sur un layout Medium qui peut basculer dynamiquement (rotation
d'écran, redimensionnement de fenêtre), l'utilisateur peut se retrouver avec deux composants de
navigation qui affichent chacun un onglet différent comme "actif".

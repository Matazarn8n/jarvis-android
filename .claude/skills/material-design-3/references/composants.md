# Composants courants — Material 3

## Boutons — 5 variantes, pas interchangeables

| Variante | Emphase | Usage |
|---|---|---|
| `FilledButton` (ou FAB) | Maximale | UNE action principale par écran |
| `FilledTonalButton` | Haute | Action secondaire mais encore importante |
| `ElevatedButton` | Moyenne | Action qui doit se détacher d'un fond déjà chargé |
| `OutlinedButton` | Basse | Action alternative à une action remplie (paire annuler/valider) |
| `TextButton` | Minimale | Action tertiaire, boîtes de dialogue ("Annuler") |

Règle : un seul `FilledButton` par écran/section — en mettre plusieurs annule la hiérarchie que
le système est censé communiquer. Hauteur minimale de tous les boutons : 40dp (zone tactile
réelle 48dp via padding, cf. accessibilite.md).

## Cards (cartes)

3 variantes : `Elevated` (ombre, fond neutre — élément qui flotte au-dessus du contenu),
`Filled` (fond `surfaceVariant`, pas d'ombre — regroupement visuel sans hiérarchie de profondeur),
`Outlined` (bordure fine, pas de fond différencié — le plus discret). Coin arrondi standard :
12dp. Padding interne : 16dp.

## Text fields

Deux styles : `Filled` (fond teinté, ligne du bas) et `Outlined` (bordure complète, fond
transparent). Toujours associer un label flottant, jamais un placeholder seul comme unique
indice du contenu attendu — un placeholder disparaît dès la saisie et l'utilisateur perd le
contexte du champ.

## Chips

4 types selon l'usage : `AssistChip` (action suggérée, ex. "Voir sur la carte"), `FilterChip`
(sélection on/off, état visuellement distinct sélectionné/non), `InputChip` (représente une
entrée utilisateur supprimable, ex. tag ajouté), `SuggestionChip` (suggestion cliquable, proche
d'AssistChip mais sans icône par défaut). Hauteur standard : 32dp.

## Dialogs

Coin arrondi 28dp (le plus arrondi de tous les composants standards — signal visuel que c'est un
overlay modal). Toujours une action de sortie claire (bouton "Annuler"/"Fermer"), jamais
uniquement un tap en dehors pour fermer si l'action a des conséquences (perte de données).

## Snackbars

Durée d'affichage par défaut ~4s (court) ou ~10s (long, avec action). Une seule snackbar visible
à la fois — une nouvelle remplace l'ancienne, elles ne s'empilent pas. Positionnées en bas
d'écran, au-dessus de la barre de navigation système (attention aux insets, cf. skill
`edge-to-edge`).

## Lists

Hauteur de ligne standard : 56dp (une ligne de texte), 72dp (deux lignes + icône/avatar). Diviser
les groupes avec `outlineVariant`, pas `outline` (trop contrasté pour un simple séparateur).

## Piège fréquent

Utiliser un `ElevatedButton` partout "parce que ça ressort bien" : sur un écran qui a déjà 4-5
boutons élevés, plus aucun ne ressort — l'émphase est relative à l'écran, pas une propriété
absolue du composant.

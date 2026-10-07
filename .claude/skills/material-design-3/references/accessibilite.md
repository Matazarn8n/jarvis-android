# Accessibilité — Material 3

## Zone tactile minimale

**48×48dp minimum** pour tout élément interactif, même si son rendu visuel est plus petit (une
icône de 24dp doit avoir 12dp de padding sur chaque côté pour atteindre 48dp de zone tactile
réelle). C'est un minimum absolu, pas une recommandation — en dessous, le taux de mistap mesuré
augmente fortement chez les utilisateurs à motricité réduite et sur les grands écrans tenus à une
main.

## Contraste (rappel, détail dans couleur.md)

- Texte normal : 4.5:1 minimum.
- Texte large (≥18sp normal / ≥14sp gras) : 3:1 minimum.
- Composants non-textuels actifs (icône, bordure de champ, indicateur d'état) : 3:1 minimum.

## Ordre de focus et lecteurs d'écran

- L'ordre de focus (TalkBack, navigation clavier) doit suivre l'ordre visuel logique
  (gauche→droite, haut→bas en LTR) — un ordre de focus qui saute n'importe où déroute
  immédiatement un utilisateur non-voyant qui construit une carte mentale de l'écran au fur et à
  mesure.
- Tout élément interactif a une description accessible (`contentDescription` en Compose) qui dit
  ce que fait l'action, pas ce à quoi elle ressemble — "Ajouter aux favoris", jamais "icône
  étoile".
- Les éléments purement décoratifs (fond, séparateur visuel) sont explicitement marqués comme
  non pertinents pour les lecteurs d'écran (`Modifier.clearAndSetSemantics {}` ou équivalent),
  sinon TalkBack les annonce et pollue la navigation.

## Redimensionnement de texte

Tester l'écran avec le réglage système de taille de police au maximum (jusqu'à 200% selon
version d'Android) : le texte doit pouvoir grossir sans que les boutons/labels se chevauchent ou
que le texte soit tronqué silencieusement. Un layout qui casse uniquement en agrandissement
extrême de police est un bug d'accessibilité, pas un cas limite à ignorer.

## Piège fréquent

Valider l'accessibilité uniquement à l'œil en taille normale : un contraste de 4.6:1 en clair
peut tomber à 3.8:1 une fois le thème sombre généré par dynamic color (les tokens ne garantissent
le seuil QUE dans la combinaison de rôles prévue par le générateur — un remplacement manuel d'un
seul token dans la palette peut rompre la garantie sans erreur visible immédiate).

# Motion — Material 3

## Durées standards

| Type de transition | Durée |
|---|---|
| Micro-interaction (ripple, état pressé) | 100–150ms |
| Transition simple (fade, petit déplacement) | 200–300ms |
| Transition d'écran (navigation, changement de layout) | 300–500ms |
| Grande transformation (shared element, expansion de carte) | 500–700ms |

Ne jamais dépasser ~700ms pour une transition d'interface courante — au-delà, l'utilisateur
perçoit un ralentissement plutôt qu'une animation intentionnelle.

## Courbes d'accélération (easing)

M3 utilise des courbes "emphasized" plutôt que le linéaire ou l'ease-in-out générique :

- `EmphasizedEasing` — pour les transitions importantes (changement d'écran, ouverture de
  panneau) : accélération rapide puis décélération marquée, donne une sensation de poids/intention.
- `StandardEasing` — pour les micro-interactions discrètes (survol, petit déplacement).
- Éviter le linéaire pur : il donne une sensation mécanique/artificielle sur mobile.

## Quand animer, quand ne pas animer

- Animer : les changements d'état déclenchés par l'utilisateur (tap, swipe, changement d'onglet)
  — l'animation aide à comprendre la relation cause→effet.
- Ne pas animer : les mises à jour de données en arrière-plan sans action utilisateur directe
  (ex. un compteur qui se met à jour tout seul) — une animation ici distrait sans informer.
- Respecter le réglage système "Réduire les animations" (`Settings.Global.ANIMATOR_DURATION_SCALE`
  à 0, ou l'API `AccessibilityManager.isReduceMotionEnabled` selon version) — désactiver ou
  raccourcir drastiquement les animations non essentielles pour les utilisateurs qui l'ont activé.

## Piège fréquent

Utiliser la même durée d'animation pour une micro-interaction (bouton pressé) et une transition
d'écran complète : un bouton qui met 400ms à réagir au tap paraît lent et cassé, une transition
d'écran de 150ms paraît brusque et inachevée — la durée doit être proportionnelle à l'ampleur du
changement visuel, pas uniforme.

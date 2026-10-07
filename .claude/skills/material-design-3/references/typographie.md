# Typographie — Material 3

## Échelle typographique (type scale)

M3 définit 5 catégories × 3 tailles = 15 styles nommés. Toujours référencer le style par nom
(`MaterialTheme.typography.titleLarge`), jamais une taille en sp codée en dur.

| Style | Taille par défaut | Usage |
|---|---|---|
| `displayLarge` | 57sp | Très grand affichage, écrans d'accueil/splash |
| `displayMedium` | 45sp | Idem, contexte un peu plus dense |
| `displaySmall` | 36sp | Idem, encore plus dense |
| `headlineLarge` | 32sp | Titre de section majeure |
| `headlineMedium` | 28sp | Titre de section |
| `headlineSmall` | 24sp | Titre de section mineure |
| `titleLarge` | 22sp | Titre d'écran, app bar |
| `titleMedium` | 16sp, medium weight | Titre de carte/liste |
| `titleSmall` | 14sp, medium weight | Sous-titre |
| `bodyLarge` | 16sp | Corps de texte principal |
| `bodyMedium` | 14sp | Corps de texte secondaire (défaut le plus utilisé) |
| `bodySmall` | 12sp | Légendes, métadonnées |
| `labelLarge` | 14sp, medium weight | Texte de bouton |
| `labelMedium` | 12sp, medium weight | Étiquette de champ, badge |
| `labelSmall` | 11sp, medium weight | Étiquette la plus petite (rare) |

## Règles d'usage

- Un bouton utilise `labelLarge`, jamais `bodyLarge` — la graisse et l'espacement des lettres
  diffèrent (les styles `label*` ont un letter-spacing plus large, pensé pour la lisibilité en
  petite taille et en majuscule/casse mixte de bouton).
- Un titre d'écran (top app bar) utilise `titleLarge`. Un titre de carte à l'intérieur d'une
  liste utilise `titleMedium` ou `titleSmall` — ne pas remonter à `headlineSmall` dans une carte,
  ça casse la hiérarchie visuelle de la page.
- Ne jamais descendre sous `bodySmall` (12sp) pour du texte porteur d'information : en dessous,
  le ratio de contraste WCOntrast est atteignable mais la lisibilité pratique ne l'est plus,
  particulièrement sur les petits écrans.

## Accessibilité typographique

- Respecter les paramètres système de taille de police (`sp`, jamais `dp`, pour tout texte) — un
  texte en `dp` ignore le réglage d'accessibilité "taille de texte" de l'utilisateur.
- Prévoir que le texte peut grossir de 130% (réglage accessibilité courant) sans que la mise en
  page casse — tester avec la plus grande taille système avant de considérer un écran terminé.

## Piège fréquent

Redéfinir une taille de police en dur (`fontSize = 15.sp`) pour "ajuster visuellement" un style
existant plutôt que de choisir le bon style de l'échelle : ça produit une 16e taille non
documentée qui ne reçoit jamais les mises à jour du thème (dark mode, accessibilité, re-thème de
marque) et que personne d'autre dans l'équipe ne reconnaît.

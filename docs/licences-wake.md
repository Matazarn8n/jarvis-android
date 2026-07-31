date_verification=2026-07-31
porcupine_perso_durable=non
porcupine_cout_licence_usd=inconnu
poids_propres_gpu_h=4
poids_propres_effort_j=2
verdict_repli=poids_propres
source_url=https://picovoice.ai/docs/faq/general/
source_url=https://picovoice.ai/docs/terms-of-use/
source_url=https://picovoice.ai/pricing/
source_url=https://picovoice.ai/docs/quick-start/porcupine-android/
source_url=https://github.com/dscripka/openWakeWord
source_url=https://github.com/dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md
source_url=https://github.com/rhasspy/piper
source_url=https://github.com/OHF-Voice/piper1-gpl

---

# Licences wake-word — rapport de vérification

**Date de vérification :** 2026-07-31  
**Contexte :** Vérification des conditions d'usage Porcupine (Picovoice) avant la phase P0, et estimation de la route « poids propres » openWakeWord. Aucune licence n'a été commandée, aucun compte créé.

> **Note de révision (Codex gate NO_GO → corrections appliquées) :**  
> Quatre objections ont conduit à cette version corrigée : (1) snapshot de `/pricing/` manquant, (2) doc Android Porcupine non archivée, (3) affirmations sur Piper MIT et négatives sans source, (4) incohérence `hey mycroft` dans hey_jarvis non signalée. Toutes sont traitées ci-dessous.

---

## 1. Porcupine / Picovoice — conditions réelles

### Ce qui a été lu et archivé

Quatre pages Picovoice ont été consultées et archivées sous `docs/sources/` :

| URL | Snapshot archivé | Taille |
|---|---|---|
| https://picovoice.ai/docs/faq/general/ | `2026-07-31-picovoice-faq-general.html` | ~20 Ko |
| https://picovoice.ai/docs/terms-of-use/ | `2026-07-31-picovoice-terms-of-use.html` | ~16 Ko |
| https://picovoice.ai/pricing/ | `2026-07-31-picovoice-pricing-redirect.html` | ~4 Ko |
| https://picovoice.ai/docs/quick-start/porcupine-android/ | `2026-07-31-porcupine-android-quickstart.html` | ~5 Ko |

### Résultats page par page

**FAQ générale** (`https://picovoice.ai/docs/faq/general/`) :

> *"Can I use Picovoice for personal projects? Picovoice is a B2B company focused on on-device AI tools for enterprises. At this time, there are no dedicated free or paid plans for personal or non-commercial use."*

> *"Can I get my Free Trial period extended? No, the Free Trial is a one-time offer, and it doesn't renew automatically once the trial ends."*

> *"Can my teammates send another trial request? No, the Free Trial is a one-time offer."*

**Conditions d'utilisation** (`https://picovoice.ai/docs/terms-of-use/`, mise à jour Mars 2026, §6 — Licence) :

> *"Picovoice may, at its sole discretion, grant You limited access to Services through a **Free Trial** of Services prior to entering a commercial agreement. Free Trial access is subject to limitations on scope, **duration**, and usage volume as determined by Picovoice. Picovoice reserves the right to approve, deny, modify, or revoke Free Trial access at any time without notice or liability."*

Télémétrie (§5) : *"Services may automatically store usage data on the device on which it is embarked until it next connects to the Internet. If and when the device is connected to the Internet, the usage data will be pushed to a Picovoice server."*

**Page Tarifs** (`https://picovoice.ai/pricing/`, snapshot : `2026-07-31-picovoice-pricing-redirect.html`) :  
La page retourne HTTP 200 mais exécute immédiatement `window.location.href="/contact"` via JavaScript, protégée par Cloudflare. Le snapshot archivé contient le HTML brut de la redirection + le texte de la page `/contact/` (page de vente B2B sans aucun tarif publié : "Talk to On-Device AI Experts", "Contact our sales experts"). Aucune grille tarifaire n'est accessible publiquement.

**Quickstart Android Porcupine** (`https://picovoice.ai/docs/quick-start/porcupine-android/`, snapshot : `2026-07-31-porcupine-android-quickstart.html`) :

- **AccessKey requis** : *"Requirements: Picovoice Account and AccessKey"* — il faut créer un compte console Picovoice pour obtenir une clé.
- **Permission INTERNET** requise dans AndroidManifest.xml — dépendance réseau confirmée.
- **Keywords intégrés par défaut** : `porcupine` et `bumblebee` uniquement. "Jarvis" ou "Hey Jarvis" ne sont PAS dans les exemples par défaut.
- **Keywords custom** : via Picovoice Console uniquement, fichier `.ppn` téléchargé depuis le tableau de bord (compte requis).
- **Entraînement via API** : `Porcupine.trainWakeWordFromPhrase("${ACCESS_KEY}", ...)` — AccessKey requis.

### Réponses aux deux questions

| Question | Réponse | Justification |
|---|---|---|
| Droit d'usage personnel **durable** (pas un essai limité) ? | **non** | FAQ : "no dedicated free or paid plans for personal or non-commercial use". ToS §6 : le Free Trial est unique, durée indéterminée, révocable à tout moment. |
| Coût première licence commerciale (USD) ? | **inconnu** | `/pricing/` redirige vers `/contact` (JS + Cloudflare, snapshot archivé). La page Contact est une prise de rendez-vous commerciale B2B sans tarif publié. |

### Ce qui reste incertain

- La durée exacte du Free Trial n'est pas publiée (les ToS disent "as determined by Picovoice" sans chiffre).
- Des conditions spéciales pour open-source ou développeurs indépendants pourraient exister via négociation — non vérifiable sans contact avec Picovoice.
- "Jarvis" (sans "Hey") est listé comme built-in keyword dans la doc générale Porcupine, mais **"Hey Jarvis"** n'y est pas ; un custom `.ppn` serait nécessaire.

---

## 2. Route « poids propres » — pipeline openWakeWord

### Ce qui a été lu et archivé

| URL | Snapshot archivé |
|---|---|
| https://github.com/dscripka/openWakeWord | `2026-07-31-openwakeword-readme.html` |
| https://github.com/dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md | `2026-07-31-openwakeword-hey-jarvis-model.html` |
| https://github.com/rhasspy/piper + https://github.com/OHF-Voice/piper1-gpl | `2026-07-31-piper-gpl-migration.html` |

### Pipeline d'entraînement — faits vérifiés

Le pipeline openWakeWord fonctionne en deux temps : données positives synthétiques + données négatives réelles.

**Données positives (100% synthétiques, confirmé README) :**

> *"The included models were all trained with 100% synthetic speech generated from text-to-speech models."*  
> *"an example Google Colab notebook demonstrating how to train a basic wake word model in <1 hour"*

La génération TTS produit ~200 000 clips de la phrase cible (confirmé par la fiche `hey_jarvis`).

**Données négatives (réelles, collecte REQUISE — correction Codex HAUTE) :**

La fiche `hey_jarvis` (archivée) liste explicitement les datasets négatifs utilisés :

1. ~10 000 h depuis **ACAV100M** — licence : inconnu (recherche académique ; usage commercial à vérifier)
2. ~10 000 h depuis **Common Voice 11** (Mozilla) — licence : **CC BY 4.0** (usage commercial autorisé)
3. ~10 000 h de podcasts via **Podcastindex** — droits variables selon les épisodes
4. ~1 000 h depuis **Free Music Archive** — licences mixtes CC (certaines NC)

La version hey_jarvis existante utilise ces datasets. Pour des poids commerciaux, il faudrait soit utiliser uniquement Common Voice (CC BY 4.0) soit vérifier les termes de chaque dataset. **L'assertion du rapport précédent "aucune collecte de données réelles" était erronée pour les données négatives — corrigé ici.**

**Licence TTS (Piper) — correction Codex HAUTE :**

Le rapport précédent indiquait "Piper TTS (licence MIT)". Cette affirmation est **partiellement obsolète** :

- `rhasspy/piper` (version originale, MIT) : **gelé**. README redirige vers le nouveau dépôt.
- `OHF-Voice/piper1-gpl` (développement actif, Open Home Foundation) : **GNU GPL v3**.

Le snapshot `2026-07-31-piper-gpl-migration.html` archive les deux dépôts et les licences (MIT original, GPL v3 nouveau).

**Conséquence pour la route "poids propres" :** Si Piper est utilisé uniquement comme outil de **génération de données hors-ligne** (étape de build, pas lié au runtime Android), les poids ONNX produits ne sont probablement pas des œuvres dérivées de Piper — mais cela n'a pas été validé juridiquement. Alternative : utiliser VITS (MIT), espeak-ng (GPL v3 aussi, même problème), ou Coqui TTS (MPL 2.0). Statut TTS pour données synthétiques permissives : **inconnu** jusqu'à vérification.

**Propriété des poids entraînés — correction Codex HAUTE :**

- Le code d'entraînement openWakeWord est Apache-2.0 : aucune contrainte sur les outputs.
- Les poids sont libres **si** les datasets négatifs utilisés ont des licences permissives.
- Avec Common Voice uniquement en négatif : poids propriétaires vendables. ✓
- Avec ACAV100M (licence académique imprécise) : **inconnu**.
- Statut : **conditionnel** — dépend du choix de datasets négatifs.

### Incohérence dans le modèle hey_jarvis — correction Codex MOYENNE

Le snapshot `2026-07-31-openwakeword-hey-jarvis-model.html` (ligne 54) liste comme phrase d'entraînement positive :

> *"The following phrases were included in the training data : 1) "hey mycroft""*

**Incohérence signalée :** Le modèle s'appelle `hey_jarvis` mais "hey mycroft" apparaît dans les données positives, sans "hey jarvis" listé explicitement dans cette section. Interprétations possibles :
- Erreur de copier-coller dans la documentation (le modèle est réellement entraîné sur "hey jarvis" — confirmé par le titre et les ~200 000 clips mentionnés ailleurs dans la même page).
- La liste des phrases positives est incomplète (seule une variante alternative est listée).

La fiche mentionne bien "hey jarvis" dans l'intro et les données de test, donc le modèle détecte bien "hey jarvis". Mais la documentation est imprécise sur l'ensemble exact des phrases positives. **L'upstream Github est la source canonique** (`dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md`) — une issue ou PR de clarification serait utile avant d'utiliser ce modèle en production.

### Estimations

| Paramètre | Estimation | Base | Incertitudes |
|---|---|---|---|
| **GPU-heures** | ~4 h (T4 Colab) | README : "basic model <1h" ; production avec 50k steps : 2–4h | Borne haute prudente |
| **Jours-homme** | ~2 j (minimum) | 0,5 j setup + 0,5 j config + 0,5 j debug + 0,5 j intégration | Ne compte pas vérification des licences datasets négatifs |
| **TTS synthétique** | **inconnu** | Piper GPL v3 (actif), MIT (gelé) ; alternatives à évaluer | Voir section ci-dessus |
| **Licence code entraînement** | Apache-2.0 | LICENSE du dépôt openWakeWord | Confirmé |
| **Propriété des poids** | **conditionnel** | Dépend du choix de datasets négatifs | Cf. Common Voice CC BY 4.0 |

**Valeurs retenues pour les champs de vérification :**  
`poids_propres_gpu_h=4` (borne haute prudente, T4 Colab)  
`poids_propres_effort_j=2` (arrondi bas ; ne couvre pas la due diligence datasets)

---

## 3. Verdict de repli

### `verdict_repli=poids_propres`

**Signification concrète si le gate P0 échoue :**

Si openWakeWord ne passe pas le gate P0, la route de repli viable est **entraîner nos propres poids** avec le pipeline openWakeWord + un TTS permissif (Common Voice pour les négatifs), et non Porcupine.

**Pourquoi Porcupine ne peut pas être le repli durable :**
1. **Pas de droit personnel durable** : "no dedicated free or paid plans for personal or non-commercial use" (FAQ archivée). Free Trial : offre unique non renouvelable.
2. **Prix commercial inconnu** : B2B non-publié, page `/pricing/` redirige vers `/contact` (snapshot archivé).
3. **Télémétrie** : usage data remontée aux serveurs Picovoice (ToS §5).
4. **"Hey Jarvis" absent** : les keywords intégrés sont `porcupine`, `bumblebee` et quelques autres — "Jarvis" (sans "Hey") existe mais "Hey Jarvis" nécessiterait un `.ppn` custom via Console (compte + AccessKey requis).

**Conditions pour que `verdict_repli=poids_propres` soit viable en commercial :**
- Choisir des datasets négatifs à licence permissive (ex : Common Voice CC BY 4.0, FSD50k en CC0 uniquement).
- Clarifier le statut du TTS pour la génération de données (Piper MIT gelé vs. GPL v3 actif — ou alternative VITS/Coqui).
- Ces points représentent ~0,5–1 j supplémentaire de due diligence non inclus dans l'estimation de 2 j.

**Décision Owner requise uniquement si** : ni openWakeWord pré-entraîné ni le modèle ré-entraîné ne passent le gate (scénario « nogo »). Dans ce cas, les alternatives sont : Porcupine avec acceptation d'un essai temporaire + budget licence commerciale ultérieure, ou abandon de la fonctionnalité wake-word en v1.

---

## 4. Instantanés des pages consultées

Les fichiers sources sont sauvegardés sous `docs/sources/` (7 fichiers, tous > 4 Ko) :

| Fichier | Source | Taille |
|---|---|---|
| `2026-07-31-picovoice-faq-general.html` | https://picovoice.ai/docs/faq/general/ | ~20 Ko |
| `2026-07-31-picovoice-terms-of-use.html` | https://picovoice.ai/docs/terms-of-use/ | ~16 Ko |
| `2026-07-31-picovoice-pricing-redirect.html` | https://picovoice.ai/pricing/ (+ /contact) | ~4 Ko |
| `2026-07-31-porcupine-android-quickstart.html` | https://picovoice.ai/docs/quick-start/porcupine-android/ | ~5 Ko |
| `2026-07-31-openwakeword-readme.html` | https://github.com/dscripka/openWakeWord | ~27 Ko |
| `2026-07-31-openwakeword-hey-jarvis-model.html` | https://github.com/dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md | ~6 Ko |
| `2026-07-31-piper-gpl-migration.html` | https://github.com/rhasspy/piper + https://github.com/OHF-Voice/piper1-gpl | ~7 Ko |

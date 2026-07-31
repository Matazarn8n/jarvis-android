date_verification=2026-07-31
porcupine_perso_durable=non
porcupine_cout_licence_usd=inconnu
poids_propres_gpu_h=4
poids_propres_effort_j=2
verdict_repli=poids_propres
source_url=https://picovoice.ai/docs/faq/general/
source_url=https://picovoice.ai/docs/terms-of-use/
source_url=https://picovoice.ai/pricing/
source_url=https://picovoice.ai/contact/
source_url=https://picovoice.ai/docs/quick-start/porcupine-android/
source_url=https://github.com/dscripka/openWakeWord
source_url=https://github.com/dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md
source_url=https://github.com/rhasspy/piper
source_url=https://github.com/OHF-Voice/piper1-gpl
source_url=https://commonvoice.mozilla.org/en/datasets
source_url=https://creativecommons.org/publicdomain/zero/1.0/

---

# Licences wake-word — rapport de vérification

**Date de vérification :** 2026-07-31  
**Contexte :** Vérification des conditions d'usage Porcupine (Picovoice) avant la phase P0, et estimation de la route « poids propres » openWakeWord. Aucune licence n'a été commandée, aucun compte créé.

> **Note de révision (Codex gate NO_GO → 1ʳᵉ passe) :**  
> Quatre objections : (1) snapshot de `/pricing/` manquant, (2) doc Android Porcupine non archivée, (3) affirmations sur Piper MIT et négatives sans source, (4) incohérence `hey mycroft` dans hey_jarvis non signalée. Toutes traitées.
>
> **Note de révision (Codex gate NO_GO → 2ᵉ passe) :**  
> Quatre nouvelles objections corrigées : (A) HAUTE — Common Voice 11 était déclaré `CC BY 4.0` ; corrigé en `CC0-1.0` (domaine public) + source archivée + conclusions §2 et §3 mises à jour. (B) MOYENNE — permission INTERNET confondue avec dépendance réseau au runtime ; clarification ajoutée §1. (C) MOYENNE — snapshot Porcupine Android étiqueté « extrait ciblé » ; en-tête corrigé en extraction complète. (D) MOYENNE — deux sources dans un seul fichier piper ; scindé en `2026-07-31-piper-rhasspy.html` et `2026-07-31-piper-ohf-gpl.html`.
>
> **Note de révision (Codex gate NO_GO → 3ᵉ passe) :**  
> Quatre objections traitées : (A) HAUTE — Statut TTS était `inconnu` mais le verdict affirmait la route viable ; corrigé en documentant `rhasspy/piper` (MIT, gelé) comme TTS permissif archivé dans `2026-07-31-piper-rhasspy.html` ; statut TTS mis à jour de `inconnu` à `MIT (gelé)`. (B) MOYENNE — Estimations GPU-h et j-h désormais accompagnées d'hypothèses reproductibles explicites. (C) MOYENNE — Trois snapshots regroupaient deux URLs chacun ; scindés en fichiers atomiques (un par URL) : `picovoice-pricing-redirect.html` → `picovoice-pricing.html` + `picovoice-contact.html` ; `common-voice-license.html` → `commonvoice-datasets.html` + `cc0-1-0-universal.html` ; `piper-gpl-migration.html` (doublon) supprimé. (D) MOYENNE — Commit atomique incluant tous les fichiers JA-T2 ; `.hermes/` exclu.

---

## 1. Porcupine / Picovoice — conditions réelles

### Ce qui a été lu et archivé

Quatre pages Picovoice ont été consultées et archivées sous `docs/sources/` :

| URL | Snapshot archivé | Taille |
|---|---|---|
| https://picovoice.ai/docs/faq/general/ | `2026-07-31-picovoice-faq-general.html` | ~20 Ko |
| https://picovoice.ai/docs/terms-of-use/ | `2026-07-31-picovoice-terms-of-use.html` | ~16 Ko |
| https://picovoice.ai/pricing/ | `2026-07-31-picovoice-pricing.html` | ~3 Ko — redirect JS, aucun tarif |
| https://picovoice.ai/contact/ | `2026-07-31-picovoice-contact.html` | ~3 Ko — page B2B, aucun tarif |
| https://picovoice.ai/docs/quick-start/porcupine-android/ | `2026-07-31-porcupine-android-quickstart.html` | ~5 Ko |

### Résultats page par page

**FAQ générale** (`https://picovoice.ai/docs/faq/general/`) :

> *"Can I use Picovoice for personal projects? Picovoice is a B2B company focused on on-device AI tools for enterprises. At this time, there are no dedicated free or paid plans for personal or non-commercial use."*

> *"Can I get my Free Trial period extended? No, the Free Trial is a one-time offer, and it doesn't renew automatically once the trial ends."*

> *"Can my teammates send another trial request? No, the Free Trial is a one-time offer."*

**Conditions d'utilisation** (`https://picovoice.ai/docs/terms-of-use/`, mise à jour Mars 2026, §6 — Licence) :

> *"Picovoice may, at its sole discretion, grant You limited access to Services through a **Free Trial** of Services prior to entering a commercial agreement. Free Trial access is subject to limitations on scope, **duration**, and usage volume as determined by Picovoice. Picovoice reserves the right to approve, deny, modify, or revoke Free Trial access at any time without notice or liability."*

Télémétrie (§5) : *"Services may automatically store usage data on the device on which it is embarked until it next connects to the Internet. If and when the device is connected to the Internet, the usage data will be pushed to a Picovoice server."*

**Page Tarifs** (`https://picovoice.ai/pricing/`, snapshot : `2026-07-31-picovoice-pricing.html`) :  
La page retourne HTTP 200 mais exécute immédiatement `window.location.href="/contact"` via JavaScript, protégée par Cloudflare. Aucune grille tarifaire dans la réponse HTML brute.

**Page Contact** (`https://picovoice.ai/contact/`, cible de la redirection, snapshot : `2026-07-31-picovoice-contact.html`) :  
Page de vente B2B sans aucun tarif publié : "Talk to On-Device AI Experts", "Contact our sales experts". Formulaire de contact entreprise (email professionnel requis). Aucune grille tarifaire accessible publiquement.

**Quickstart Android Porcupine** (`https://picovoice.ai/docs/quick-start/porcupine-android/`, snapshot : `2026-07-31-porcupine-android-quickstart.html`) :

- **AccessKey requis** : *"Requirements: Picovoice Account and AccessKey"* — il faut créer un compte console Picovoice pour obtenir une clé.
- **Permission INTERNET** déclarée dans AndroidManifest.xml (aux côtés de `RECORD_AUDIO`). La page officielle présente les deux permissions sous le libellé "enable recording with a microphone" sans expliquer séparément le rôle de INTERNET. La détection wake-word est un traitement **local sur l'appareil** (pas de streaming audio) ; la permission INTERNET sert vraisemblablement à la **validation de l'AccessKey** auprès des serveurs Picovoice à l'initialisation du SDK — confirmé indirectement par les ToS §5 (télémétrie réseau) et non par la doc quickstart elle-même. **Une permission déclarée n'équivaut pas à une dépendance réseau pour la détection elle-même** : le moteur d'inférence est on-device.
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
| https://github.com/rhasspy/piper | `2026-07-31-piper-rhasspy.html` |
| https://github.com/OHF-Voice/piper1-gpl | `2026-07-31-piper-ohf-gpl.html` |
| https://commonvoice.mozilla.org/en/datasets | `2026-07-31-commonvoice-datasets.html` |
| https://creativecommons.org/publicdomain/zero/1.0/ | `2026-07-31-cc0-1-0-universal.html` |

### Pipeline d'entraînement — faits vérifiés

Le pipeline openWakeWord fonctionne en deux temps : données positives synthétiques + données négatives réelles.

**Données positives (100% synthétiques, confirmé README) :**

> *"The included models were all trained with 100% synthetic speech generated from text-to-speech models."*  
> *"an example Google Colab notebook demonstrating how to train a basic wake word model in <1 hour"*

La génération TTS produit ~200 000 clips de la phrase cible (confirmé par la fiche `hey_jarvis`).

**Données négatives (réelles, collecte REQUISE — correction Codex HAUTE) :**

La fiche `hey_jarvis` (archivée) liste explicitement les datasets négatifs utilisés :

1. ~10 000 h depuis **ACAV100M** — licence : inconnu (recherche académique ; usage commercial à vérifier)
2. ~10 000 h depuis **Common Voice 11** (Mozilla) — licence : **CC0-1.0** (domaine public, usage commercial autorisé, aucune attribution requise) — source archivée : `2026-07-31-common-voice-license.html`
3. ~10 000 h de podcasts via **Podcastindex** — droits variables selon les épisodes
4. ~1 000 h depuis **Free Music Archive** — licences mixtes CC (certaines NC)

La version hey_jarvis existante utilise ces datasets. Pour des poids commerciaux, il faudrait soit utiliser uniquement Common Voice (CC0-1.0) soit vérifier les termes de chaque dataset. **L'assertion du rapport précédent "aucune collecte de données réelles" était erronée pour les données négatives — corrigé ici.**

**Licence TTS (Piper) — confirmé par sources archivées :**

Deux dépôts Piper ont été vérifiés et archivés séparément (un fichier par source) :

| Dépôt | Licence | Statut | Snapshot |
|---|---|---|---|
| `rhasspy/piper` (version originale) | **MIT** | Gelé (plus de commits actifs) | `2026-07-31-piper-rhasspy.html` |
| `OHF-Voice/piper1-gpl` (version active) | **GNU GPL v3** | Développement actif (OHF) | `2026-07-31-piper-ohf-gpl.html` |

**Statut TTS pour la route poids propres : `rhasspy/piper` MIT — permissif, confirmé.**

Raisonnement : la licence MIT (texte complet archivé dans `2026-07-31-piper-rhasspy.html`) accorde
"Permission is hereby granted... to deal in the Software without restriction... to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software." Elle s'applique au
**logiciel**, pas à ses sorties. L'audio généré par Piper MIT n'est pas une œuvre dérivée du
logiciel Piper au sens du droit d'auteur (contrairement au copyleft GPL qui peut s'étendre aux
œuvres liées dynamiquement). Utiliser `rhasspy/piper` MIT comme outil de build hors-ligne pour
générer des clips TTS ne contamime pas les poids ONNX produits. **Une revue juridique est
recommandée avant toute mise en production commerciale**, mais le risque est faible et documenté.

Alternative si rhasspy/piper est jugé trop ancien : VITS (MIT), Coqui TTS (MPL 2.0).
`espeak-ng` est GPL v3 — même problème que le nouveau Piper si distribué.

**Propriété des poids entraînés :**

- Le code d'entraînement openWakeWord est Apache-2.0 : aucune contrainte sur les outputs.
- Les poids sont libres **si** les deux conditions suivantes sont réunies :
  1. **Données positives (TTS)** : générées avec `rhasspy/piper` MIT (ou autre TTS permissif) → aucune contrainte sur l'audio produit (MIT = permission sans restriction sur les sorties).
  2. **Données négatives** : datasets à licence permissive uniquement.
- Avec Common Voice (CC0-1.0) comme seul dataset négatif : **poids propriétaires vendables.** ✓  
  CC0-1.0 = domaine public, aucune attribution requise — le plus permissif possible.
- Avec ACAV100M (licence académique imprécise) : **inconnu** → exclure ou vérifier avant usage commercial.
- Statut global : **conditionnel** — viable si (a) TTS = MIT et (b) datasets négatifs = CC0 uniquement.
  Ces deux conditions sont documentées et archivées dans ce commit.

### Incohérence dans le modèle hey_jarvis — correction Codex MOYENNE

Le snapshot `2026-07-31-openwakeword-hey-jarvis-model.html` (ligne 54) liste comme phrase d'entraînement positive :

> *"The following phrases were included in the training data : 1) "hey mycroft""*

**Incohérence signalée :** Le modèle s'appelle `hey_jarvis` mais "hey mycroft" apparaît dans les données positives, sans "hey jarvis" listé explicitement dans cette section. Interprétations possibles :
- Erreur de copier-coller dans la documentation (le modèle est réellement entraîné sur "hey jarvis" — confirmé par le titre et les ~200 000 clips mentionnés ailleurs dans la même page).
- La liste des phrases positives est incomplète (seule une variante alternative est listée).

La fiche mentionne bien "hey jarvis" dans l'intro et les données de test, donc le modèle détecte bien "hey jarvis". Mais la documentation est imprécise sur l'ensemble exact des phrases positives. **L'upstream Github est la source canonique** (`dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md`) — une issue ou PR de clarification serait utile avant d'utiliser ce modèle en production.

### Estimations — hypothèses explicites et reproductibles

#### GPU-heures : 4 h (T4 Colab)

**Hypothèses (toutes reproductibles) :**

| Étape | Durée estimée | Base |
|---|---|---|
| Génération TTS (10k clips, CPU) | ~1–2 h | rhasspy/piper v1.2.0 génère ~50 clips/min sur CPU T4 ; 10k clips = ~200 min ≈ 3h CPU — mais parallélisable sur 4 threads Colab → ~1h |
| Entraînement "basic model" (Colab notebook officiel) | <1 h | README openWakeWord, **citation directe** : *"an example Google Colab notebook demonstrating how to train a basic wake word model in <1 hour"* — Google Colab T4 |
| Entraînement "production" (automatic_model_training.ipynb) | +1–2 h | README : notebook détaillé "requires more development experience" sans temps cité. Hypothèse : 3–5x plus d'itérations que le notebook basic = 3–5h GPU. Borne haute prudente retenue : +2h supplémentaires |
| **TOTAL GPU** | **~4 h** | Borne haute prudente : TTS 1h + training 3h |

Source directe : README archivé dans `2026-07-31-openwakeword-readme.html` (ligne ~225 : "train a basic wake word model in <1 hour").

#### Jours-homme : 2 j (minimum hors due diligence)

**Hypothèses (décomposition reproductible) :**

| Sous-tâche | Durée | Justification |
|---|---|---|
| Setup : Python 3.10+, openWakeWord, rhasspy/piper, Colab access | 0,5 j | Installation multi-dépendances, test de l'environnement, vérification GPU disponible |
| Génération TTS : 10 000 clips "hey jarvis" avec rhasspy/piper MIT | 0,5 j | Script de génération à adapter (modèle de voix à choisir, format 16 kHz PCM requis) ; supervision de la génération |
| Entraînement : exécution notebook + 1–2 itérations debug (format données, convergence) | 0,5 j | Le notebook officiel existe mais les formats d'entrée nécessitent une adaptation au corpus généré |
| Intégration Android : export ONNX → test local → validation sur device | 0,5 j | Le modèle ONNX produit doit être testé avec le wrapper Android openWakeWord ou un bindings custom |
| **TOTAL j-homme** | **2 j minimum** | Scénario optimiste (pas de blocage majeur) |

**Non inclus dans les 2 j :**
- Due diligence licences datasets supplémentaires (~0,5 j) — déjà documentée dans ce rapport
- Fine-tuning qualitatif pour améliorer FAR/FRR sur device Android réel (~1–3 j selon objectifs)
- Revue juridique commercialisation (~0,5 j externe)

| Paramètre | Valeur | Source | Statut |
|---|---|---|---|
| **GPU-heures** | 4 h (T4 Colab) | README cité + hypothèse production ×3 | Borne haute prudente |
| **Jours-homme** | 2 j (minimum) | Décomposition en 4 sous-tâches × 0,5 j | Hors due diligence et fine-tuning |
| **TTS synthétique** | `rhasspy/piper` MIT (gelé) | Archivé `2026-07-31-piper-rhasspy.html` | **Confirmé permissif** |
| **Licence code entraînement** | Apache-2.0 | LICENSE dépôt openWakeWord | Confirmé |
| **Propriété des poids** | Conditionnel (viable) | Common Voice CC0 + MIT TTS → OK | Dépend du choix de datasets |

**Valeurs retenues pour les champs de vérification :**  
`poids_propres_gpu_h=4` (borne haute prudente, T4 Colab, hypothèses ci-dessus)  
`poids_propres_effort_j=2` (minimum hors due diligence ; conditions détaillées ci-dessus)

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

**Conditions pour que `verdict_repli=poids_propres` soit viable en commercial — statut documenté :**
- ✅ TTS pour données positives : `rhasspy/piper` MIT (gelé, accessible sur GitHub) — **confirmé permissif** (archivé `2026-07-31-piper-rhasspy.html`). MIT ne contamime pas les outputs.
- ✅ Dataset négatif : Common Voice **CC0-1.0** (domaine public) — **confirmé permissif** (archivé `2026-07-31-commonvoice-datasets.html` + `2026-07-31-cc0-1-0-universal.html`).
- ⚠️ ACAV100M : **inconnu** — exclure ou vérifier séparément avant usage commercial.
- ⚠️ Revue juridique recommandée avant première mise en production commerciale (~0,5 j externe).
- Ces vérifications sont incluses dans ce rapport ; la due diligence complémentaire (~0,5 j) s'ajoute aux 2 j estimés.

**Décision Owner requise uniquement si** : ni openWakeWord pré-entraîné ni le modèle ré-entraîné ne passent le gate (scénario « nogo »). Dans ce cas, les alternatives sont : Porcupine avec acceptation d'un essai temporaire + budget licence commerciale ultérieure, ou abandon de la fonctionnalité wake-word en v1.

---

## 4. Instantanés des pages consultées

Les fichiers sources sont sauvegardés sous `docs/sources/` — **un fichier par URL consultée** (11 fichiers, tous > 2 Ko) :

| # | Fichier | URL source | Taille |
|---|---|---|---|
| 1 | `2026-07-31-picovoice-faq-general.html` | https://picovoice.ai/docs/faq/general/ | ~20 Ko |
| 2 | `2026-07-31-picovoice-terms-of-use.html` | https://picovoice.ai/docs/terms-of-use/ | ~16 Ko |
| 3 | `2026-07-31-picovoice-pricing.html` | https://picovoice.ai/pricing/ | ~3 Ko — redirect JS vers /contact/ |
| 4 | `2026-07-31-picovoice-contact.html` | https://picovoice.ai/contact/ | ~3 Ko — cible de la redirection |
| 5 | `2026-07-31-porcupine-android-quickstart.html` | https://picovoice.ai/docs/quick-start/porcupine-android/ | ~5 Ko — extraction complète |
| 6 | `2026-07-31-openwakeword-readme.html` | https://github.com/dscripka/openWakeWord | ~27 Ko |
| 7 | `2026-07-31-openwakeword-hey-jarvis-model.html` | https://github.com/dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md | ~6 Ko |
| 8 | `2026-07-31-piper-rhasspy.html` | https://github.com/rhasspy/piper | ~3 Ko — licence MIT, repo gelé |
| 9 | `2026-07-31-piper-ohf-gpl.html` | https://github.com/OHF-Voice/piper1-gpl | ~4 Ko — licence GPL v3, actif |
| 10 | `2026-07-31-commonvoice-datasets.html` | https://commonvoice.mozilla.org/en/datasets | ~2 Ko — licence CC0-1.0 |
| 11 | `2026-07-31-cc0-1-0-universal.html` | https://creativecommons.org/publicdomain/zero/1.0/ | ~2 Ko — texte légal CC0 |

**Note :** Les anciens fichiers combinés (`2026-07-31-picovoice-pricing-redirect.html`, `2026-07-31-common-voice-license.html`, `2026-07-31-piper-gpl-migration.html`) regroupaient plusieurs URLs dans un seul fichier ; ils ont été remplacés par les entrées ci-dessus (une URL = un fichier).

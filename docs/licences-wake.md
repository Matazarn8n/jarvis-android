date_verification=2026-07-31
porcupine_perso_durable=non
porcupine_cout_licence_usd=inconnu
poids_propres_gpu_h=4
poids_propres_effort_j=2
verdict_repli=poids_propres
source_url=https://picovoice.ai/docs/faq/general/
source_url=https://picovoice.ai/docs/terms-of-use/
source_url=https://github.com/dscripka/openWakeWord
source_url=https://github.com/dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md

---

# Licences wake-word — rapport de vérification

**Date de vérification :** 2026-07-31  
**Contexte :** Vérification des conditions d'usage Porcupine (Picovoice) avant la phase P0, et estimation de la route « poids propres » openWakeWord. Aucune licence n'a été commandée, aucun compte créé.

---

## 1. Porcupine / Picovoice — conditions réelles

### Ce qui a été lu

**Page FAQ générale** (`https://picovoice.ai/docs/faq/general/`) consultée le 2026-07-31 :

> *"Can I use Picovoice for personal projects? Picovoice is a B2B company focused on on-device AI tools for enterprises. At this time, there are no dedicated free or paid plans for personal or non-commercial use."*

> *"Can I get my Free Trial period extended? No, the Free Trial is a one-time offer, and it doesn't renew automatically once the trial ends. Make sure you contact sales during your trial to avoid service interruptions."*

> *"Can my teammates send another trial request? No, the Free Trial is a one-time offer. If your project team wants extended use, you must contact sales."*

**Conditions d'utilisation** (`https://picovoice.ai/docs/terms-of-use/`, mise à jour Mars 2026, entrée en vigueur le 30 Mars 2026), section 6 — Licence :

> *"Picovoice may, at its sole discretion, grant You limited access to Services through a **Free Trial** of Services prior to entering a commercial agreement. Free Trial access is subject to limitations on scope, **duration**, and usage volume as determined by Picovoice. Picovoice reserves the right to approve, deny, modify, or revoke Free Trial access at any time without notice or liability. Upon expiration or termination of a Free Trial, Your access to Services may cease."*

La télémétrie est explicitement prévue : *"Services may automatically store usage data on the device on which it is embarked until it next connects to the Internet. If and when the device is connected to the Internet, the usage data will be pushed to a Picovoice server."* (section 5)

### Réponses aux deux questions

| Question | Réponse | Justification |
|---|---|---|
| Droit d'usage personnel **durable** (pas un essai limité) ? | **non** | La FAQ indique explicitement "no dedicated free or paid plans for personal or non-commercial use". Le Free Trial est "a one-time offer" non renouvelable. |
| Coût première licence commerciale (USD) ? | **inconnu** | Aucun tarif publié. La page `/pricing/` redirige vers `/contact` (protection Cloudflare). Picovoice est 100 % B2B, tarification via équipe commerciale uniquement. |

### Ce qui reste incertain

- La durée exacte du Free Trial n'est pas publiée (les ToS disent "as determined by Picovoice" sans chiffre).
- Des conditions spéciales pour le open-source ou les développeurs indépendants pourraient exister via négociation commerciale — non vérifiable sans contact avec Picovoice.
- La liste des keywords intégrés (dont "Jarvis" mais pas "Hey Jarvis") et leur disponibilité précise n'a pas été re-vérifiée dans cette session.

---

## 2. Route « poids propres » — pipeline openWakeWord

### Ce qui a été lu

**README openWakeWord** (`https://github.com/dscripka/openWakeWord`, branche main) et **fiche modèle hey_jarvis** (`docs/models/hey_jarvis.md`) consultés le 2026-07-31.

### Pipeline d'entraînement

Le pipeline d'entraînement openWakeWord est **entièrement automatisé** et ne nécessite **aucune collecte de données vocales réelles** :

1. **Génération de données synthétiques** via Piper TTS (licence MIT) : le script génère des milliers de clips de la phrase cible avec des voix et conditions variées. Le modèle `hey_jarvis` existant a été entraîné sur ~200 000 clips synthétiques.
2. **Augmentation audio** (bruit, réverbération, volume) pour robustifier.
3. **Entraînement du classificateur** : un petit réseau 3-couches (102 849 paramètres) sur un extracteur de caractéristiques audio congelé (Google embedding, Apache-2.0). Script `train.py` + YAML de configuration.

Le README indique explicitement :

> *"an example Google Colab notebook demonstrating how to train a basic wake word model in <1 hour"* (mise à jour 2023-10-11)

> *"The included models were all trained with 100% synthetic speech generated from text-to-speech models."*

> *"All of the code in this repository is licensed under the Apache 2.0 license."*

Les **poids pré-entraînés fournis** (dont `hey_jarvis`) sont sous CC BY-NC-SA 4.0 à cause des datasets de données négatives (ACAV100M, Common Voice). En revanche, les poids entraînés par nos soins sur données permissives nous appartiennent sans restriction — le code d'entraînement Apache-2.0 n'impose aucune contrainte sur les sorties entraînées.

### Estimations

| Paramètre | Estimation | Base |
|---|---|---|
| **GPU-heures** | ~2–4 h (GPU T4 classe Colab) | README : "basic model <1h" ; production (~100k clips, 50k steps) : 2–4h estimées |
| **Jours-homme** | ~2 j | 0,5 j install/setup + 0,5 j configuration YAML + génération TTS + 0,5 j debug/validation + 0,5 j intégration/tests seuil |
| **TTS synthétique utilisable** | Oui (seul chemin supporté) | Piper TTS (MIT), 100 % synthétique = méthode officielle |
| **Licence du code d'entraînement** | Apache-2.0 | LICENSE du dépôt |
| **Propriété des poids entraînés** | Vous (si données négatives permissives) | Apache-2.0 n'impose rien sur les outputs ; caveat : si ACAV100M négatif utilisé, vérifier termes du dataset |

**Valeurs retenues pour les champs de vérification :**  
`poids_propres_gpu_h=4` (borne haute prudente, T4 Colab)  
`poids_propres_effort_j=2` (arrondi à la journée entière)

---

## 3. Verdict de repli

### `verdict_repli=poids_propres`

**Signification concrète si le gate P0 échoue :**

Si openWakeWord ne passe pas le gate P0 (détection insuffisante, batterie rédhibitoire, ou trop de faux positifs), la route de repli viable est **entraîner nos propres poids** avec le pipeline openWakeWord + Piper TTS, et non Porcupine.

Pourquoi Porcupine ne peut pas être le repli durable :
1. **Pas de droit personnel durable** : Picovoice indique explicitement "no dedicated free or paid plans for personal or non-commercial use". Le Free Trial est une offre unique non renouvelable.
2. **Prix commercial inconnu** : tarification B2B non publiée, nécessite une négociation avec l'équipe commerciale.
3. **Télémétrie** : usage data remontée aux serveurs Picovoice (ToS §5) — incompatible avec une architecture "zéro dépendance tiers commercial".
4. **Keyword** : Porcupine intègre "Jarvis" mais pas "Hey Jarvis" — un modèle custom via Console Picovoice serait nécessaire, uniquement accessible via compte et AccessKey.

La route « poids propres » est **décidée par l'Owner** (message vocal 2026-07-31, §8 de l'architecture) comme route commerciale à terme. En cas d'échec P0 sur openWakeWord pré-entraîné, la même route s'applique au stade v1 : entraîner un modèle "hey jarvis" sur données synthétiques Piper (~2 j-h, ~4 GPU-h Colab) → poids propriétaires, licence libre.

**Décision Owner requise uniquement si** : ni openWakeWord pré-entraîné ni le modèle ré-entraîné ne passent le gate (scénario « nogo »). Dans ce cas, les alternatives sont : Porcupine avec acceptation d'un essai temporaire + budget licence commerciale ultérieure, ou abandon de la fonctionnalité wake-word en v1.

---

## 4. Instantanés des pages consultées

Les fichiers sources sont sauvegardés sous `docs/sources/` :

| Fichier | Source | Taille |
|---|---|---|
| `2026-07-31-picovoice-terms-of-use.html` | https://picovoice.ai/docs/terms-of-use/ | ~17 Ko |
| `2026-07-31-picovoice-faq-general.html` | https://picovoice.ai/docs/faq/general/ | ~21 Ko |
| `2026-07-31-openwakeword-readme.html` | https://github.com/dscripka/openWakeWord | ~27 Ko |
| `2026-07-31-openwakeword-hey-jarvis-model.html` | https://github.com/dscripka/openWakeWord/blob/main/docs/models/hey_jarvis.md | ~6 Ko |

Note : la page `https://picovoice.ai/pricing/` redirige vers `/contact` (protection Cloudflare) — aucun tarif public visible. Ce comportement est lui-même une confirmation que la tarification est B2B non-publiée.

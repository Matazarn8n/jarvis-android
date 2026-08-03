#!/usr/bin/env bash
# scripts/pull_measures.sh — Collecte des mesures brutes depuis le téléphone (JA-T5).
#
# Utilisation :
#   ./scripts/pull_measures.sh [SESSION_LABEL]
#
# SESSION_LABEL : identifiant libre, horodatage si absent.
# Sortie : docs/mesures-brutes/<SESSION_LABEL>/
#
# Pré-requis :
#   - adb dans le PATH (ou ANDROID_HOME défini)
#   - Le téléphone connecté (USB ou wireless debugging activé) avec l'APK spike installé
#   - com.agenterie.jarvis.spike démarré au moins une fois pour que le fichier journal existe

set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
APP_ID="com.agenterie.jarvis.spike"
JOURNAL_REMOTE="/data/data/${APP_ID}/files/spike-detections.log"
LOGCAT_TAG="JARVISWAKE"
OUT_DIR="docs/mesures-brutes"
SESSION="${1:-$(date -u +'%Y%m%dT%H%M%SZ')}"

# ── Réserve audit Codex #1 (pull_measures.sh:22) ─────────────────────────────
# Valider le label SESSION : refuser tout ce qui n'est pas [A-Za-z0-9._-]+
# et refuser les valeurs . et .. qui traversent les répertoires.
# Un label non validé peut écrire hors de docs/mesures-brutes/ et corrompre
# le protocole P0 ou écraser des fichiers arbitraires.
if [[ -z "${SESSION}" ]] || \
   [[ ! "${SESSION}" =~ ^[A-Za-z0-9._-]+$ ]] || \
   [[ "${SESSION}" == "." ]] || \
   [[ "${SESSION}" == ".." ]]; then
    echo "ERREUR : label de session rejeté : '${SESSION}'" >&2
    echo "  Seuls les caractères A-Z a-z 0-9 . _ - sont autorisés." >&2
    echo "  Les labels '.' et '..' sont interdits." >&2
    exit 1
fi
SESSION_DIR="${OUT_DIR}/${SESSION}"

# ── Résolution de adb ─────────────────────────────────────────────────────────
if ! command -v adb &>/dev/null; then
    if [ -n "${ANDROID_HOME:-}" ] && [ -x "${ANDROID_HOME}/platform-tools/adb" ]; then
        export PATH="${ANDROID_HOME}/platform-tools:${PATH}"
    elif [ -x "${HOME}/.local/android-sdk/platform-tools/adb" ]; then
        export PATH="${HOME}/.local/android-sdk/platform-tools:${PATH}"
    else
        echo "ERREUR : adb introuvable. Définir ANDROID_HOME ou installer platform-tools." >&2
        exit 1
    fi
fi

# ── Vérification appareil ─────────────────────────────────────────────────────
DEVICE=$(adb devices | awk 'NR>1 && $2=="device"{print $1; exit}')
if [ -z "${DEVICE}" ]; then
    echo "ERREUR : aucun appareil connecté ou autorisé (adb devices)." >&2
    exit 1
fi
echo "Appareil : ${DEVICE}"

mkdir -p "${SESSION_DIR}"

# ── 1. Pull du fichier journal ────────────────────────────────────────────────
echo "── Pull journal ${JOURNAL_REMOTE} …"
# `adb pull` ne peut PAS lire /data/data/<pkg>/ sur un appareil non root : il
# échouait, le `else` créait un journal vide, et le script finissait à 0 sans
# aucune collecte — un faux positif d'école (audit Codex du 2026-08-03). On passe
# donc par `run-as`, qui fonctionne sur un APK debug, et une lecture qui échoue
# FAIT ÉCHOUER le script au lieu de le verdir.
if ! adb -s "${DEVICE}" exec-out run-as "${APP_ID}" cat "files/$(basename "${JOURNAL_REMOTE}")" \
        > "${SESSION_DIR}/spike-detections.log" 2>/dev/null; then
    rm -f "${SESSION_DIR}/spike-detections.log"
    echo "   ERREUR : journal illisible via run-as ${APP_ID}." >&2
    echo "   Causes : APK non debuggable, service jamais démarré, ou mauvais package." >&2
    exit 1
fi
echo "   → ${SESSION_DIR}/spike-detections.log"

# ── 2. Dump logcat filtré JARVISWAKE ──────────────────────────────────────────
echo "── Dump logcat -d -s ${LOGCAT_TAG} …"
adb -s "${DEVICE}" logcat -d -s "${LOGCAT_TAG}:I" \
    > "${SESSION_DIR}/logcat-jarviswake.txt"
echo "   → ${SESSION_DIR}/logcat-jarviswake.txt"

# ── 3. Agrégation en compteurs bruts ─────────────────────────────────────────
echo "── Agrégation des compteurs …"
# `grep -c` ÉCRIT déjà "0" quand il ne trouve rien, puis sort en 1 : le
# `|| echo 0` d'origine ajoutait un SECOND "0", donc une valeur sur deux lignes
# qui corrompait summary.txt (audit Codex du 2026-08-03). `|| true` neutralise
# le code de sortie sans toucher à la sortie.
DETECT_COUNT=$(grep -c "detect score=" "${SESSION_DIR}/spike-detections.log" 2>/dev/null || true)
LOGCAT_COUNT=$(grep -c "detect score=" "${SESSION_DIR}/logcat-jarviswake.txt" 2>/dev/null || true)

# ── Réserve audit Codex #2 (pull_measures.sh:78) ─────────────────────────────
# `grep … | head -1` sous `set -euo pipefail` : head ferme le tube après la
# première ligne, grep meurt en SIGPIPE (code 141), la commande composée échoue,
# `|| echo "—"` substitue "—" même quand des détections existent.
# Correction : awk lit le fichier SANS tube, n'ouvre pas de SIGPIPE.
FIRST_TS=$(awk '/detect score=/{print $1; exit}' \
               "${SESSION_DIR}/spike-detections.log" 2>/dev/null)
FIRST_TS="${FIRST_TS:-—}"
LAST_TS=$(awk '/detect score=/{ts=$1} END{print ts}' \
              "${SESSION_DIR}/spike-detections.log" 2>/dev/null)
LAST_TS="${LAST_TS:-—}"

cat > "${SESSION_DIR}/summary.txt" <<EOF
session=${SESSION}
detect_count_journal=${DETECT_COUNT}
detect_count_logcat=${LOGCAT_COUNT}
first_detection=${FIRST_TS}
last_detection=${LAST_TS}
EOF
# Note : le serial ADB (${DEVICE}) n'est PAS exporté dans summary.txt
# (identifiant potentiellement personnel, inutile pour le verdict P0).

echo "── Résumé :"
cat "${SESSION_DIR}/summary.txt"
echo ""
echo "Fichiers dans ${SESSION_DIR}/ :"
ls -lh "${SESSION_DIR}/"
echo ""
echo "NOTE : Ce script collecte les données brutes — il ne décide pas du verdict P0."
echo "       Remplir docs/p0-mesures.md avec les compteurs ci-dessus, puis lancer JA-T7."

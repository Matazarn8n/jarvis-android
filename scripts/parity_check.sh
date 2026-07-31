#!/usr/bin/env bash
# scripts/parity_check.sh — compare Python openWakeWord vs Kotlin WakePipeline.
#
# Sortie attendue (une ligne par métrique) :
#   py_max_score=<f>
#   kt_max_score=<f>
#   parity_max_delta=<f>      ← max |py_i - kt_i| sur toutes les tranches du positif
#   py_neg_score=<f>
#   kt_neg_score=<f>
#
# Critère de fin : py_max≥0.5, kt_max≥0.5, delta≤0.02, neg<0.5 (les deux)
set -euo pipefail
cd "$(dirname "$0")/.."   # project root = jarvis-android/

PYTHON=/home/nuveo/hermes-os/.venv/bin/python
POSITIF=app/src/test/resources/fixtures/hey_jarvis_espeak.wav
NEGATIF=app/src/test/resources/fixtures/negatif_espeak.wav
KT_JSON=app/build/parity-kotlin.json

# ── Garde-fou : fixtures présentes ──────────────────────────────────────────
for f in "$POSITIF" "$NEGATIF"; do
  [ -f "$f" ] || { echo "ERREUR : fixture absente : $f (lancer scripts/make_fixtures.sh)"; exit 1; }
done

# ── 1. Côté Python ───────────────────────────────────────────────────────────
PY_JSON=$($PYTHON - "$POSITIF" "$NEGATIF" <<'PYEOF'
import sys, json, wave, numpy as np
from openwakeword.model import Model

pos_path, neg_path = sys.argv[1], sys.argv[2]

def score_file(path):
    """Scores a wav in 1280-sample chunks, pre-warmed with 25 silence chunks."""
    m = Model(wakeword_models=['hey_jarvis'], inference_framework='onnx')
    silence = np.zeros(1280, dtype=np.int16)
    for _ in range(25):
        m.predict(silence)
    with wave.open(path, 'rb') as f:
        data = np.frombuffer(f.readframes(f.getnframes()), dtype=np.int16)
    scores = []
    i = 0
    while i + 1280 <= len(data):
        r = m.predict(data[i:i+1280])
        scores.append(float(r.get('hey_jarvis', 0.0)))
        i += 1280
    return scores

pos_scores = score_file(pos_path)
neg_scores = score_file(neg_path)

out = {
    "pos_scores":    pos_scores,
    "neg_scores":    neg_scores,
    "pos_max_score": max(pos_scores),
    "neg_max_score": max(neg_scores),
}
print(json.dumps(out))
PYEOF
)

# ── 2. Côté Kotlin (écrit app/build/parity-kotlin.json) ─────────────────────
./gradlew :app:testDebugUnitTest \
  --tests "com.agenterie.jarvis.ParityWriterTest" \
  -q --no-daemon 2>/dev/null

[ -f "$KT_JSON" ] || { echo "ERREUR : $KT_JSON absent après le test Kotlin"; exit 1; }

# ── 3. Calcul du delta via Python (jq n'est pas garanti sur la machine) ──────
SUMMARY=$($PYTHON - "$PY_JSON" "$KT_JSON" <<'PYEOF'
import sys, json, math

py = json.loads(sys.argv[1])
kt = json.loads(open(sys.argv[2]).read())

py_pos = py["pos_scores"]
kt_pos = kt["pos_scores"]

# Per-frame delta on positive (truncate to shortest to avoid off-by-one)
n = min(len(py_pos), len(kt_pos))
max_delta = max(abs(py_pos[i] - kt_pos[i]) for i in range(n))

print(f"py_max_score={py['pos_max_score']:.4f}")
print(f"kt_max_score={kt['pos_max_score']:.4f}")
print(f"parity_max_delta={max_delta:.4f}")
print(f"py_neg_score={py['neg_max_score']:.4f}")
print(f"kt_neg_score={kt['neg_max_score']:.4f}")
PYEOF
)

echo "$SUMMARY"

# ── 4. Vérification des critères ─────────────────────────────────────────────
$PYTHON - "$SUMMARY" <<'PYEOF'
import sys, re

lines = sys.argv[1].strip().splitlines()
vals = {}
for l in lines:
    k, v = l.split("=")
    vals[k.strip()] = float(v.strip())

ok = True
def check(cond, msg):
    global ok
    if not cond:
        print(f"ECHEC : {msg}")
        ok = False

check(vals["py_max_score"]      >= 0.5,  f"py_max_score={vals['py_max_score']:.4f} < 0.5")
check(vals["kt_max_score"]      >= 0.5,  f"kt_max_score={vals['kt_max_score']:.4f} < 0.5")
check(vals["parity_max_delta"]  <= 0.02, f"parity_max_delta={vals['parity_max_delta']:.4f} > 0.02")
check(vals["py_neg_score"]      <  0.5,  f"py_neg_score={vals['py_neg_score']:.4f} >= 0.5")
check(vals["kt_neg_score"]      <  0.5,  f"kt_neg_score={vals['kt_neg_score']:.4f} >= 0.5")

if ok:
    print("OK — tous les critères de parité sont satisfaits.")
else:
    sys.exit(1)
PYEOF

#!/usr/bin/env bash
# scripts/make_fixtures.sh — génère les fixtures audio déterministes et libres de droits.
# Référence de mesure Python (2026-07-31) : max_score = 0.9967 sur hey_jarvis_espeak.wav.
# Si le score Python descend sous 0,5 sur le positif, la fixture est cassée : la régénérer.
set -euo pipefail

FIXTURES_DIR="app/src/test/resources/fixtures"
mkdir -p "$FIXTURES_DIR"

echo "Génération des fixtures avec espeak-ng + ffmpeg…"

# ── Positif : "hey jarvis" ───────────────────────────────────────────────────
espeak-ng -v en-us -s 150 -w /tmp/hj.wav "hey jarvis"
ffmpeg -y -loglevel error -i /tmp/hj.wav -ar 16000 -ac 1 \
  -af "adelay=500|500,apad=pad_dur=1" \
  "$FIXTURES_DIR/hey_jarvis_espeak.wav"

# ── Négatif : phrase neutre ──────────────────────────────────────────────────
espeak-ng -v en-us -s 150 -w /tmp/neg.wav "the weather is nice today"
ffmpeg -y -loglevel error -i /tmp/neg.wav -ar 16000 -ac 1 \
  -af "adelay=500|500,apad=pad_dur=1" \
  "$FIXTURES_DIR/negatif_espeak.wav"

echo "Fixtures créées :"
ls -lh "$FIXTURES_DIR/"

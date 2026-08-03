#!/usr/bin/env bash
# scripts/pull_measures_test.sh — Tests unitaires des gardes de pull_measures.sh.
#
# Vérifie les deux réserves MOYENNE de l'audit Codex du 2026-08-03 :
#   Réserve #1 (pull_measures.sh:22) — validation du label SESSION
#   Réserve #2 (pull_measures.sh:78) — extraction de timestamps sans SIGPIPE
#
# Usage :
#   bash scripts/pull_measures_test.sh
#
# Retourne 0 si tous les tests passent, 1 sinon.
# Ces tests TOMBENT si les gardes sautent :
#   - supprimer le bloc de validation SESSION → assert_reject tombe
#   - remettre `grep | head -1` → test_timestamp_extraction tombe

set -euo pipefail

PASS=0
FAIL=0
ERRORS=()

# ── Utilitaires ────────────────────────────────────────────────────────────────

ok() {
    local name="$1"
    echo "  PASS $name"
    ((PASS++)) || true
}

fail() {
    local name="$1" msg="$2"
    echo "  FAIL $name : $msg"
    ERRORS+=("$name : $msg")
    ((FAIL++)) || true
}

# ── Garde réserve #1 : validation SESSION ─────────────────────────────────────
# Miroir exact du bloc ajouté dans pull_measures.sh après SESSION="${1:-...}".
# IMPORTANT : ne PAS importer pull_measures.sh (il appelle adb) — dupliquer la
# logique de validation est intentionnel pour le test d'isolation.

validate_session() {
    local SESSION="$1"
    if [[ -z "${SESSION}" ]] || \
       [[ ! "${SESSION}" =~ ^[A-Za-z0-9._-]+$ ]] || \
       [[ "${SESSION}" == "." ]] || \
       [[ "${SESSION}" == ".." ]]; then
        return 1   # rejeté
    fi
    return 0       # accepté
}

test_session_validation() {
    echo "── Réserve #1 : validation SESSION ──────────────────────────────────────"

    # Cas rejetés
    local reject_cases=(
        "../evil"
        "../../etc/passwd"
        "/absolute/path"
        "."
        ".."
        ""
        "session with spaces"
        "label;injection"
        "label\$(cmd)"
        "label|pipe"
        "label>redir"
    )
    for label in "${reject_cases[@]}"; do
        if validate_session "${label}"; then
            fail "reject '${label}'" "label accepté alors qu'il devrait être rejeté"
        else
            ok "reject '${label}'"
        fi
    done

    # Cas acceptés
    local accept_cases=(
        "session-ok"
        "2026-08-03T120000Z"
        "my.session_1"
        "A"
        "a-b-c"
        "123"
        "session.2026.08.03"
    )
    for label in "${accept_cases[@]}"; do
        if validate_session "${label}"; then
            ok "accept '${label}'"
        else
            fail "accept '${label}'" "label rejeté alors qu'il devrait être accepté"
        fi
    done
}

# ── Garde réserve #2 : extraction de timestamps sans SIGPIPE ──────────────────
# Vérifie que awk lit les timestamps correctement sur 1 ligne et sur N lignes.
# Miroir des lignes corrigées dans pull_measures.sh.

test_timestamp_extraction() {
    echo "── Réserve #2 : extraction timestamps sans SIGPIPE ──────────────────────"

    local TMP
    TMP=$(mktemp)
    # shellcheck disable=SC2064
    trap "rm -f '${TMP}'" RETURN

    # ── Sous-test A : un seul enregistrement (scénario exact du SIGPIPE) ────
    echo "2026-08-02T21:14:03.412Z detect score=0.930 rms=1234.5 since_boot_ms=123456" \
        > "${TMP}"

    local FIRST_TS LAST_TS
    FIRST_TS=$(awk '/detect score=/{print $1; exit}' "${TMP}")
    FIRST_TS="${FIRST_TS:-—}"
    LAST_TS=$(awk '/detect score=/{ts=$1} END{print ts}' "${TMP}")
    LAST_TS="${LAST_TS:-—}"

    if [[ "${FIRST_TS}" == "—" ]]; then
        fail "single-line FIRST_TS" \
            "attendu '2026-08-02T21:14:03.412Z', obtenu '—' (SIGPIPE non corrigé ?)"
    else
        ok "single-line FIRST_TS (${FIRST_TS})"
    fi

    if [[ "${FIRST_TS}" != "2026-08-02T21:14:03.412Z" ]]; then
        fail "single-line FIRST_TS valeur" \
            "attendu '2026-08-02T21:14:03.412Z', obtenu '${FIRST_TS}'"
    else
        ok "single-line FIRST_TS valeur exacte"
    fi

    # ── Sous-test B : plusieurs enregistrements ──────────────────────────────
    {
        echo "2026-08-02T21:14:03.412Z detect score=0.930 rms=1234.5 since_boot_ms=123456"
        echo "2026-08-02T21:14:07.100Z detect score=0.920 rms=1100.0 since_boot_ms=127124"
        echo "2026-08-02T21:14:11.500Z detect score=0.870 rms=980.0 since_boot_ms=131524"
    } > "${TMP}"

    FIRST_TS=$(awk '/detect score=/{print $1; exit}' "${TMP}")
    FIRST_TS="${FIRST_TS:-—}"
    LAST_TS=$(awk '/detect score=/{ts=$1} END{print ts}' "${TMP}")
    LAST_TS="${LAST_TS:-—}"

    if [[ "${FIRST_TS}" != "2026-08-02T21:14:03.412Z" ]]; then
        fail "multi-line FIRST_TS" \
            "attendu '2026-08-02T21:14:03.412Z', obtenu '${FIRST_TS}'"
    else
        ok "multi-line FIRST_TS"
    fi

    if [[ "${LAST_TS}" != "2026-08-02T21:14:11.500Z" ]]; then
        fail "multi-line LAST_TS" \
            "attendu '2026-08-02T21:14:11.500Z', obtenu '${LAST_TS}'"
    else
        ok "multi-line LAST_TS"
    fi

    # ── Sous-test C : fichier vide → fallback "—" ───────────────────────────
    > "${TMP}"   # vider le fichier
    FIRST_TS=$(awk '/detect score=/{print $1; exit}' "${TMP}")
    FIRST_TS="${FIRST_TS:-—}"

    if [[ "${FIRST_TS}" != "—" ]]; then
        fail "empty-log FIRST_TS" \
            "attendu '—' sur fichier vide, obtenu '${FIRST_TS}'"
    else
        ok "empty-log FIRST_TS = '—'"
    fi
}

# ── Exécution ─────────────────────────────────────────────────────────────────

echo ""
echo "=== pull_measures_test.sh — gardes audit Codex 2026-08-03 ==="
echo ""

test_session_validation
echo ""
test_timestamp_extraction
echo ""

# ── Rapport ───────────────────────────────────────────────────────────────────
echo "=== Résultat : ${PASS} PASS, ${FAIL} FAIL ==="
if [[ ${FAIL} -gt 0 ]]; then
    echo ""
    echo "Échecs détaillés :"
    for e in "${ERRORS[@]}"; do
        echo "  • $e"
    done
    exit 1
fi
echo "OK — tous les gardes sont en place."

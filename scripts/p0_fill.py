#!/usr/bin/env python3
"""scripts/p0_fill.py — agrège les mesures brutes P0 et remplit docs/p0-mesures.md.

    python3 scripts/p0_fill.py            # écrit le fichier
    python3 scripts/p0_fill.py --check    # auto-test, n'écrit rien

Aucun chiffre n'est saisi à la main : tout vient de docs/mesures-brutes/*.csv
produits par p0_session.sh / p0_fp.sh / p0_battery.sh. Une source absente laisse
son champ VIDE — jamais une valeur par défaut, jamais un zéro consolant.

`verdict_p0` n'est calculé que si le protocole est COMPLET (20 essais par
condition, ≥ 8 h de corpus négatif, ≥ 2 nuits). Incomplet ≠ échoué : le champ
reste vide et le rapport dit ce qui manque. C'est la même distinction que
NO_VERDICT vs NO_GO côté plan_runner.
"""
from __future__ import annotations

import csv
import sys
from datetime import date
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RAW = ROOT / "docs" / "mesures-brutes"
DOC = ROOT / "docs" / "p0-mesures.md"

CONDITIONS = ["1m-silence", "1m-tv", "3m-silence", "3m-tv"]
ESSAIS_PAR_CONDITION = 20
SEUILS = {                      # champ: (comparaison, seuil)
    "hits_1m": (">=", 0.90),
    "hits_3m": (">=", 0.75),
    "fp_per_day": ("<=", 2.0),
    "battery_delta": ("<=", 1.5),
    "latency_p95": ("<=", 2.5),
}


def _rows(path: Path) -> list[dict]:
    if not path.is_file():
        return []
    with path.open(encoding="utf-8") as fh:
        return [r for r in csv.DictReader(fh) if any(v.strip() for v in r.values())]


def essais() -> dict[str, list[dict]]:
    return {c: _rows(RAW / f"essais-{c}.csv") for c in CONDITIONS}


def p95(valeurs: list[float]) -> float | None:
    """Rang le plus proche, borne haute — 40 points ne justifient pas d'interpoler."""
    if not valeurs:
        return None
    ordonnes = sorted(valeurs)
    i = max(0, -(-len(ordonnes) * 95 // 100) - 1)   # ceil(n*0.95) - 1
    return ordonnes[i]


def agrege() -> tuple[dict, list[str]]:
    """Retourne (champs, manques). Un champ absent des `champs` reste vide."""
    ess, champs, manques = essais(), {}, []

    for dist in ("1m", "3m"):
        lignes = ess[f"{dist}-silence"] + ess[f"{dist}-tv"]
        attendu = 2 * ESSAIS_PAR_CONDITION
        if lignes:
            champs[f"n_essais_{dist}"] = len(lignes)
            ok = sum(1 for r in lignes if r["succes"].strip().upper() == "O")
            champs[f"hits_{dist}"] = round(ok / len(lignes), 3)
        if len(lignes) < attendu:
            manques.append(f"{dist} : {len(lignes)}/{attendu} essais")

    lat = [float(r["latence_s"]) for c in CONDITIONS for r in ess[c]
           if r["succes"].strip().upper() == "O" and r["latence_s"].strip()]
    v = p95(lat)
    if v is not None:
        champs["latency_p95"] = round(v, 2)

    fp = _rows(RAW / "fp.csv")
    if fp:
        heures = sum(float(r["heures"]) for r in fp)
        detections = sum(int(r["detections"]) for r in fp)
        champs["fp_hours"] = round(heures, 2)
        if heures > 0:
            champs["fp_per_day"] = round(detections * 24 / heures, 2)
        if heures < 8:
            manques.append(f"corpus négatif : {heures:.1f} h / 8 h")
    else:
        manques.append("corpus négatif : aucune fenêtre")

    bat = {r["nuit"]: r for r in _rows(RAW / "batterie.csv")}
    champs["battery_nights"] = len(bat)
    if {"temoin", "armee"} <= set(bat):
        champs["battery_delta"] = round(
            float(bat["armee"]["pct_par_h"]) - float(bat["temoin"]["pct_par_h"]), 2)
    else:
        manques.append(f"batterie : {sorted(bat) or 'aucune nuit'} (témoin + armée requises)")

    return champs, manques


def verdict(champs: dict, manques: list[str]) -> tuple[str, list[str]]:
    if manques:
        return "", []
    echecs = [
        f"{k} = {champs[k]} (seuil {op} {s})"
        for k, (op, s) in SEUILS.items()
        if (champs[k] < s if op == ">=" else champs[k] > s)
    ]
    return ("GO" if not echecs else "NOGO"), echecs


def table(lignes: list[dict], n: int) -> str:
    out = ["| # | succès (O/N) | latence_s | notes |", "|---|---|---|---|"]
    for i in range(1, n + 1):
        r = lignes[i - 1] if i <= len(lignes) else {}
        out.append(f"| {i} | {r.get('succes','')} | {r.get('latence_s','')} | |")
    return "\n".join(out)


def rendu(champs: dict, manques: list[str], v: str, echecs: list[str], notes: str) -> str:
    ordre = ["date_mesure", "n_essais_1m", "hits_1m", "n_essais_3m", "hits_3m",
             "fp_hours", "fp_per_day", "battery_nights", "battery_delta",
             "latency_p95", "verdict_p0", "verdict_repli"]
    champs = dict(champs)
    champs.setdefault("date_mesure", date.today().isoformat())
    champs["verdict_p0"] = v
    champs.setdefault("verdict_repli", "")
    bloc = "\n".join(f"{k}={champs.get(k, '')}" for k in ordre)

    etat = ["## État du protocole", ""]
    if manques:
        etat.append("**INCOMPLET — `verdict_p0` laissé vide, un protocole partiel "
                    "ne rend pas de verdict.** Il manque :")
        etat += [f"- {m}" for m in manques]
    elif echecs:
        etat.append("**Protocole complet. Seuil(s) NON tenu(s) → `verdict_p0=NOGO`** :")
        etat += [f"- {e}" for e in echecs]
        etat.append("")
        etat.append("Remplir `verdict_repli` (`porcupine` ou `poids-propres`) pour router le P0-bis.")
    else:
        etat.append("**Protocole complet, les cinq seuils sont tenus → `verdict_p0=GO`.**")

    ess = essais()
    tables = "\n\n".join(
        f"### Condition `{c}`\n\n{table(ess[c], ESSAIS_PAR_CONDITION)}" for c in CONDITIONS)

    fp = _rows(RAW / "fp.csv")
    fp_tbl = "\n".join(
        ["| début | fin | durée_h | détections | fp/jour |", "|---|---|---|---|---|"]
        + [f"| {r['debut']} | {r['fin']} | {r['heures']} | {r['detections']} | {r['fp_par_jour']} |"
           for r in fp] or ["| | | | | |"])

    bat = _rows(RAW / "batterie.csv")
    bat_tbl = "\n".join(
        ["| nuit | début | fin | durée_h | %_début | %_fin | %/h |", "|---|---|---|---|---|---|---|"]
        + [f"| {r['nuit']} | {r['debut']} | {r['fin']} | {r['heures']} | "
           f"{r['pct_debut']} | {r['pct_fin']} | {r['pct_par_h']} |" for r in bat] or ["| | | | | | | |"])

    return f"""# Mesures — protocole P0

> **Fichier GÉNÉRÉ par `scripts/p0_fill.py`.** Ne pas éditer à la main au-dessus
> de « Notes de terrain » : la prochaine exécution écrase. Les chiffres viennent
> de `docs/mesures-brutes/*.csv`, produits par `p0_session.sh`, `p0_fp.sh` et
> `p0_battery.sh`. Les noms de champs sont un contrat lu par
> `scripts/p0_verdict.py` (JA-T7) — ne pas les renommer.
>
> `verdict_p0` = `GO` ou `NOGO` (sans tiret : le check JA-T7 lit `GO|NOGO`).
> `latency_p95` inclut le temps de réaction et la durée du mot (décision Owner
> 2026-08-03) : c'est une borne HAUTE, pas la latence du moteur seul.

## Seuils d'acceptation (Owner 2026-07-31 — architecture §8.4)

| Grandeur | Seuil | Champ |
|---|---|---|
| Détection 1 m | ≥ 90 % | `hits_1m` |
| Détection 3 m | ≥ 75 % | `hits_3m` |
| Faux positifs | ≤ 2/jour | `fp_per_day` |
| Batterie, écran éteint | ≤ +1,5 %/h vs témoin | `battery_delta` |
| Latence | p95 ≤ 2,5 s (plafond dur ; cible 1,5 s) | `latency_p95` |

## Mesures

```
{bloc}
```

{chr(10).join(etat)}

## Essais — 20 par condition, 80 au total

{tables}

## Corpus négatif

{fp_tbl}

## Batterie

{bat_tbl}

## Notes de terrain

{notes}"""


def notes_existantes() -> str:
    if not DOC.is_file():
        return "<!-- Consigner ici les observations : OEM Samsung, batterie, faux positifs remarquables. -->\n"
    txt = DOC.read_text(encoding="utf-8")
    marqueur = "## Notes de terrain"
    return txt.split(marqueur, 1)[1].lstrip("\n") if marqueur in txt else \
        "<!-- Consigner ici les observations. -->\n"


def _check() -> None:
    """Auto-test : le verdict ne ment pas, et l'incomplet ne rend pas de verdict."""
    complet = {"hits_1m": 0.95, "hits_3m": 0.80, "fp_per_day": 1.0,
               "battery_delta": 0.9, "latency_p95": 1.2}
    assert verdict(complet, []) == ("GO", [])
    assert verdict(complet, ["batterie : 1 nuit"]) == ("", []), "incomplet doit rester sans verdict"
    v, e = verdict({**complet, "hits_3m": 0.70}, [])
    assert v == "NOGO" and len(e) == 1 and "hits_3m" in e[0], (v, e)
    v, e = verdict({**complet, "latency_p95": 2.6}, [])
    assert v == "NOGO" and "latency_p95" in e[0]
    assert verdict({**complet, "latency_p95": 2.5}, []) == ("GO", []), "plafond dur 2.5 inclus"
    assert p95([1.0]) == 1.0
    assert p95(list(range(1, 21))) == 19, p95(list(range(1, 21)))
    assert p95([]) is None
    print("p0_fill --check : OK")


def main() -> int:
    if "--check" in sys.argv:
        _check()
        return 0
    champs, manques = agrege()
    v, echecs = verdict(champs, manques)
    DOC.write_text(rendu(champs, manques, v, echecs, notes_existantes()), encoding="utf-8")
    print(f"{DOC.relative_to(ROOT)} écrit — verdict_p0={v or '(vide, protocole incomplet)'}")
    for m in manques:
        print(f"  manque : {m}")
    for e in echecs:
        print(f"  seuil non tenu : {e}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""JA-T7 — verdict P0 recalculé depuis docs/p0-mesures.md (stdlib). --demo = auto-contrôle."""
import math
import re
import sys
from pathlib import Path

DOC = Path(__file__).resolve().parent.parent / "docs" / "p0-mesures.md"
CHAMPS = ["n_essais_1m", "hits_1m", "n_essais_3m", "hits_3m", "fp_hours",
          "fp_per_day", "battery_nights", "battery_delta", "latency_p95"]


def lire(texte):
    c = {}
    for m in re.finditer(r"^([a-z_0-9]+)=(.*)$", texte, re.M):
        if m.group(1) in c:
            raise SystemExit(f"champ en double: {m.group(1)}")
        c[m.group(1)] = m.group(2).strip()
    return c


def calcule(c):
    v = {}
    for k in CHAMPS:
        try:
            v[k] = float(c[k])
        except (KeyError, ValueError):
            raise SystemExit(f"protocole incomplet: champ {k} manquant ou vide")
        if not math.isfinite(v[k]):
            raise SystemExit(f"valeur non finie: {k}")
    err = [m for m, ok in [
        ("n_essais_1m < 20", v["n_essais_1m"] >= 20), ("n_essais_3m < 20", v["n_essais_3m"] >= 20),
        ("fp_hours < 8", v["fp_hours"] >= 8), ("battery_nights < 2", v["battery_nights"] >= 2),
        ("ratio hors [0,1]", 0 <= v["hits_1m"] <= 1 and 0 <= v["hits_3m"] <= 1),
        ("latency_p95 <= 0", v["latency_p95"] > 0),
        ("fp_per_day < 0", v["fp_per_day"] >= 0)] if not ok]
    if err:
        raise SystemExit("protocole incomplet: " + ", ".join(err))
    go = (v["hits_1m"] >= .90 and v["hits_3m"] >= .75 and v["fp_per_day"] <= 2
          and v["battery_delta"] <= 1.5 and v["latency_p95"] <= 2.5)
    cible = "tenue" if v["latency_p95"] <= 1.5 else "NON_tenue"
    return (f"p0: hits_1m={v['hits_1m']} hits_3m={v['hits_3m']} fp_per_day={v['fp_per_day']} "
            f"battery_delta={v['battery_delta']} latency_p95={v['latency_p95']} "
            f"latence_cible_1s5={cible} verdict_calcule={'GO' if go else 'NOGO'}")


def demo():
    base = dict(n_essais_1m=40, hits_1m=.9, n_essais_3m=40, hits_3m=.8, fp_hours=8, fp_per_day=1,
                battery_nights=2, battery_delta=1.1, latency_p95=1.4)
    assert calcule(base).endswith("verdict_calcule=GO")
    assert calcule({**base, "hits_3m": .7}).endswith("verdict_calcule=NOGO")
    assert "NON_tenue" in calcule({**base, "latency_p95": 2.0})
    for bad in ({**base, "n_essais_1m": 19}, {**base, "hits_1m": ""}, {**base, "battery_nights": 1}):
        try:
            calcule(bad)
        except SystemExit:
            continue
        raise AssertionError("incomplet accepté")
    for bad in ({**base, "fp_hours": "inf"}, {**base, "fp_per_day": "nan"}, {**base, "fp_per_day": -1}):
        try:
            calcule(bad)
        except SystemExit:
            continue
        raise AssertionError("valeur invalide acceptée")
    try:
        lire("hits_1m=0.5\nhits_1m=0.9\n")
    except SystemExit:
        print("ok")
        return
    raise AssertionError("doublon accepté")


if __name__ == "__main__":
    if "--demo" in sys.argv:
        demo()
    else:
        print(calcule(lire(DOC.read_text())))

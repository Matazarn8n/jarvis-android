# Gabarit de mesures — protocole P0

> Remplir après chaque session de mesure. Les noms de champs sont un contrat
> lu par `scripts/p0_verdict.py` (JA-T7) — **ne pas les renommer**.
>
> `hits_*` = ratio entre 0 et 1 (18 succès sur 20 → `0.90`).
> `battery_delta` = %/h supplémentaires par rapport à la nuit témoin sans l'app.
> `latency_p95` = secondes, fin d'énonciation → signal de détection (logcat JARVISWAKE).
> `verdict_p0` = `GO` ou `NO-GO`.

```
date_mesure=
n_essais_1m=
hits_1m=
n_essais_3m=
hits_3m=
fp_hours=
fp_per_day=
battery_nights=
battery_delta=
latency_p95=
verdict_p0=
```

## Tableau des essais

| # | condition | distance | bruit | succès | latence_s | notes |
|---|-----------|----------|-------|--------|-----------|-------|
| 1 | | | | | | |
| 2 | | | | | | |
| 3 | | | | | | |
| 4 | | | | | | |
| 5 | | | | | | |
| 6 | | | | | | |
| 7 | | | | | | |
| 8 | | | | | | |
| 9 | | | | | | |
| 10 | | | | | | |
| 11 | | | | | | |
| 12 | | | | | | |
| 13 | | | | | | |
| 14 | | | | | | |
| 15 | | | | | | |
| 16 | | | | | | |
| 17 | | | | | | |
| 18 | | | | | | |
| 19 | | | | | | |
| 20 | | | | | | |

**Colonnes :**
- `condition` : `1m-silence`, `1m-tv`, `3m-silence`, `3m-tv`
- `distance` : en mètres
- `bruit` : `silence` / `tv` / `musique` / autre
- `succès` : `O` / `N`
- `latence_s` : secondes de la fin d'énonciation au signal JARVISWAKE (peut être vide si N)

## Notes de terrain

<!-- Consigner ici les observations : OEM Samsung, comportement batterie, faux positifs remarquables, etc. -->

# Batterie / background

Sources : background-optimization, power/power-details.

## Restrictions broadcasts implicites (Android 7.0+ / API 24+)

- **CONNECTIVITY_ACTION** : plus reçu si déclaré en manifest pour les apps ciblant API 24+. `Context.registerReceiver()` (dynamique) continue de fonctionner.
- **ACTION_NEW_PICTURE / ACTION_NEW_VIDEO** : complètement bloqués pour toutes les apps.

### Migration — job sur connexion non facturée
```kotlin
val job = JobInfo.Builder(MY_BACKGROUND_JOB, ComponentName(context, MyJobService::class.java))
    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
    .setRequiresCharging(true)
    .build()
jobScheduler.schedule(job)
```

### Migration — monitoring réseau en foreground
```kotlin
val request = NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build()
connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) { /* ... */ }
})
// unregisterNetworkCallback() avant de quitter
```

### Migration — ACTION_NEW_PICTURE/VIDEO → TriggerContentUri
```kotlin
val job = JobInfo.Builder(MY_BACKGROUND_JOB, ComponentName(context, MediaContentJob::class.java))
    .addTriggerContentUri(JobInfo.TriggerContentUri(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        JobInfo.TriggerContentUri.FLAG_NOTIFY_FOR_DESCENDANTS))
    .build()
```
Piège : incompatible avec `setPeriodic()` et `setPersisted()`.

## Restrictions utilisateur (battery usage)

| État | Comportement |
|---|---|
| Unrestricted | tout travail de fond autorisé |
| Optimized (défaut) | selon interaction utilisateur |
| Restricted | aucun travail de fond |

Seuils déclenchant une notification système : **1 wake lock partiel maintenu ≥1h écran éteint**, ou app API<26 avec services de fond excessifs. En mode "Restricted" (AOSP A9+) : foreground services bloqués, alarmes bloquées, jobs bloqués, et depuis A13 les broadcasts BOOT_COMPLETED/LOCKED_BOOT_COMPLETED aussi.

## App Standby Buckets — quotas chiffrés

| Bucket | Jobs réguliers | Jobs expedited | Alarmes | Réseau |
|---|---|---|---|---|
| Active | 20 min/60 min | 30 min/24h | illimité | oui |
| Working set | 10 min/4h | 15 min/24h | 10/h | oui |
| Frequent | 10 min/12h | 10 min/24h | 2/h | oui |
| Rare | 10 min/24h | 10 min/24h | 1/h | **non** |
| Restricted | 10 min/jour (1 fois) | 5 min/24h | 1/jour | **non** |

Rare et Restricted désactivent complètement le réseau.

**Changement Android 16** : avant, le bucket "active" n'avait AUCUNE limite de jobs, y compris en foreground service. Depuis A16, les limites (20 min/60 min) s'appliquent même en "active" et même avec un foreground service actif. Tester impérativement sur A16+ pour éviter des rejets d'exécution en prod qui n'existaient pas avant.

FCM high-priority (A13+) : les Standby Buckets n'affectent plus les quotas FCM, mais le système downgrade automatiquement les envois "high-priority" détectés comme abusifs (sans notification associée).

## Aucune limite si

- Device en charge (sauf bucket "restricted").
- App visible au premier plan.
- Écran allumé, pour certaines ressources.

Priorité : `foreground visible > foreground service > background > restricted`.

## Stratégie par bucket

- App en "Rare" : 10 min/24h ≈ 41s/heure en moyenne → grouper les tâches en batch, utiliser les fenêtres de maintenance Doze, préférer FCM pour l'urgent.
- Alarmes régulières : différées à la fenêtre de maintenance Doze. `setAndAllowWhileIdle()` max ~7/heure en Doze.
- Jobs expedited : quota séparé et plus court — réserver au vrai travail urgent, prévoir un fallback vers les jobs réguliers si quota épuisé.

## Tests

```bash
adb shell cmd appops set <package_name> RUN_IN_BACKGROUND ignore
adb shell cmd appops set <package_name> RUN_IN_BACKGROUND allow
adb shell cmd appops set <PACKAGE_NAME> RUN_ANY_IN_BACKGROUND deny
```

## Recommandation par défaut

Préférer **WorkManager** à JobScheduler brut : pas de dépendance Play Services forcée, chaînage de tâches, vérification de statut — WorkManager reste soumis aux mêmes quotas de bucket en interne.

## Pièges nommés

- Remplacer un broadcast manifest par un registerReceiver() dynamique sans vérifier qu'il ne fuit pas (désenregistrer au bon moment de cycle de vie).
- Croire qu'un foreground service échappe aux quotas sur A16+ — faux depuis ce changement.
- Alarmes exactes en excès → consommées trop vite par le quota du bucket, préférer inexact quand possible (mieux toléré, batché par le système).

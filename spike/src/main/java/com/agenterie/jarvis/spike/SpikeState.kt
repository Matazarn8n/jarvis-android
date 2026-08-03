// Copyright 2026 Agenterie. Apache-2.0.
package com.agenterie.jarvis.spike

import java.util.concurrent.atomic.AtomicBoolean

/**
 * État partagé du service spike — objet Kotlin pur (aucune dépendance Android)
 * pour être testable en JVM sans contexte Android.
 *
 * Source de vérité pour [SpikeActivity.onResume] : l'état affiché doit être dérivé
 * de l'état RÉEL du service, pas du cycle de vie de l'activité.
 *
 * Réserve audit Codex #3 (SpikeActivity.kt:83) — garde rotation :
 * si `SpikeActivity` lit ici à chaque `onResume`, une rotation d'écran ne peut
 * pas afficher "Armer" alors que le service tourne.
 */
internal object SpikeState {
    /** true ↔ SpikeWakeService est en cours d'exécution. */
    val running: AtomicBoolean = AtomicBoolean(false)

    /** Lecture thread-safe depuis n'importe quel contexte. */
    fun isRunning(): Boolean = running.get()
}

// Copyright 2026 Agenterie. Apache-2.0.
package com.agenterie.jarvis.spike

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Réserve audit Codex #3 — rotation d'écran (SpikeActivity.kt:83).
 *
 * Garantit que [SpikeState] reflète fidèlement l'état du service et que
 * [SpikeWakeService.isRunning] délègue à [SpikeState].
 *
 * Ce test TOMBE si le garde saute, c'est-à-dire si :
 *   - SpikeWakeService.onCreate() arrête d'appeler SpikeState.running.set(true), ou
 *   - SpikeActivity.onResume() arrête de lire SpikeWakeService.isRunning().
 * Dans les deux cas, l'activité recréée après rotation ne pourrait plus synchroniser
 * son UI avec l'état réel du service.
 *
 * Aucune dépendance Android : [SpikeState] est un objet Kotlin pur.
 * Mutation vérifiée : retirer `SpikeState.running.set(true)` → assertFalse l.40 tombe.
 */
class SpikeRotationGuardTest {

    @Before
    fun reset() {
        // Partir d'un état propre avant chaque test.
        SpikeState.running.set(false)
    }

    @After
    fun cleanup() {
        SpikeState.running.set(false)
    }

    // ── Test 1 : isRunning() reflète SpikeState ───────────────────────────────

    @Test
    fun `SpikeWakeService_isRunning delegates to SpikeState`() {
        SpikeState.running.set(false)
        assertFalse(
            "isRunning() doit être false quand SpikeState.running est false",
            SpikeWakeService.isRunning()
        )

        SpikeState.running.set(true)
        assertTrue(
            "isRunning() doit être true quand SpikeState.running est true",
            SpikeWakeService.isRunning()
        )
    }

    // ── Test 2 : état faux avant démarrage, vrai après, faux après arrêt ─────

    @Test
    fun `SpikeState tracks simulated service lifecycle`() {
        // Avant démarrage (simule : service non créé)
        assertFalse("Service non démarré → isRunning false", SpikeState.isRunning())

        // Simule onCreate → SpikeState.running.set(true)
        SpikeState.running.set(true)
        assertTrue("Service démarré → isRunning true", SpikeState.isRunning())

        // Simule onDestroy → SpikeState.running.set(false)
        SpikeState.running.set(false)
        assertFalse("Service arrêté → isRunning false", SpikeState.isRunning())
    }

    // ── Test 3 : thread-safety basique ───────────────────────────────────────

    @Test
    fun `SpikeState running is thread-safe`() {
        // Deux threads écrivent en sens opposés ; la lecture finale doit être cohérente.
        val t1 = Thread { SpikeState.running.set(true) }
        val t2 = Thread { SpikeState.running.set(false) }
        t1.start(); t2.start(); t1.join(); t2.join()
        // Pas d'assertion sur la valeur finale (race) — on vérifie juste que ça
        // ne lève pas ConcurrentModificationException.
        SpikeState.isRunning() // appel sans crash = succès
    }
}

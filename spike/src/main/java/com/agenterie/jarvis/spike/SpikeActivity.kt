// Copyright 2026 Agenterie. Apache-2.0.
package com.agenterie.jarvis.spike

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * SpikeActivity — activité unique de l'APK de mesure P0 (JA-T5).
 *
 * UI :
 *   - Bouton « Armer / Désarmer » : démarre / arrête [SpikeWakeService]
 *   - Score courant en temps réel (mis à jour via BroadcastReceiver ~6 fps)
 *   - Compteur de détections au-dessus du seuil
 *   - Chemin du fichier journal (pour `adb pull` par scripts/pull_measures.sh)
 *
 * Restriction Android 34+ respectée : le FGS est démarré DEPUIS cette activité
 * visible — démarrage depuis l'arrière-plan lèverait SecurityException.
 */
class SpikeActivity : AppCompatActivity() {

    private lateinit var tvScore: TextView
    private lateinit var tvDetections: TextView
    private lateinit var tvJournal: TextView
    private lateinit var btnToggle: Button

    private var serviceRunning = false

    // ── BroadcastReceiver pour les mises à jour temps réel ──────────────────
    private val scoreReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val score   = intent.getFloatExtra(SpikeWakeService.EXTRA_SCORE, 0f)
            val detects = intent.getIntExtra(SpikeWakeService.EXTRA_DETECTS, 0)
            tvScore.text      = "Score : %.4f".format(score)
            tvDetections.text = "Détections : $detects"
        }
    }

    // ── Permissions ──────────────────────────────────────────────────────────
    companion object {
        private const val REQ_PERMISSIONS = 42
        private val REQUIRED_PERMISSIONS = buildList {
            add(Manifest.permission.RECORD_AUDIO)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()
    }

    // ── Lifecycle ────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spike)

        tvScore      = findViewById(R.id.tvScore)
        tvDetections = findViewById(R.id.tvDetections)
        tvJournal    = findViewById(R.id.tvJournal)
        btnToggle    = findViewById(R.id.btnToggle)

        // Afficher le chemin du journal dès l'ouverture
        val journalPath = "${filesDir.absolutePath}/spike-detections.log"
        tvJournal.text = "Journal : $journalPath"

        btnToggle.setOnClickListener { onToggle() }
    }

    override fun onStart() {
        super.onStart()
        // Enregistrement du receiver (RECEIVER_NOT_EXPORTED pour Android 13+)
        val filter = IntentFilter(SpikeWakeService.ACTION_SCORE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(scoreReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(scoreReceiver, filter)
        }
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(scoreReceiver)
    }

    // ── Logique armer / désarmer ─────────────────────────────────────────────

    private fun onToggle() {
        if (serviceRunning) {
            // Désarmer
            stopService(Intent(this, SpikeWakeService::class.java))
            serviceRunning = false
            btnToggle.text = "Armer"
            tvScore.text   = "Score : —"
        } else {
            // Vérifier les permissions avant de démarrer
            if (!hasAllPermissions()) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQ_PERMISSIONS)
                return
            }
            armService()
        }
    }

    private fun armService() {
        val intent = Intent(this, SpikeWakeService::class.java)
        ContextCompat.startForegroundService(this, intent)
        serviceRunning = true
        btnToggle.text = "Désarmer"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQ_PERMISSIONS) return

        val denied = permissions.indices.filter { grantResults[it] != PackageManager.PERMISSION_GRANTED }
        if (denied.isEmpty()) {
            armService()
        } else {
            val missing = denied.joinToString { permissions[it].substringAfterLast('.') }
            Toast.makeText(this, "Permission(s) refusée(s) : $missing", Toast.LENGTH_LONG).show()
        }
    }

    private fun hasAllPermissions(): Boolean =
        REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
}

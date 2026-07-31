package com.agenterie.jarvis

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Socle JA-T1 : prouve seulement que la chaîne de build produit un apk
 * installable. La WebView durcie (allowlist d'origine, onShowFileChooser,
 * onPermissionRequest) arrive en JA-T8.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(TextView(this).apply { text = "Jarvis — socle P1" })
    }
}

package com.nosefc27.launcher

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply { text = "FC 27 Launcher"; textSize = 28f; setPadding(24, 32, 24, 16) }
        val status = TextView(this).apply {
            text = "Preparado para descargar los recursos del juego.\n\nLa descarga ocupará aproximadamente 6,2 GB."
            textSize = 16f; setPadding(24, 8, 24, 16)
        }
        val progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply { max = 100; progress = 0 }
        val action = Button(this).apply { text = "Comprobar y descargar recursos" }
        action.setOnClickListener {
            status.text = "El manifiesto de distribución todavía no está configurado.\n\nEl launcher necesita URL públicas y hashes SHA-256 antes de descargar."
            action.isEnabled = false
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title)
            addView(status)
            addView(progress, LinearLayout.LayoutParams(-1, 48))
            addView(action)
        })
    }
}

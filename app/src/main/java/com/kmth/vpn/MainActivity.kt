package com.kmth.vpn

import android.app.AlertDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private var selectedServer = "SELECT SERVER"

    private val servers = arrayOf(
        "🇹🇭 DTAC V2RAY",
        "🇹🇭 DTAC SSH",
        "🇹🇭 DTAC SOCIAL",
        "🇹🇭 DTAC UIV",
        "🇹🇭 DTAC WETV"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.rgb(16, 20, 38))
        }

        val title = TextView(this).apply {
            text = "KMTH VPN"
            textSize = 32f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Open Source VPN"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        val server = TextView(this).apply {
            text = "🇹🇭  $selectedServer"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(20, 30, 20, 30)

            setOnClickListener {
                showServerDialog(this)
            }
        }

        val connect = TextView(this).apply {
            text = "CONNECT"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(20, 35, 20, 35)
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(server)
        root.addView(connect)

        setContentView(root)
    }

    private fun showServerDialog(serverView: TextView) {

        AlertDialog.Builder(this)
            .setTitle("SELECT SERVER")
            .setItems(servers) { _, which ->

                selectedServer = servers[which]

                serverView.text = selectedServer
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }
}

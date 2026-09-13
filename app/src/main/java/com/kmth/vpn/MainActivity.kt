package com.kmth.vpn

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.VpnService
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var serverText: TextView
    private lateinit var connectText: TextView
    private lateinit var statusText: TextView

    private var selectedServer = "DTAC V2RAY"
    private var connected = false

    private val servers = arrayOf(
        "🇹🇭 DTAC V2RAY",
        "🇹🇭 DTAC SSH",
        "🇹🇭 DTAC SOCIAL",
        "🇹🇭 DTAC UIV",
        "🇹🇭 DTAC WETV"
    )

    companion object {
        private const val VPN_REQUEST_CODE = 100
    }

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
            textSize = 34f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Open Source VPN"
            textSize = 18f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }

        serverText = TextView(this).apply {
            text = "🇹🇭  $selectedServer"
            textSize = 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(20, 30, 20, 30)

            setOnClickListener {
                showServerSelector()
            }
        }

        statusText = TextView(this).apply {
            text = "● DISCONNECTED"
            textSize = 16f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(10, 10, 10, 20)
        }

        connectText = TextView(this).apply {
            text = "CONNECT"
            textSize = 23f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(40, 25, 40, 25)

            setOnClickListener {
                toggleConnection()
            }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(serverText)
        root.addView(statusText)
        root.addView(connectText)

        setContentView(root)
    }

    private fun showServerSelector() {

        val builder = AlertDialog.Builder(this)

        builder.setTitle("SELECT SERVER")

        builder.setSingleChoiceItems(
            servers,
            servers.indexOfFirst {
                it.contains(selectedServer)
            }
        ) { dialog, which ->

            selectedServer = servers[which].removePrefix("🇹🇭 ")

            serverText.text = "🇹🇭  $selectedServer"

            dialog.dismiss()
        }

        builder.setNegativeButton("CANCEL", null)

        builder.show()
    }

    private fun toggleConnection() {

        if (connected) {
            disconnectVpn()
        } else {
            requestVpnPermission()
        }
    }

    private fun requestVpnPermission() {

        val intent = VpnService.prepare(this)

        if (intent != null) {
            startActivityForResult(
                intent,
                VPN_REQUEST_CODE
            )
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {

        val intent = Intent(this, MyVpnService::class.java)

        startService(intent)

        connected = true

        statusText.text = "● CONNECTED"
        statusText.setTextColor(Color.GREEN)

        connectText.text = "DISCONNECT"
    }

    private fun disconnectVpn() {

        val intent = Intent(this, MyVpnService::class.java)

        stopService(intent)

        connected = false

        statusText.text = "● DISCONNECTED"
        statusText.setTextColor(Color.LTGRAY)

        connectText.text = "CONNECT"
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (requestCode == VPN_REQUEST_CODE) {

            if (resultCode == Activity.RESULT_OK) {
                startVpnService()
            } else {
                statusText.text = "● VPN PERMISSION DENIED"
                statusText.setTextColor(Color.RED)
            }
        }
    }
}

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
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var serverText: TextView
    private lateinit var connectText: TextView
    private lateinit var statusText: TextView

    private var selectedServer = "DTAC V2RAY"
    private var selectedServerType = "vless"
    private var selectedServerConfig = ""
    private var connected = false

    private var servers = arrayOf(
        "🇹🇭 DTAC V2RAY",
        "🇹🇭 DTAC SSH",
        "🇹🇭 DTAC SOCIAL",
        "🇹🇭 DTAC UIV",
        "🇹🇭 DTAC WETV"
    )

    private var serverTypes = arrayOf("vless", "ssh", "ssh", "ssh", "ssh")
    private var serverConfigs = arrayOf("", "", "", "", "")

    companion object {
        private const val VPN_REQUEST_CODE = 100

        private const val CONFIG_URL =
            "https://raw.githubusercontent.com/kaungmyatjapan1999-boop/KMTH/refs/heads/main/config.json"

        private const val PREFS_NAME = "kmth_config"
        private const val KEY_VERSION = "version"
        private const val KEY_SERVERS = "servers"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUI()
        checkOnlineConfig()
    }

    private fun buildUI() {
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
            setOnClickListener { showServerSelector() }
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
            setOnClickListener { toggleConnection() }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(serverText)
        root.addView(statusText)
        root.addView(connectText)
        setContentView(root)
    }

    private fun showServerSelector() {
        AlertDialog.Builder(this)
            .setTitle("SELECT SERVER")
            .setSingleChoiceItems(
                servers,
                servers.indexOfFirst { it.contains(selectedServer) }
            ) { dialog, which ->
                selectedServer = servers[which].removePrefix("🇹🇭 ")
                selectedServerType = serverTypes.getOrElse(which) { "" }
                selectedServerConfig = serverConfigs.getOrElse(which) { "" }
                serverText.text = "🇹🇭  $selectedServer"
                dialog.dismiss()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun toggleConnection() {
        if (connected) disconnectVpn() else requestVpnPermission()
    }

    private fun requestVpnPermission() {
        if (selectedServerConfig.isBlank() ||
            selectedServerConfig == "YOUR_VLESS_CONFIG" ||
            selectedServerConfig == "YOUR_SSH_CONFIG"
        ) {
            AlertDialog.Builder(this)
                .setTitle("SERVER CONFIG MISSING")
                .setMessage("The selected server does not have a real configuration yet.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        if (selectedServerType != "vless") {
            AlertDialog.Builder(this)
                .setTitle("NOT READY")
                .setMessage("VLESS is being connected first. This server type is not wired to libXray yet.")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        val intent = VpnService.prepare(this)
        if (intent != null) {
            startActivityForResult(intent, VPN_REQUEST_CODE)
        } else {
            startVpnService()
        }
    }

    private fun startVpnService() {
        val intent = Intent(this, MyVpnService::class.java).apply {
            putExtra(MyVpnService.EXTRA_SERVER_NAME, selectedServer)
            putExtra(MyVpnService.EXTRA_SERVER_TYPE, selectedServerType)
            putExtra(MyVpnService.EXTRA_SERVER_CONFIG, selectedServerConfig)
        }

        startService(intent)
        connected = true
        statusText.text = "● CONNECTING..."
        statusText.setTextColor(Color.YELLOW)
        connectText.text = "DISCONNECT"
    }

    private fun disconnectVpn() {
        stopService(Intent(this, MyVpnService::class.java))
        connected = false
        statusText.text = "● DISCONNECTED"
        statusText.setTextColor(Color.LTGRAY)
        connectText.text = "CONNECT"
    }

    private fun checkOnlineConfig() {
        Thread {
            try {
                val connection = URL(CONFIG_URL).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val jsonText = connection.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(jsonText)
                    val onlineVersion = json.getInt("version")
                    val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    val localVersion = prefs.getInt(KEY_VERSION, 0)

                    if (onlineVersion > localVersion) {
                        runOnUiThread { showUpdateDialog(json, onlineVersion) }
                    } else {
                        runOnUiThread { loadSavedConfig() }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                android.util.Log.e("KMTH_CONFIG", "Config check failed", e)
                runOnUiThread { loadSavedConfig() }
            }
        }.start()
    }

    private fun showUpdateDialog(json: JSONObject, newVersion: Int) {
        AlertDialog.Builder(this)
            .setTitle("🔄 UPDATE AVAILABLE")
            .setMessage("New server configuration is available.\n\nVersion: $newVersion")
            .setPositiveButton("UPDATE NOW") { _, _ -> applyOnlineConfig(json, newVersion) }
            .setNegativeButton("LATER", null)
            .show()
    }

    private fun applyOnlineConfig(json: JSONObject, version: Int) {
        try {
            val serverArray = json.getJSONArray("servers")
            applyServerArray(serverArray)

            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putInt(KEY_VERSION, version)
                .putString(KEY_SERVERS, serverArray.toString())
                .apply()

            AlertDialog.Builder(this)
                .setTitle("✅ UPDATED")
                .setMessage("Server configuration updated successfully.")
                .setPositiveButton("OK", null)
                .show()
        } catch (e: Exception) {
            android.util.Log.e("KMTH_CONFIG", "Config update failed", e)
            AlertDialog.Builder(this)
                .setTitle("Update Failed")
                .setMessage("Could not update server configuration.")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun applyServerArray(serverArray: JSONArray) {
        val serverNames = mutableListOf<String>()
        val types = mutableListOf<String>()
        val configs = mutableListOf<String>()

        for (i in 0 until serverArray.length()) {
            val server = serverArray.getJSONObject(i)
            serverNames.add("🇹🇭 ${server.getString("name")}")
            types.add(server.optString("type", ""))
            configs.add(server.optString("config", ""))
        }

        if (serverNames.isEmpty()) return

        servers = serverNames.toTypedArray()
        serverTypes = types.toTypedArray()
        serverConfigs = configs.toTypedArray()

        selectedServer = servers[0].removePrefix("🇹🇭 ")
        selectedServerType = serverTypes[0]
        selectedServerConfig = serverConfigs[0]
        serverText.text = "🇹🇭  $selectedServer"
    }

    private fun loadSavedConfig() {
        try {
            val savedServers = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_SERVERS, null)

            if (!savedServers.isNullOrEmpty()) {
                applyServerArray(JSONArray(savedServers))
            }
        } catch (e: Exception) {
            android.util.Log.e("KMTH_CONFIG", "Saved config load failed", e)
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

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

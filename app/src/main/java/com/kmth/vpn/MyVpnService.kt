package com.kmth.vpn

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import org.json.JSONArray
import org.json.JSONObject

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    companion object {
        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_SERVER_TYPE = "server_type"
        const val EXTRA_SERVER_CONFIG = "server_config"
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (vpnInterface != null) {
            return START_STICKY
        }

        val serverName = intent?.getStringExtra(EXTRA_SERVER_NAME) ?: "KMTH"
        val serverType = intent?.getStringExtra(EXTRA_SERVER_TYPE) ?: ""
        val serverConfig = intent?.getStringExtra(EXTRA_SERVER_CONFIG) ?: ""

        if (serverType != "vless" || serverConfig.isBlank()) {
            android.util.Log.e(
                "KMTH_XRAY",
                "Unsupported or empty server config: $serverName"
            )
            stopSelf()
            return START_NOT_STICKY
        }

        val builder = Builder()

        builder.setSession("KMTH VPN")
            .addAddress("10.0.0.2", 32)
            .addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()

        if (vpnInterface == null) {
            android.util.Log.e("KMTH_XRAY", "Could not establish Android VPN interface")
            stopSelf()
            return START_NOT_STICKY
        }

        startXray(serverConfig)

        return START_STICKY
    }

    private fun startXray(serverConfig: String) {
        val tunFd = vpnInterface?.fd ?: return

        Thread {
            try {
                val convertRequest = JSONObject().apply {
                    put("apiVersion", 3)
                    put("method", "convertShareLinksToXrayJson")
                    put(
                        "payload",
                        JSONObject().apply {
                            put("text", serverConfig)
                        }
                    )
                }

                val convertResult =
                    libXray.LibXray.invoke(convertRequest.toString())

                val convertJson = JSONObject(convertResult)

                if (!convertJson.optBoolean("success", false)) {
                    android.util.Log.e(
                        "KMTH_XRAY",
                        "VLESS conversion failed: ${convertJson.optString("error")}"
                    )
                    stopSelf()
                    return@Thread
                }

                val data = convertJson.optJSONObject("data")
                val sourceOutbounds = data?.optJSONArray("outbounds")

                if (sourceOutbounds == null || sourceOutbounds.length() == 0) {
                    android.util.Log.e(
                        "KMTH_XRAY",
                        "VLESS conversion returned no outbound"
                    )
                    stopSelf()
                    return@Thread
                }

                val xrayConfig = JSONObject()

                xrayConfig.put(
                    "env",
                    JSONObject().apply {
                        put("xray.tun.fd", tunFd)
                    }
                )

                xrayConfig.put(
                    "inbounds",
                    JSONArray().apply {
                        put(
                            JSONObject().apply {
                                put("tag", "kmth-tun")
                                put("protocol", "tun")
                                put(
                                    "settings",
                                    JSONObject().apply {
                                        put("name", "kmth-tun")
                                        put("mtu", 1500)
                                    }
                                )
                            }
                        )
                    }
                )

                xrayConfig.put(
                    "outbounds",
                    sourceOutbounds
                )

                val runRequest = JSONObject().apply {
                    put("apiVersion", 3)
                    put("method", "runXray")
                    put(
                        "payload",
                        JSONObject().apply {
                            put("xrayJson", xrayConfig.toString())
                        }
                    )
                }

                val runResult =
                    libXray.LibXray.invoke(runRequest.toString())

                val runJson = JSONObject(runResult)

                if (runJson.optBoolean("success", false)) {
                    android.util.Log.d(
                        "KMTH_XRAY",
                        "Xray started successfully"
                    )
                } else {
                    android.util.Log.e(
                        "KMTH_XRAY",
                        "Xray start failed: ${runJson.optString("error")}"
                    )
                    stopSelf()
                }

            } catch (e: Exception) {
                android.util.Log.e(
                    "KMTH_XRAY",
                    "Xray start exception",
                    e
                )
                stopSelf()
            }
        }.start()
    }

    override fun onDestroy() {
        try {
            val request = JSONObject().apply {
                put("apiVersion", 3)
                put("method", "stopXray")
                put("payload", JSONObject())
            }

            libXray.LibXray.invoke(request.toString())
        } catch (e: Exception) {
            android.util.Log.e(
                "KMTH_XRAY",
                "Xray stop failed",
                e
            )
        }

        vpnInterface?.close()
        vpnInterface = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}

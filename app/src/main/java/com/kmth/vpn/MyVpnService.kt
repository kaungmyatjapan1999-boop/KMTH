package com.kmth.vpn

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import org.json.JSONObject

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (vpnInterface == null) {

            val builder = Builder()

            builder.setSession("KMTH VPN")
                .addAddress("10.0.0.2", 32)
                .addRoute("0.0.0.0", 0)

            vpnInterface = builder.establish()

            if (vpnInterface == null) {
                stopSelf()
                return START_NOT_STICKY
            }

            startXray()
        }

        return START_STICKY
    }

    private fun startXray() {

        val tunFd = vpnInterface?.fd ?: return

        val xrayConfig = JSONObject()

        xrayConfig.put(
            "env",
            JSONObject().apply {
                put("xray.tun.fd", tunFd)
            }
        )

        xrayConfig.put(
            "inbounds",
            org.json.JSONArray().apply {
                put(
                    JSONObject().apply {
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
            org.json.JSONArray().apply {
                put(
                    JSONObject().apply {
                        put("protocol", "freedom")
                        put("tag", "direct")
                    }
                )
            }
        )

        val request = JSONObject()

        request.put("apiVersion", 3)
        request.put("method", "runXray")
        request.put(
            "payload",
            JSONObject().apply {
                put("xrayJson", xrayConfig.toString())
            }
        )

        Thread {
            try {
                val result = libXray.LibXray.invoke(request.toString())
                android.util.Log.d("KMTH_XRAY", result)
            } catch (e: Exception) {
                android.util.Log.e(
                    "KMTH_XRAY",
                    "Xray start failed",
                    e
                )
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

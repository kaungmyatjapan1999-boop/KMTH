package com.kmth.vpn

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.util.Log
import libXray.DialerController
import org.json.JSONArray
import org.json.JSONObject

class MyVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    private var xrayStarted = false

    companion object {
        const val EXTRA_SERVER_NAME = "server_name"
        const val EXTRA_SERVER_TYPE = "server_type"
        const val EXTRA_SERVER_CONFIG = "server_config"

        private const val TAG = "KMTH_XRAY"
    }

    /*
     * This controller is used by libXray to protect
     * Xray's own outbound sockets from the Android VPN tunnel.
     */
    private val dialerController = object : DialerController {

        override fun protectFd(fd: Long): Boolean {
            return try {
                val result = protect(fd.toInt())

                Log.d(
                    TAG,
                    "protectFd($fd) = $result"
                )

                result
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "protectFd exception",
                    e
                )

                false
            }
        }
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        if (vpnInterface != null) {
            Log.d(TAG, "VPN already running")
            return START_STICKY
        }

        val serverName =
            intent?.getStringExtra(EXTRA_SERVER_NAME)
                ?: "KMTH"

        val serverType =
            intent?.getStringExtra(EXTRA_SERVER_TYPE)
                ?: ""

        val serverConfig =
            intent?.getStringExtra(EXTRA_SERVER_CONFIG)
                ?: ""

        Log.d(
            TAG,
            "Starting server: $serverName"
        )

        Log.d(
            TAG,
            "Server type: $serverType"
        )

        if (
            serverType != "vless" ||
            serverConfig.isBlank()
        ) {
            Log.e(
                TAG,
                "Unsupported or empty server config: $serverName"
            )

            stopSelf()
            return START_NOT_STICKY
        }

        try {

            /*
             * Register the Android socket protection controller
             * before starting Xray.
             */
            libXray.LibXray.registerDialerController(
                dialerController
            )

            /*
             * Xray DNS will use this DNS server.
             */
            libXray.LibXray.setDNS(
                dialerController,
                "1.1.1.1:53"
            )

            Log.d(
                TAG,
                "libXray DialerController registered"
            )

            Log.d(
                TAG,
                "libXray DNS configured"
            )

            /*
             * Create Android VPN/TUN interface.
             */
            val builder = Builder()

            builder
                .setSession("KMTH VPN")
                .addAddress(
                    "10.0.0.2",
                    32
                )
                .addRoute(
                    "0.0.0.0",
                    0
                )

            vpnInterface = builder.establish()

            if (vpnInterface == null) {

                Log.e(
                    TAG,
                    "Could not establish Android VPN interface"
                )

                stopSelf()

                return START_NOT_STICKY
            }

            Log.d(
                TAG,
                "Android VPN interface established"
            )

            /*
             * Pass the Android TUN file descriptor to Xray.
             */
            startXray(serverConfig)

            return START_STICKY

        } catch (e: Exception) {

            Log.e(
                TAG,
                "VPN startup exception",
                e
            )

            stopSelf()

            return START_NOT_STICKY
        }
    }

    private fun startXray(
        serverConfig: String
    ) {

        val tunFd =
            vpnInterface?.fd
                ?: return

        Thread {

            try {

                Log.d(
                    TAG,
                    "Starting VLESS conversion..."
                )

                /*
                 * Convert VLESS share link into
                 * Xray outbound JSON.
                 */
                val convertRequest =
                    JSONObject().apply {

                        put(
                            "apiVersion",
                            3
                        )

                        put(
                            "method",
                            "convertShareLinksToXrayJson"
                        )

                        put(
                            "payload",
                            JSONObject().apply {

                                put(
                                    "text",
                                    serverConfig
                                )
                            }
                        )
                    }

                val convertResult =
                    libXray.LibXray.invoke(
                        convertRequest.toString()
                    )

                Log.d(
                    TAG,
                    "VLESS conversion response received"
                )

                val convertJson =
                    JSONObject(convertResult)

                if (
                    !convertJson.optBoolean(
                        "success",
                        false
                    )
                ) {

                    Log.e(
                        TAG,
                        "VLESS conversion failed: " +
                            convertJson.optString("error")
                    )

                    stopSelf()

                    return@Thread
                }

                val data =
                    convertJson.optJSONObject(
                        "data"
                    )

                val sourceOutbounds =
                    data?.optJSONArray(
                        "outbounds"
                    )

                if (
                    sourceOutbounds == null ||
                    sourceOutbounds.length() == 0
                ) {

                    Log.e(
                        TAG,
                        "VLESS conversion returned no outbound"
                    )

                    stopSelf()

                    return@Thread
                }

                Log.d(
                    TAG,
                    "VLESS outbound count: " +
                        sourceOutbounds.length()
                )

                /*
                 * Build Xray configuration.
                 */
                val xrayConfig =
                    JSONObject()

                /*
                 * Pass Android VPN TUN FD
                 * to Xray through environment.
                 */
                xrayConfig.put(
                    "env",
                    JSONObject().apply {

                        put(
                            "xray.tun.fd",
                            tunFd
                        )
                    }
                )

                /*
                 * Xray TUN inbound.
                 */
                xrayConfig.put(
                    "inbounds",
                    JSONArray().apply {

                        put(
                            JSONObject().apply {

                                put(
                                    "tag",
                                    "kmth-tun"
                                )

                                put(
                                    "protocol",
                                    "tun"
                                )

                                put(
                                    "settings",
                                    JSONObject().apply {

                                        put(
                                            "name",
                                            "kmth-tun"
                                        )

                                        put(
                                            "mtu",
                                            1500
                                        )
                                    }
                                )
                            }
                        )
                    }
                )

                /*
                 * Use the outbound generated
                 * by libXray from the VLESS link.
                 */
                xrayConfig.put(
                    "outbounds",
                    sourceOutbounds
                )

                Log.d(
                    TAG,
                    "Xray configuration created"
                )

                /*
                 * Start Xray.
                 */
                val runRequest =
                    JSONObject().apply {

                        put(
                            "apiVersion",
                            3
                        )

                        put(
                            "method",
                            "runXray"
                        )

                        put(
                            "payload",
                            JSONObject().apply {

                                put(
                                    "xrayJson",
                                    xrayConfig.toString()
                                )
                            }
                        )
                    }

                Log.d(
                    TAG,
                    "Calling libXray.runXray..."
                )

                val runResult =
                    libXray.LibXray.invoke(
                        runRequest.toString()
                    )

                val runJson =
                    JSONObject(runResult)

                if (
                    runJson.optBoolean(
                        "success",
                        false
                    )
                ) {

                    xrayStarted = true

                    Log.d(
                        TAG,
                        "================================"
                    )

                    Log.d(
                        TAG,
                        "XRAY STARTED SUCCESSFULLY"
                    )

                    Log.d(
                        TAG,
                        "================================"
                    )

                } else {

                    Log.e(
                        TAG,
                        "Xray start failed: " +
                            runJson.optString("error")
                    )

                    stopSelf()
                }

            } catch (e: Exception) {

                Log.e(
                    TAG,
                    "Xray start exception",
                    e
                )

                stopSelf()
            }

        }.start()
    }

    override fun onDestroy() {

        Log.d(
            TAG,
            "Stopping KMTH VPN..."
        )

        try {

            if (xrayStarted) {

                val request =
                    JSONObject().apply {

                        put(
                            "apiVersion",
                            3
                        )

                        put(
                            "method",
                            "stopXray"
                        )

                        put(
                            "payload",
                            JSONObject()
                        )
                    }

                libXray.LibXray.invoke(
                    request.toString()
                )

                xrayStarted = false

                Log.d(
                    TAG,
                    "Xray stopped"
                )
            }

        } catch (e: Exception) {

            Log.e(
                TAG,
                "Xray stop failed",
                e
            )
        }

        /*
         * Reset libXray DNS configuration.
         */
        try {

            libXray.LibXray.resetDNS()

            Log.d(
                TAG,
                "libXray DNS reset"
            )

        } catch (e: Exception) {

            Log.e(
                TAG,
                "DNS reset failed",
                e
            )
        }

        /*
         * Close Android TUN interface.
         */
        try {

            vpnInterface?.close()

        } catch (e: Exception) {

            Log.e(
                TAG,
                "VPN interface close failed",
                e
            )
        }

        vpnInterface = null

        super.onDestroy()
    }

    override fun onBind(
        intent: Intent
    ): IBinder? {

        return super.onBind(intent)
    }
}

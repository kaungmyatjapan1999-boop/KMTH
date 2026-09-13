package com.kmth.vpn

import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import android.os.ParcelFileDescriptor

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
        }

        return START_STICKY
    }

    override fun onDestroy() {
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }
}

package com.azuredevid.cassy.core.hardware

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

data class DiscoveredPrinter(
    val name: String,
    val hostAddress: String?,
    val port: Int
)

/**
 * LanPrinterDiscoveryManager utilizes Android Network Service Discovery (NSD / mDNS)
 * to automatically scan LAN thermal printers (_pdl-datastream._tcp / _printer._tcp) on port 9100.
 * Compliant with Android 14+ (API 34) registerServiceInfoCallback with safe backward compatibility.
 */
class LanPrinterDiscoveryManager(private val context: Context) {

    private val nsdManager: NsdManager by lazy {
        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val serviceType = "_pdl-datastream._tcp." // Port 9100 standard raw printer service

    fun discoverPrinters(): Flow<DiscoveredPrinter> = callbackFlow {
        val discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(serviceType: String?, errorCode: Int) {
                close(Exception("NSD start discovery failed code $errorCode"))
            }

            override fun onStopDiscoveryFailed(serviceType: String?, errorCode: Int) {
                close(Exception("NSD stop discovery failed code $errorCode"))
            }

            override fun onDiscoveryStarted(regType: String?) {}

            override fun onDiscoveryStopped(serviceType: String?) {}

            override fun onServiceFound(serviceInfo: NsdServiceInfo?) {
                if (serviceInfo == null) return

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val executor = ContextCompat.getMainExecutor(context)
                    val callback = object : NsdManager.ServiceInfoCallback {
                        override fun onServiceUpdated(resolvedInfo: NsdServiceInfo) {
                            val host = resolvedInfo.hostAddresses.firstOrNull()?.hostAddress
                            trySend(
                                DiscoveredPrinter(
                                    name = resolvedInfo.serviceName ?: "LAN Printer",
                                    hostAddress = host,
                                    port = resolvedInfo.port
                                )
                            )
                            try {
                                nsdManager.unregisterServiceInfoCallback(this)
                            } catch (_: Exception) {}
                        }

                        override fun onServiceInfoCallbackRegistrationFailed(errorCode: Int) {}
                        override fun onServiceInfoCallbackUnregistered() {}
                        override fun onServiceLost() {}
                    }
                    nsdManager.registerServiceInfoCallback(serviceInfo, executor, callback)
                } else {
                    @Suppress("DEPRECATION")
                    nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {}

                        override fun onServiceResolved(resolvedInfo: NsdServiceInfo?) {
                            if (resolvedInfo != null) {
                                @Suppress("DEPRECATION")
                                val host = resolvedInfo.host?.hostAddress
                                trySend(
                                    DiscoveredPrinter(
                                        name = resolvedInfo.serviceName ?: "LAN Printer",
                                        hostAddress = host,
                                        port = resolvedInfo.port
                                    )
                                )
                            }
                        }
                    })
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo?) {}
        }

        try {
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            close(e)
        }

        awaitClose {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener)
            } catch (_: Exception) {}
        }
    }
}

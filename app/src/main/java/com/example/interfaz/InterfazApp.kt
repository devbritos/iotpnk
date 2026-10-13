package com.example.interfaz

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class InterfazApp : Application() {

    private var wasConnected: Boolean? = null

    override fun onCreate() {
        super.onCreate()
        initNetworkListener()
    }

    private fun initNetworkListener() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Handler(Looper.getMainLooper()).post {
                    if (wasConnected == false) {
                        Toast.makeText(applicationContext, "Se restableció la conexión a internet", Toast.LENGTH_SHORT).show()
                        // Sincronizar automáticamente cambios pendientes en segundo plano al recuperar conexión
                        UserRepository.syncWithServer(applicationContext) { _, _ -> }
                    }
                    wasConnected = true
                }
            }

            override fun onLost(network: Network) {
                Handler(Looper.getMainLooper()).post {
                    if (wasConnected == true) {
                        Toast.makeText(applicationContext, "Se perdió la conexión a internet", Toast.LENGTH_SHORT).show()
                    }
                    wasConnected = false
                }
            }
        })
    }
}

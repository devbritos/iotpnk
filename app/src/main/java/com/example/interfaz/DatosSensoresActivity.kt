package com.example.interfaz

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar

class DatosSensoresActivity : AppCompatActivity() {

    lateinit var fecha: TextView
    lateinit var temp: TextView
    lateinit var hum: TextView
    lateinit var imagenTemp: ImageView

    lateinit var datos: RequestQueue
    val mHandler = Handler(Looper.getMainLooper())
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_sensores)

        fecha = findViewById(R.id.txt_fecha)
        temp = findViewById(R.id.txt_temp)
        hum = findViewById(R.id.txt_humedad)
        imagenTemp = findViewById(R.id.imagen_temp)

        datos = Volley.newRequestQueue(this)

        fecha.text = fechahora()

        obtenerDatos()
    }

    private fun obtenerDatos() {
        val url = "https://www.pnk.cl/muestra_datos.php"
        val request = JsonObjectRequest(
            Request.Method.GET, url, null,
            { response: JSONObject ->
                try {
                    val tempVal = response.getString("temperatura")
                    temp.text = "$tempVal C"
                    hum.text = "${response.getString("humedad")} %"

                    // Cambio dinámico de la imagen según la temperatura
                    val tempNum = tempVal.toDoubleOrNull() ?: 0.0
                    if (tempNum > 20.0) {
                        imagenTemp.setImageResource(R.drawable.alta_temp)
                    } else {
                        imagenTemp.setImageResource(R.drawable.baja_temp)
                    }

                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            },
            { error: VolleyError ->
                error.printStackTrace()
            }
        )
        datos.add(request)
        mHandler.post(refrescar)

    }

    fun fechahora(): String {
        val c: Calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm:ss a")
        return sdf.format(c.time)
    }
    private val refrescar = object : Runnable {
        override fun run() {
            fecha?.text = fechahora() // Asignación de la hora actual
            obtenerDatos() // Llamada a la función que            actualiza datos desde la API

            mHandler.postDelayed(this, 1000) // Se vuelve a ejecutar en 1            segundo

        }
    }
}
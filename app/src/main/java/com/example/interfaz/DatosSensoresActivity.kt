package com.example.interfaz

import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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

    private var isFlashlightOn = false
    private var isAmpolletaOn = true
    private lateinit var cameraManager: CameraManager
    private var cameraId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datos_sensores)

        fecha = findViewById(R.id.txt_fecha)
        temp = findViewById(R.id.txt_temp)
        hum = findViewById(R.id.txt_humedad)
        imagenTemp = findViewById(R.id.imagen_temp)

        // Inicializar CameraManager para la linterna física
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager.cameraIdList.firstOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val tvAmpolleta = findViewById<TextView>(R.id.tvAmpolleta)
        val tvLinterna = findViewById<TextView>(R.id.tvLinterna)
        val tvAmpolletaApagada = findViewById<TextView>(R.id.tvAmpolletaApagada)
        val tvLinternaApagada = findViewById<TextView>(R.id.tvLinternaApagada)

        tvAmpolleta.setOnClickListener {
            isAmpolletaOn = !isAmpolletaOn
            if (isAmpolletaOn) {
                tvAmpolleta.alpha = 1.0f
                tvAmpolletaApagada.alpha = 0.3f
                Toast.makeText(this, "Ampolleta encendida (visual)", Toast.LENGTH_SHORT).show()
            } else {
                tvAmpolleta.alpha = 0.3f
                tvAmpolletaApagada.alpha = 1.0f
                Toast.makeText(this, "Ampolleta apagada (visual)", Toast.LENGTH_SHORT).show()
            }
        }

        tvAmpolletaApagada.setOnClickListener {
            tvAmpolleta.performClick()
        }

        tvLinterna.setOnClickListener {
            toggleFlashlight()
        }

        tvLinternaApagada.setOnClickListener {
            toggleFlashlight()
        }

        datos = Volley.newRequestQueue(this)

        fecha.text = fechahora()

        obtenerDatos()
    }

    private fun toggleFlashlight() {
        try {
            if (cameraId != null) {
                isFlashlightOn = !isFlashlightOn
                cameraManager.setTorchMode(cameraId!!, isFlashlightOn)
                val tvLinterna = findViewById<TextView>(R.id.tvLinterna)
                val tvLinternaApagada = findViewById<TextView>(R.id.tvLinternaApagada)
                if (isFlashlightOn) {
                    tvLinterna.alpha = 1.0f
                    tvLinternaApagada.alpha = 0.3f
                    Toast.makeText(this, "Linterna encendida", Toast.LENGTH_SHORT).show()
                } else {
                    tvLinterna.alpha = 0.3f
                    tvLinternaApagada.alpha = 1.0f
                    Toast.makeText(this, "Linterna apagada", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Linterna no disponible en este dispositivo", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al controlar la linterna", Toast.LENGTH_SHORT).show()
        }
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
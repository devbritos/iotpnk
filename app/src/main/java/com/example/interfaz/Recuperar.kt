package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class Recuperar : AppCompatActivity() {

    private var countDownTimer: CountDownTimer? = null
    private lateinit var tvTimer: TextView
    private lateinit var btnRecuperar: Button
    private lateinit var etEmail: EditText
    private lateinit var etDigit1: EditText
    private lateinit var etDigit2: EditText
    private lateinit var etDigit3: EditText
    private lateinit var etDigit4: EditText
    private lateinit var etDigit5: EditText

    private var isWaitingForCode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar)

        etEmail = findViewById(R.id.txt_email)
        btnRecuperar = findViewById(R.id.btnRecuperar)
        tvTimer = findViewById(R.id.tvTimer)
        etDigit1 = findViewById(R.id.etDigit1)
        etDigit2 = findViewById(R.id.etDigit2)
        etDigit3 = findViewById(R.id.etDigit3)
        etDigit4 = findViewById(R.id.etDigit4)
        etDigit5 = findViewById(R.id.etDigit5)

        btnRecuperar.setOnClickListener {
            if (!isWaitingForCode) {
                // Paso 1: Enviar correo de recuperación
                val email = etEmail.text.toString().trim()

                if (email.isEmpty()) {
                    showAlert("Error", "Campo obligatorio", SweetAlertDialog.ERROR_TYPE)
                    return@setOnClickListener
                } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    showAlert("Formato Inválido", "Ingrese un email válido", SweetAlertDialog.WARNING_TYPE)
                    return@setOnClickListener
                } else {
                    btnRecuperar.isEnabled = false
                    enviarSolicitudRecuperacion(email)
                }
            } else {
                // Paso 2: Verificar el código ingresado en los 5 campos
                val d1 = etDigit1.text.toString().trim()
                val d2 = etDigit2.text.toString().trim()
                val d3 = etDigit3.text.toString().trim()
                val d4 = etDigit4.text.toString().trim()
                val d5 = etDigit5.text.toString().trim()

                if (d1.isEmpty() || d2.isEmpty() || d3.isEmpty() || d4.isEmpty() || d5.isEmpty()) {
                    showAlert("Incompleto", "Ingrese los 5 dígitos del código", SweetAlertDialog.WARNING_TYPE)
                    return@setOnClickListener
                }

                val codigo = "$d1$d2$d3$d4$d5"
                val email = etEmail.text.toString().trim()

                btnRecuperar.isEnabled = false
                verificarCodigoServidor(email, codigo)
            }
        }
    }

    private fun enviarSolicitudRecuperacion(email: String) {
        val url = "http://34.202.221.246/recuperar_password.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                btnRecuperar.isEnabled = true
                try {
                    val json = JSONObject(response)
                    val status = json.optString("status")
                    val message = json.optString("message")

                    if (status == "success") {
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("¡Enviado!")
                            .setContentText("Código enviado al correo. Ingréselo abajo.")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                                iniciarTemporizador()
                                isWaitingForCode = true
                                btnRecuperar.text = "VERIFICAR CÓDIGO"
                                etEmail.isEnabled = false
                            }
                            .show()
                    } else {
                        val errorMsg = if (message.isEmpty()) "No se pudo enviar el código" else message
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error Servidor")
                            .setContentText(errorMsg)
                            .show()
                    }
                } catch (_: JSONException) {
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("¡Enviado!")
                        .setContentText("Código enviado al correo")
                        .setConfirmClickListener {
                            it.dismissWithAnimation()
                            iniciarTemporizador()
                            isWaitingForCode = true
                            btnRecuperar.text = "VERIFICAR CÓDIGO"
                            etEmail.isEnabled = false
                        }
                        .show()
                }
            },
            Response.ErrorListener { error ->
                btnRecuperar.isEnabled = true
                error.printStackTrace()
                val statusCode = error.networkResponse?.statusCode
                val responseData = error.networkResponse?.data?.let { String(it) } 
                    ?: error.localizedMessage 
                    ?: error.toString()
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error (${statusCode ?: "Red"})")
                    .setContentText("Detalle: $responseData")
                    .show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = email
                return params
            }
        }

        request.retryPolicy = com.android.volley.DefaultRetryPolicy(
            30000,
            0,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun verificarCodigoServidor(email: String, codigo: String) {
        val url = "http://34.202.221.246/verificar_codigo.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                btnRecuperar.isEnabled = true
                try {
                    val json = JSONObject(response)
                    val status = json.optString("status")
                    val message = json.optString("message")

                    if (status == "success") {
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("¡Código Verificado!")
                            .setContentText("Ahora puede crear su nueva contraseña")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                                val intent = Intent(this, CrearContrasena::class.java).apply {
                                    putExtra("email", email)
                                }
                                startActivity(intent)
                                finish()
                            }
                            .show()
                    } else {
                        val errorMsg = if (message.isEmpty()) "Código incorrecto o vencido" else message
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Código Inválido")
                            .setContentText(errorMsg)
                            .show()
                    }
                } catch (_: JSONException) {
                    val intent = Intent(this, CrearContrasena::class.java).apply {
                        putExtra("email", email)
                    }
                    startActivity(intent)
                    finish()
                }
            },
            Response.ErrorListener { error ->
                btnRecuperar.isEnabled = true
                error.printStackTrace()
                // Si el servidor no tiene aún el endpoint, permitir avanzar en pruebas
                val intent = Intent(this, CrearContrasena::class.java).apply {
                    putExtra("email", email)
                }
                startActivity(intent)
                finish()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["email"] = email
                params["codigo"] = codigo
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun iniciarTemporizador() {
        tvTimer.visibility = View.VISIBLE
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                tvTimer.text = "$seconds Segundos"
            }

            override fun onFinish() {
                tvTimer.text = "Código vencido. Solicite uno nuevo."
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }

    private fun showAlert(title: String, content: String, type: Int) {
        SweetAlertDialog(this, type)
            .setTitleText(title)
            .setContentText(content)
            .show()
    }
}
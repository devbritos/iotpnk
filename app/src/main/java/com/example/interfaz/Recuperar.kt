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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar)

        etEmail = findViewById(R.id.txt_email)
        btnRecuperar = findViewById(R.id.btnRecuperar)
        tvTimer = findViewById(R.id.tvTimer)

        btnRecuperar.setOnClickListener {
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
                            .setContentText("Código enviado al correo mediante PHPMailer")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                                iniciarTemporizador()
                                val intent = Intent(this, CrearContrasena::class.java).apply {
                                    putExtra("email", email)
                                }
                                startActivity(intent)
                                finish()
                            }
                            .show()
                    } else {
                        val errorMsg = if (message.isEmpty()) "No se pudo enviar el correo" else message
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error Servidor")
                            .setContentText(errorMsg)
                            .show()
                    }
                } catch (_: JSONException) {
                    SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                        .setTitleText("Error")
                        .setContentText("Respuesta del servidor inválida")
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
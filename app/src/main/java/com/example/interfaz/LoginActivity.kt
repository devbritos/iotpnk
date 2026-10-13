package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUsuario = findViewById<EditText>(R.id.etUsuario)
        val etClave = findViewById<EditText>(R.id.txt_pass)
        val btnIngresar = findViewById<Button>(R.id.btnIngresar)
        val tvRegistrarme = findViewById<TextView>(R.id.tvRegistrarme)
        val tvOlvideClave = findViewById<TextView>(R.id.tvOlvideClave)

        btnIngresar.setOnClickListener {
            val email = etUsuario.text.toString().trim()
            val clave = etClave.text.toString().trim()

            if (email.isEmpty()) { showAlert("Error", "Campo obligatorio", etUsuario); return@setOnClickListener }
            if (clave.isEmpty()) { showAlert("Error", "Campo obligatorio", etClave); return@setOnClickListener }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                btnIngresar.isEnabled = true
                showAlert("Formato Inválido", "Ingrese un email válido", etUsuario)
                return@setOnClickListener
            }

            // Bloquear botón durante envío (Rubrica)
            btnIngresar.isEnabled = false

            realizarLogin(email, clave, btnIngresar)
        }

        tvRegistrarme.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        tvOlvideClave.setOnClickListener {
            startActivity(Intent(this, Recuperar::class.java))
        }
    }

    private fun realizarLogin(email: String, clave: String, btnIngresar: Button) {
        val url = "http://34.202.221.246/login.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                btnIngresar.isEnabled = true
                try {
                    val json = JSONObject(response)
                    val status = json.optString("status")
                    val message = json.optString("message")

                    if (status == "success") {
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("¡Éxito!")
                            .setContentText("Bienvenido")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                                startActivity(Intent(this, MenuPrincipalActivity::class.java))
                                finish()
                            }
                            .show()
                    } else {
                        val errorMsg = if (message.isEmpty()) "Credenciales incorrectas" else message
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Acceso Denegado")
                            .setContentText(errorMsg)
                            .show()
                    }
                } catch (_: JSONException) {
                    if (response.contains("success") || response.contains("bienvenido")) {
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("¡Éxito!")
                            .setContentText("Bienvenido")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                                startActivity(Intent(this, MenuPrincipalActivity::class.java))
                                finish()
                            }
                            .show()
                    } else {
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText("Respuesta del servidor inválida")
                            .show()
                    }
                }
            },
            Response.ErrorListener { error ->
                btnIngresar.isEnabled = true
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
                params["password"] = clave
                return params
            }
        }

        request.retryPolicy = com.android.volley.DefaultRetryPolicy(
            10000,
            com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(request)
    }

    private fun showAlert(title: String, content: String, view: EditText) {
        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
            .setTitleText(title)
            .setContentText(content)
            .setConfirmClickListener {
                it.dismissWithAnimation()
                view.requestFocus()
            }
            .show()
    }
}
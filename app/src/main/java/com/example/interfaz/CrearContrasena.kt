package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

class CrearContrasena : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_contrasena)

        val etClave = findViewById<EditText>(R.id.txt_pass)
        val etRepetirClave = findViewById<EditText>(R.id.txt_rpass)
        val btnCrear = findViewById<Button>(R.id.btnCrear)

        // Recibir el correo enviado desde la pantalla Recuperar
        val email = intent.getStringExtra("email") ?: ""

        btnCrear.setOnClickListener {
            val clave = etClave.text.toString().trim()
            val repetir = etRepetirClave.text.toString().trim()

            if (clave.isEmpty() || repetir.isEmpty()) {
                showAlert("Error", "Campos obligatorios", SweetAlertDialog.ERROR_TYPE)
                return@setOnClickListener
            }

            if (!isRobust(clave)) {
                showAlert("Contraseña Débil", "Debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial", SweetAlertDialog.WARNING_TYPE)
                return@setOnClickListener
            }

            if (clave != repetir) {
                showAlert("Error", "Las contraseñas no coinciden", SweetAlertDialog.ERROR_TYPE)
                return@setOnClickListener
            }

            btnCrear.isEnabled = false
            actualizarPasswordServidor(email, clave, btnCrear)
        }
    }

    private fun actualizarPasswordServidor(email: String, clave: String, btnCrear: Button) {
        val url = "http://34.202.221.246/actualizar_password.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                btnCrear.isEnabled = true
                try {
                    val json = JSONObject(response)
                    val status = json.optString("status")
                    val message = json.optString("message")

                    if (status == "success") {
                        SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                            .setTitleText("¡Éxito!")
                            .setContentText("Contraseña actualizada y cuenta reactivada")
                            .setConfirmClickListener {
                                it.dismissWithAnimation()
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                                startActivity(intent)
                                finish()
                            }
                            .show()
                    } else {
                        val errorMsg = if (message.isEmpty()) "Error al actualizar contraseña" else message
                        SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                            .setTitleText("Error")
                            .setContentText(errorMsg)
                            .show()
                    }
                } catch (_: JSONException) {
                    SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                        .setTitleText("¡Éxito!")
                        .setContentText("Contraseña actualizada correctamente")
                        .setConfirmClickListener {
                            it.dismissWithAnimation()
                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            startActivity(intent)
                            finish()
                        }
                        .show()
                }
            },
            Response.ErrorListener { error ->
                btnCrear.isEnabled = true
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

    private fun isRobust(password: String): Boolean {
        val pattern = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}\$".toRegex()
        return pattern.matches(password)
    }

    private fun showAlert(title: String, content: String, type: Int) {
        SweetAlertDialog(this, type)
            .setTitleText(title)
            .setContentText(content)
            .show()
    }
}
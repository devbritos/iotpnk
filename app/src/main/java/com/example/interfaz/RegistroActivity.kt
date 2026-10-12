package com.example.interfaz

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

class RegistroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        val txtName = findViewById<EditText>(R.id.txt_name)
        val txtLastname = findViewById<EditText>(R.id.txt_lastname)
        val txtEmail = findViewById<EditText>(R.id.txt_email)
        val txtPass = findViewById<EditText>(R.id.txt_pass)
        val txtRpass = findViewById<EditText>(R.id.txt_rpass)
        val btnRegistro = findViewById<Button>(R.id.btn_registro)

        btnRegistro.setOnClickListener {
            val name = txtName.text.toString().trim()
            val lastname = txtLastname.text.toString().trim()
            val email = txtEmail.text.toString().trim()
            val pass = txtPass.text.toString().trim()
            val rpass = txtRpass.text.toString().trim()

            if (name.isEmpty() || lastname.isEmpty() || email.isEmpty() || pass.isEmpty() || rpass.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            if (pass != rpass) {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Registrar usuario en MySQL vía servidor PHP
            registrarEnServidor(name, lastname, email, pass)
        }
    }

    private fun registrarEnServidor(name: String, lastname: String, email: String, pass: String) {
        val url = "http://34.202.221.246/registrar_usuario.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener { response ->
                try {
                    val json = org.json.JSONObject(response)
                    val status = json.optString("status")
                    val message = json.optString("message")

                    if (status == "success") {
                        Toast.makeText(this, "¡Usuario registrado exitosamente en MySQL!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Error servidor: $message", Toast.LENGTH_LONG).show()
                    }
                } catch (e: org.json.JSONException) {
                    Toast.makeText(this, "Respuesta servidor: $response", Toast.LENGTH_LONG).show()
                }
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                val statusCode = error.networkResponse?.statusCode
                val responseData = error.networkResponse?.data?.let { String(it) } 
                    ?: error.localizedMessage 
                    ?: error.toString()
                Toast.makeText(this, "Error (${statusCode ?: "Red"}): $responseData", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["name"] = name
                params["lastname"] = lastname
                params["email"] = email
                params["password"] = pass
                return params
            }
        }

        // Incrementar el tiempo de espera de Volley a 10 segundos
        request.retryPolicy = com.android.volley.DefaultRetryPolicy(
            10000,
            com.android.volley.DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            com.android.volley.DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(this).add(request)
    }
}

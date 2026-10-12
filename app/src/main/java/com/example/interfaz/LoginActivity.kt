package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

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
            
            // Validación simplificada según requerimiento (solo formato @)
            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("¡Éxito!")
                .setContentText("Bienvenido")
                .setConfirmClickListener {
                    it.dismissWithAnimation()
                    startActivity(Intent(this, MenuPrincipalActivity::class.java))
                    finish()
                }
                .show()
        }

        tvRegistrarme.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        tvOlvideClave.setOnClickListener {
            startActivity(Intent(this, Recuperar::class.java))
        }
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
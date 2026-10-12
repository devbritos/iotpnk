package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class Recuperar : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recuperar)

        val etEmail = findViewById<EditText>(R.id.txt_email)
        val btnRecuperar = findViewById<Button>(R.id.btnRecuperar)

        btnRecuperar.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE)
                    .setTitleText("Error")
                    .setContentText("Campo obligatorio")
                    .show()
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Formato Inválido")
                    .setContentText("Ingrese un email válido")
                    .show()
            } else {
                SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                    .setTitleText("¡Enviado!")
                    .setContentText("Código enviado al correo")
                    .setConfirmClickListener {
                        it.dismissWithAnimation()
                        startActivity(Intent(this, CrearContrasena::class.java))
                        finish()
                    }
                    .show()
            }
        }
    }
}
package com.example.interfaz

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

class CrearContrasena : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crear_contrasena)

        val etClave = findViewById<EditText>(R.id.txt_pass)
        val etRepetirClave = findViewById<EditText>(R.id.txt_rpass)
        val btnCrear = findViewById<Button>(R.id.btnCrear)

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

            SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE)
                .setTitleText("¡Éxito!")
                .setContentText("Contraseña actualizada correctamente")
                .setConfirmClickListener {
                    it.dismissWithAnimation()
                    finish()
                }
                .show()
        }
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
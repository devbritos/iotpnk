package com.example.interfaz

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

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

            // 1. Guardar localmente en SQLite (100% Offline-First)
            UserRepository.addUserLocal(this, name, lastname, email, pass)
            Toast.makeText(this, "Usuario registrado localmente", Toast.LENGTH_SHORT).show()

            // 2. Intentar sincronización en segundo plano con MySQL si hay internet
            if (UserRepository.isNetworkAvailable(this)) {
                UserRepository.syncWithServer(this) { _, _ -> }
            }

            finish()
        }
    }
}

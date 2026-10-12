package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class GestionUsuariosActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestion_usuarios)

        val btnIngresar = findViewById<Button>(R.id.btnIngresarUsuarios)
        val btnListar = findViewById<Button>(R.id.btnListarUsuarios)

        btnIngresar.setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }

        btnListar.setOnClickListener {
            startActivity(Intent(this, ListarUsuariosActivity::class.java))
        }
    }
}
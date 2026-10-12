package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MenuPrincipalActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_principal)

        val tvFechaHora = findViewById<TextView>(R.id.tvFechaHora)
        val sdf = SimpleDateFormat("dd-MM-yyyy, HH:mm:ss", Locale.getDefault())
        val currentDate = sdf.format(Date())
        tvFechaHora.text = "Fecha/Hora: $currentDate"

        val cardCrud = findViewById<CardView>(R.id.cardCrud)
        cardCrud.setOnClickListener {
            startActivity(Intent(this, GestionUsuariosActivity::class.java))
        }

        val cardSensores = findViewById<CardView>(R.id.cardSensores)
        cardSensores.setOnClickListener {
            startActivity(Intent(this, DatosSensoresActivity::class.java))
        }

        val cardDesarrolladores = findViewById<CardView>(R.id.cardDesarrolladores)
        cardDesarrolladores.setOnClickListener {
            startActivity(Intent(this, ListadoDesarrolladoresActivity::class.java))
        }
    }
}
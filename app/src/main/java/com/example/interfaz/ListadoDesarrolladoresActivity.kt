package com.example.interfaz

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ListadoDesarrolladoresActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listado_desarrolladores)

        val tvGithubAlan = findViewById<TextView>(R.id.tvGithubAlan)
        tvGithubAlan.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/devbritos"))
            startActivity(intent)
        }

        val tvGithubLucas = findViewById<TextView>(R.id.tvGithubLucas)
        tvGithubLucas.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/udbdl"))
            startActivity(intent)
        }
    }
}
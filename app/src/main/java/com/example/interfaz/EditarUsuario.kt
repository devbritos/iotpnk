package com.example.interfaz

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EditarUsuario : AppCompatActivity() {
    private lateinit var id: EditText
    private lateinit var nom: EditText
    private lateinit var ape: EditText
    private lateinit var mod: Button
    private lateinit var elim: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_editar_usuario)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        id = findViewById(R.id.txtid)
        nom = findViewById(R.id.txtnombremod)
        ape = findViewById(R.id.txtapellidomod)
        mod = findViewById(R.id.btn_modificar)
        elim = findViewById(R.id.btn_eliminar)

        // Recibir los datos enviados desde ListarUsuariosActivity
        val userIdInt = intent.getIntExtra("id", 0)
        val userIdStr = intent.getStringExtra("id_str") ?: intent.getStringExtra("id")
        val name = intent.getStringExtra("name") ?: ""
        val lastname = intent.getStringExtra("lastname") ?: ""

        val displayId = when {
            userIdInt != 0 -> userIdInt.toString()
            !userIdStr.isNullOrEmpty() && userIdStr != "0" -> userIdStr
            else -> ""
        }

        id.setText(displayId)
        nom.setText(name)
        ape.setText(lastname)

        // Botón Modificar usuario en SQLite local + Sincronización MySQL
        mod.setOnClickListener {
            val currentId = id.text.toString().trim()
            val newName = nom.text.toString().trim()
            val newLastname = ape.text.toString().trim()

            if (currentId.isEmpty() || newName.isEmpty() || newLastname.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val userId = currentId.toIntOrNull() ?: 0
            if (userId > 0) {
                // 1. Guardar modificación localmente en SQLite
                UserRepository.updateUserLocal(this, userId, newName, newLastname)
                Toast.makeText(this, "Usuario modificado localmente", Toast.LENGTH_SHORT).show()

                // 2. Intentar sincronización en segundo plano con servidor MySQL
                if (UserRepository.isNetworkAvailable(this)) {
                    UserRepository.syncWithServer(this) { _, _ -> }
                }
            }

            onBackPressedDispatcher.onBackPressed()
        }

        // Botón Eliminar usuario en SQLite local + Sincronización MySQL
        elim.setOnClickListener {
            val currentId = id.text.toString().trim()
            val userId = currentId.toIntOrNull() ?: 0

            if (userId <= 0) {
                Toast.makeText(this, "ID de usuario inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Eliminar/Marcar localmente en SQLite
            UserRepository.deleteUserLocal(this, userId)
            Toast.makeText(this, "Usuario eliminado localmente", Toast.LENGTH_SHORT).show()

            // 2. Intentar sincronización en segundo plano con servidor MySQL
            if (UserRepository.isNetworkAvailable(this)) {
                UserRepository.syncWithServer(this) { _, _ -> }
            }

            onBackPressedDispatcher.onBackPressed()
        }
    }
}

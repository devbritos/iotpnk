package com.example.interfaz

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley

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

        // Botón Modificar usuario en la base de datos
        mod.setOnClickListener {
            val currentId = id.text.toString().trim()
            val newName = nom.text.toString().trim()
            val newLastname = ape.text.toString().trim()

            if (currentId.isEmpty() || newName.isEmpty() || newLastname.isEmpty()) {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            modificarEnServidor(currentId, newName, newLastname)
        }

        // Botón Eliminar usuario de la base de datos
        elim.setOnClickListener {
            val currentId = id.text.toString().trim()

            if (currentId.isEmpty()) {
                Toast.makeText(this, "ID de usuario inválido", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            eliminarEnServidor(currentId)
        }
    }

    private fun modificarEnServidor(idUsuario: String, name: String, lastname: String) {
        val url = "http://34.202.221.246/modificar_usuario.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener {
                Toast.makeText(this, "Usuario modificado exitosamente", Toast.LENGTH_SHORT).show()
                onBackPressedDispatcher.onBackPressed()
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Toast.makeText(this, "Error al modificar usuario en el servidor", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id"] = idUsuario
                params["name"] = name
                params["lastname"] = lastname
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }

    private fun eliminarEnServidor(idUsuario: String) {
        val url = "http://34.202.221.246/eliminar_usuario.php"

        val request = object : StringRequest(
            Method.POST, url,
            Response.Listener {
                Toast.makeText(this, "Usuario eliminado exitosamente", Toast.LENGTH_SHORT).show()
                onBackPressedDispatcher.onBackPressed()
            },
            Response.ErrorListener { error ->
                error.printStackTrace()
                Toast.makeText(this, "Error al eliminar usuario en el servidor", Toast.LENGTH_SHORT).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params = HashMap<String, String>()
                params["id"] = idUsuario
                return params
            }
        }

        Volley.newRequestQueue(this).add(request)
    }
}

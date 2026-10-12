package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog
import com.android.volley.Request
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException

data class UsuarioItem(
    val id: Int,
    val name: String,
    val lastname: String,
    val email: String
)

class ListarUsuariosActivity : AppCompatActivity() {

    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var lvUsuarios: ListView
    private var displayList = mutableListOf<String>()
    private var userList = mutableListOf<UsuarioItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_listar_usuarios)

        lvUsuarios = findViewById(R.id.lvUsuarios)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, displayList)
        lvUsuarios.adapter = adapter

        // Seleccionar usuario para enviar sus datos a EditarUsuario
        lvUsuarios.setOnItemClickListener { _, _, position, _ ->
            if (position < userList.size) {
                val user = userList[position]

                SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("Gestión de Usuario")
                    .setContentText("¿Qué desea hacer con ${user.name} ${user.lastname}?")
                    .setConfirmText("Modificar")
                    .setCancelText("Eliminar")
                    .showCancelButton(true)
                    .setConfirmClickListener { sDialog ->
                        sDialog.dismissWithAnimation()
                        val intent = Intent(this@ListarUsuariosActivity, EditarUsuario::class.java).apply {
                            putExtra("id", user.id)
                            putExtra("id_str", user.id.toString())
                            putExtra("name", user.name)
                            putExtra("lastname", user.lastname)
                            putExtra("email", user.email)
                        }
                        startActivity(intent)
                    }
                    .setCancelClickListener { sDialog ->
                        sDialog.setTitleText("Eliminar")
                            .setContentText("¿Desea eliminar a ${user.email}?")
                            .setConfirmText("OK")
                            .showCancelButton(false)
                            .setConfirmClickListener { it.dismissWithAnimation() }
                            .changeAlertType(SweetAlertDialog.SUCCESS_TYPE)
                    }
                    .show()
            }
        }

        // Cargar usuarios desde MySQL en el servidor vía Volley
        obtenerUsuariosServidor()
    }

    override fun onResume() {
        super.onResume()
        // Recargar la lista cuando se regresa de EditarUsuario
        obtenerUsuariosServidor()
    }

    private fun obtenerUsuariosServidor() {
        val url = "http://34.202.221.246/obtener_usuarios.php"

        val request = JsonArrayRequest(
            Request.Method.GET, url, null,
            { response ->
                displayList.clear()
                userList.clear()

                try {
                    for (i in 0 until response.length()) {
                        val obj = response.getJSONObject(i)

                        val idStr = when {
                            obj.has("user_id") -> obj.optString("user_id")
                            obj.has("id") -> obj.optString("id")
                            obj.has("id_usuario") -> obj.optString("id_usuario")
                            obj.has("ID") -> obj.optString("ID")
                            else -> "0"
                        }
                        val id = idStr.toIntOrNull() ?: obj.optInt("id", obj.optInt("user_id", 0))

                        val nombre = obj.optString("name", "")
                        val apellido = obj.optString("lastname", "")
                        val email = obj.optString("email", "")

                        userList.add(UsuarioItem(id, nombre, apellido, email))
                        displayList.add("ID: $id - $nombre $apellido ($email)".trim())
                    }
                    adapter.notifyDataSetChanged()
                } catch (e: JSONException) {
                    e.printStackTrace()
                    Toast.makeText(this, "Error procesando datos del servidor", Toast.LENGTH_SHORT).show()
                }
            },
            { error ->
                error.printStackTrace()
                Toast.makeText(this, "Error de red al consultar el servidor", Toast.LENGTH_SHORT).show()
            }
        )
        Volley.newRequestQueue(this).add(request)
    }
}

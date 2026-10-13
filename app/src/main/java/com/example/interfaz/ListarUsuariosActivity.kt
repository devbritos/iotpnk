package com.example.interfaz

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.pedant.SweetAlert.SweetAlertDialog

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
                        sDialog.dismissWithAnimation()
                        // Eliminar localmente en SQLite
                        UserRepository.deleteUserLocal(this, user.id)
                        Toast.makeText(this, "Usuario eliminado localmente", Toast.LENGTH_SHORT).show()
                        cargarUsuariosLocales()

                        // Sincronizar en segundo plano con MySQL si hay conexión
                        if (UserRepository.isNetworkAvailable(this)) {
                            UserRepository.syncWithServer(this) { _, _ ->
                                cargarUsuariosLocales()
                            }
                        }
                    }
                    .show()
            }
        }

        // Cargar datos desde la base de datos local SQLite (100% Offline-First)
        cargarUsuariosLocales()

        // Sincronizar automáticamente en segundo plano si hay red disponible
        sincronizarServidor()
    }

    override fun onResume() {
        super.onResume()
        // Cargar lista local inmediatamente
        cargarUsuariosLocales()
        // Intentar sincronización automática con MySQL
        sincronizarServidor()
    }

    private fun cargarUsuariosLocales() {
        userList.clear()
        displayList.clear()

        val usuariosLocales = UserRepository.getUsersLocal(this)
        userList.addAll(usuariosLocales)

        val pending = UserRepository.getPendingCount(this)
        val isOnline = UserRepository.isNetworkAvailable(this)

        for (u in userList) {
            displayList.add("ID: ${u.id} - ${u.name} ${u.lastname} (${u.email}) [${u.status}]".trim())
        }
        adapter.notifyDataSetChanged()

        // Mostrar estado de sincronización y red en el título
        val estadoRed = if (isOnline) "En línea" else "Sin conexión"
        val estadoSync = if (pending > 0) " | $pending pendientes de sincronizar" else " | Todo sincronizado"
        title = "Usuarios ($estadoRed$estadoSync)"
    }

    private fun sincronizarServidor() {
        if (UserRepository.isNetworkAvailable(this)) {
            UserRepository.syncWithServer(this) { success, _ ->
                if (success) {
                    cargarUsuariosLocales()
                }
            }
        }
    }
}

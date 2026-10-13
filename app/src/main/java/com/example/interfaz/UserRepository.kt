package com.example.interfaz

import android.content.ContentValues
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject

object UserRepository {

    // 1. Verificar si hay conexión a Internet
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // 2. Operaciones locales SQLite
    fun getUsersLocal(context: Context): List<UsuarioItem> {
        val users = mutableListOf<UsuarioItem>()
        val dbHelper = ConexionDbHelper(context)
        val db = dbHelper.readableDatabase

        val cursor = db.query(
            "users",
            arrayOf("user_id", "name", "lastname", "email", "status"),
            "sync_state != ?",
            arrayOf(ConexionDbHelper.STATE_PENDING_DELETE.toString()),
            null,
            null,
            "user_id ASC"
        )

        with(cursor) {
            while (moveToNext()) {
                val id = getInt(getColumnIndexOrThrow("user_id"))
                val name = getString(getColumnIndexOrThrow("name")) ?: ""
                val lastname = getString(getColumnIndexOrThrow("lastname")) ?: ""
                val email = getString(getColumnIndexOrThrow("email")) ?: ""
                val status = getString(getColumnIndexOrThrow("status")) ?: "active"
                users.add(UsuarioItem(id, name, lastname, email, status))
            }
            close()
        }
        db.close()
        return users
    }

    fun getPendingCount(context: Context): Int {
        val dbHelper = ConexionDbHelper(context)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM users WHERE sync_state != ? AND sync_state != ?",
            arrayOf(ConexionDbHelper.STATE_SYNCED.toString(), ConexionDbHelper.STATE_SYNCING.toString())
        )
        var count = 0
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }

    fun addUserLocal(context: Context, name: String, lastname: String, email: String, pass: String): Long {
        val dbHelper = ConexionDbHelper(context)
        val db = dbHelper.writableDatabase
        val cleanEmail = email.trim().lowercase()

        val values = ContentValues().apply {
            put("name", name.trim())
            put("lastname", lastname.trim())
            put("email", cleanEmail)
            put("password", pass)
            put("status", "active")
            put("sync_state", ConexionDbHelper.STATE_PENDING_INSERT)
        }

        val id = db.insertWithOnConflict("users", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        db.close()
        return id
    }

    fun updateUserLocal(context: Context, id: Int, name: String, lastname: String): Int {
        val dbHelper = ConexionDbHelper(context)
        val db = dbHelper.writableDatabase

        // Verificar estado actual
        val cursor = db.query("users", arrayOf("sync_state"), "user_id = ?", arrayOf(id.toString()), null, null, null)
        var currentState = ConexionDbHelper.STATE_SYNCED
        if (cursor.moveToFirst()) {
            currentState = cursor.getInt(0)
        }
        cursor.close()

        val newState = if (currentState == ConexionDbHelper.STATE_PENDING_INSERT) {
            ConexionDbHelper.STATE_PENDING_INSERT
        } else {
            ConexionDbHelper.STATE_PENDING_UPDATE
        }

        val values = ContentValues().apply {
            put("name", name)
            put("lastname", lastname)
            put("sync_state", newState)
        }

        val rows = db.update("users", values, "user_id = ?", arrayOf(id.toString()))
        db.close()
        return rows
    }

    fun deleteUserLocal(context: Context, id: Int): Int {
        val dbHelper = ConexionDbHelper(context)
        val db = dbHelper.writableDatabase

        val cursor = db.query("users", arrayOf("sync_state"), "user_id = ?", arrayOf(id.toString()), null, null, null)
        var currentState = ConexionDbHelper.STATE_SYNCED
        if (cursor.moveToFirst()) {
            currentState = cursor.getInt(0)
        }
        cursor.close()

        val rows = if (currentState == ConexionDbHelper.STATE_PENDING_INSERT) {
            // Creado offline sin sincronizar aún, eliminar de SQLite directamente
            db.delete("users", "user_id = ?", arrayOf(id.toString()))
        } else {
            // Marcar como pendiente de eliminar en MySQL
            val values = ContentValues().apply {
                put("sync_state", ConexionDbHelper.STATE_PENDING_DELETE)
            }
            db.update("users", values, "user_id = ?", arrayOf(id.toString()))
        }

        db.close()
        return rows
    }

    // 3. Sincronización bidireccional con MySQL
    fun syncWithServer(context: Context, onResult: (success: Boolean, message: String) -> Unit) {
        if (!isNetworkAvailable(context)) {
            val pending = getPendingCount(context)
            val msg = if (pending > 0) {
                "Sin conexión. Hay $pending cambios pendientes de sincronización."
            } else {
                "Modo sin conexión. Trabajando con datos locales."
            }
            onResult(false, msg)
            return
        }

        val dbHelper = ConexionDbHelper(context)
        val db = dbHelper.writableDatabase
        val queue = Volley.newRequestQueue(context)

        val actions = mutableListOf<(() -> Unit) -> Unit>()

        // A. Sincronizar Inserciones Pendientes
        val cursorInserts = db.query(
            "users",
            arrayOf("user_id", "name", "lastname", "email", "password", "status"),
            "sync_state = ?",
            arrayOf(ConexionDbHelper.STATE_PENDING_INSERT.toString()),
            null, null, null
        )

        while (cursorInserts.moveToNext()) {
            val localId = cursorInserts.getInt(0)
            val name = cursorInserts.getString(1) ?: ""
            val lastname = cursorInserts.getString(2) ?: ""
            val email = cursorInserts.getString(3) ?: ""
            val pass = cursorInserts.getString(4) ?: ""
            val status = cursorInserts.getString(5) ?: "active"

            val syncingValues = ContentValues().apply {
                put("sync_state", ConexionDbHelper.STATE_SYNCING)
            }
            db.update("users", syncingValues, "user_id = ? AND sync_state = ?", arrayOf(localId.toString(), ConexionDbHelper.STATE_PENDING_INSERT.toString()))

            actions.add { onComplete ->
                val url = "http://34.202.221.246/registrar_usuario.php"
                val request = object : StringRequest(
                    Method.POST, url,
                    Response.Listener { response ->
                        try {
                            val json = JSONObject(response)
                            if (json.optString("status") == "success") {
                                val values = ContentValues().apply {
                                    put("sync_state", ConexionDbHelper.STATE_SYNCED)
                                }
                                db.update("users", values, "user_id = ?", arrayOf(localId.toString()))
                            } else {
                                val values = ContentValues().apply {
                                    put("sync_state", ConexionDbHelper.STATE_PENDING_INSERT)
                                }
                                db.update("users", values, "user_id = ?", arrayOf(localId.toString()))
                            }
                        } catch (_: Exception) {
                            val values = ContentValues().apply {
                                put("sync_state", ConexionDbHelper.STATE_PENDING_INSERT)
                            }
                            db.update("users", values, "user_id = ?", arrayOf(localId.toString()))
                        }
                        onComplete()
                    },
                    Response.ErrorListener {
                        try {
                            val dbHelperW = ConexionDbHelper(context)
                            val dbW = dbHelperW.writableDatabase
                            val values = ContentValues().apply {
                                put("sync_state", ConexionDbHelper.STATE_PENDING_INSERT)
                            }
                            dbW.update("users", values, "user_id = ?", arrayOf(localId.toString()))
                            dbW.close()
                        } catch (_: Exception) {}
                        onComplete()
                    }
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        val params = HashMap<String, String>()
                        params["name"] = name
                        params["lastname"] = lastname
                        params["email"] = email
                        params["password"] = pass
                        params["status"] = if (status.isNotEmpty()) status else "active"
                        return params
                    }
                }
                queue.add(request)
            }
        }
        cursorInserts.close()

        // B. Sincronizar Modificaciones Pendientes
        val cursorUpdates = db.query(
            "users",
            arrayOf("user_id", "name", "lastname"),
            "sync_state = ?",
            arrayOf(ConexionDbHelper.STATE_PENDING_UPDATE.toString()),
            null, null, null
        )

        while (cursorUpdates.moveToNext()) {
            val id = cursorUpdates.getInt(0)
            val name = cursorUpdates.getString(1) ?: ""
            val lastname = cursorUpdates.getString(2) ?: ""

            val syncingValues = ContentValues().apply {
                put("sync_state", ConexionDbHelper.STATE_SYNCING)
            }
            db.update("users", syncingValues, "user_id = ? AND sync_state = ?", arrayOf(id.toString(), ConexionDbHelper.STATE_PENDING_UPDATE.toString()))

            actions.add { onComplete ->
                val url = "http://34.202.221.246/modificar_usuario.php"
                val request = object : StringRequest(
                    Method.POST, url,
                    Response.Listener { response ->
                        try {
                            val json = JSONObject(response)
                            if (json.optString("status") == "success") {
                                val values = ContentValues().apply {
                                    put("sync_state", ConexionDbHelper.STATE_SYNCED)
                                }
                                db.update("users", values, "user_id = ?", arrayOf(id.toString()))
                            } else {
                                val values = ContentValues().apply {
                                    put("sync_state", ConexionDbHelper.STATE_PENDING_UPDATE)
                                }
                                db.update("users", values, "user_id = ?", arrayOf(id.toString()))
                            }
                        } catch (_: Exception) {
                            val values = ContentValues().apply {
                                put("sync_state", ConexionDbHelper.STATE_PENDING_UPDATE)
                            }
                            db.update("users", values, "user_id = ?", arrayOf(id.toString()))
                        }
                        onComplete()
                    },
                    Response.ErrorListener {
                        try {
                            val dbHelperW = ConexionDbHelper(context)
                            val dbW = dbHelperW.writableDatabase
                            val values = ContentValues().apply {
                                put("sync_state", ConexionDbHelper.STATE_PENDING_UPDATE)
                            }
                            dbW.update("users", values, "user_id = ?", arrayOf(id.toString()))
                            dbW.close()
                        } catch (_: Exception) {}
                        onComplete()
                    }
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        val params = HashMap<String, String>()
                        params["id"] = id.toString()
                        params["user_id"] = id.toString()
                        params["name"] = name
                        params["lastname"] = lastname
                        return params
                    }
                }
                queue.add(request)
            }
        }
        cursorUpdates.close()

        // C. Sincronizar Eliminaciones Pendientes
        val cursorDeletes = db.query(
            "users",
            arrayOf("user_id"),
            "sync_state = ?",
            arrayOf(ConexionDbHelper.STATE_PENDING_DELETE.toString()),
            null, null, null
        )

        while (cursorDeletes.moveToNext()) {
            val id = cursorDeletes.getInt(0)

            val syncingValues = ContentValues().apply {
                put("sync_state", ConexionDbHelper.STATE_SYNCING)
            }
            db.update("users", syncingValues, "user_id = ? AND sync_state = ?", arrayOf(id.toString(), ConexionDbHelper.STATE_PENDING_DELETE.toString()))

            actions.add { onComplete ->
                val url = "http://34.202.221.246/eliminar_usuario.php"
                val request = object : StringRequest(
                    Method.POST, url,
                    Response.Listener { response ->
                        var deletedOnServer = false
                        try {
                            val trimmed = response.trim()
                            if (trimmed.contains("success", ignoreCase = true)) {
                                deletedOnServer = true
                            } else {
                                val json = JSONObject(trimmed)
                                val status = json.optString("status", json.optString("resultado", ""))
                                val success = json.optBoolean("success", false)
                                if (status.equals("success", ignoreCase = true) || success) {
                                    deletedOnServer = true
                                }
                            }
                        } catch (_: Exception) {
                            if (response.trim().isNotEmpty()) {
                                deletedOnServer = true
                            }
                        }

                        if (deletedOnServer) {
                            db.delete("users", "user_id = ?", arrayOf(id.toString()))
                        } else {
                            val values = ContentValues().apply {
                                put("sync_state", ConexionDbHelper.STATE_PENDING_DELETE)
                            }
                            db.update("users", values, "user_id = ?", arrayOf(id.toString()))
                        }
                        onComplete()
                    },
                    Response.ErrorListener {
                        try {
                            val dbHelperW = ConexionDbHelper(context)
                            val dbW = dbHelperW.writableDatabase
                            val values = ContentValues().apply {
                                put("sync_state", ConexionDbHelper.STATE_PENDING_DELETE)
                            }
                            dbW.update("users", values, "user_id = ?", arrayOf(id.toString()))
                            dbW.close()
                        } catch (_: Exception) {}
                        onComplete()
                    }
                ) {
                    override fun getParams(): MutableMap<String, String> {
                        val params = HashMap<String, String>()
                        params["id"] = id.toString()
                        params["user_id"] = id.toString()
                        params["id_usuario"] = id.toString()
                        return params
                    }
                }
                queue.add(request)
            }
        }
        cursorDeletes.close()

        val runDownload = {
            val urlGet = "http://34.202.221.246/obtener_usuarios.php"
            val getRequest = JsonArrayRequest(
                Request.Method.GET, urlGet, null,
                Response.Listener { response ->
                    try {
                        for (i in 0 until response.length()) {
                            val obj = response.getJSONObject(i)
                            val idStr = when {
                                obj.has("user_id") -> obj.optString("user_id")
                                obj.has("id") -> obj.optString("id")
                                obj.has("id_usuario") -> obj.optString("id_usuario")
                                else -> "0"
                            }
                            val serverId = idStr.toIntOrNull() ?: 0
                            val nombre = obj.optString("name", "")
                            val apellido = obj.optString("lastname", "")
                            val email = obj.optString("email", "")
                            val statusStr = obj.optString("status", "active")
                            val userStatus = if (statusStr.isNotEmpty()) statusStr else "active"
                            val cleanEmail = email.trim().lowercase()

                            if (serverId > 0 && cleanEmail.isNotEmpty()) {
                                val values = ContentValues().apply {
                                    put("user_id", serverId)
                                    put("name", nombre.trim())
                                    put("lastname", apellido.trim())
                                    put("email", cleanEmail)
                                    put("status", userStatus)
                                    put("sync_state", ConexionDbHelper.STATE_SYNCED)
                                }
                                db.insertWithOnConflict("users", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                            }
                        }
                        db.close()
                        onResult(true, "Sincronización completada exitosamente.")
                    } catch (_: JSONException) {
                        db.close()
                        onResult(true, "Sincronización enviada con éxito.")
                    }
                },
                Response.ErrorListener {
                    db.close()
                    onResult(true, "Cambios enviados al servidor.")
                }
            )
            queue.add(getRequest)
        }

        if (actions.isEmpty()) {
            runDownload()
        } else {
            var completed = 0
            val total = actions.size
            val onComplete = {
                completed++
                if (completed >= total) {
                    runDownload()
                }
            }
            for (action in actions) {
                action(onComplete)
            }
        }
    }
}

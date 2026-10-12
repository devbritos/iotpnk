package com.example.interfaz

data class User(
    val nombres: String,
    val apellidos: String,
    val email: String,
    val clave: String
)

object UserManager {
    private val userList = mutableListOf<User>()

    // Añadir usuarios por defecto para que la lista no esté vacía
    init {
        userList.add(User("Admin", "Sistema", "admin@gmail.com", "Admin123!"))
    }

    fun addUser(user: User): Boolean {
        if (userList.any { it.email == user.email }) return false
        userList.add(user)
        return true
    }

    fun getUsers(): List<User> = userList

    fun removeUser(email: String) {
        userList.removeAll { it.email == email }
    }
}
package com.example.interfaz

data class UsuarioItem(
    val id: Int,
    val name: String,
    val lastname: String,
    val email: String,
    val status: String = "active"
)

package com.example.hismaadmin.model

/**
 * Modelo de datos para un Lubricentro en la aplicación de administración
 */
data class Lubricentro(
    val uid: String = "",
    val nombreFantasia: String = "",
    val responsable: String = "",
    val cuit: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val email: String = "",
    val logoUrl: String = "",
    val fechaRegistro: String = "",
    val activo: Boolean = true
)

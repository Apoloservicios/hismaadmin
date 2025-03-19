package com.example.hismaadm.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Modelo extendido del Lubricentro con información de suscripción y estado
 */
data class LubricentroExtended(
    @DocumentId val id: String = "",
    val uid: String = "",
    val nombreFantasia: String = "",
    val responsable: String = "",
    val cuit: String = "",
    val direccion: String = "",
    val telefono: String = "",
    val email: String = "",
    val logoUrl: String = "",

    // Campos administrativos
    val activo: Boolean = true,
    val verificado: Boolean = false,
    val fechaRegistro: Timestamp = Timestamp.now(),
    val ultimoAcceso: Timestamp? = null,

    // Estadísticas
    val totalCambiosAceite: Int = 0,
    val totalEmpleados: Int = 0,

    // Datos de suscripción actual (desnormalizados para facilitar consultas)
    val planActual: String = "",
    val suscripcionActiva: Boolean = false,
    val suscripcionId: String = "",
    val suscripcionFechaFin: Timestamp? = null
)
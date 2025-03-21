package com.example.hismaadm.model

import com.google.firebase.Timestamp

data class Suscripcion(
    val id: String = "",
    val lubricentroId: String = "",
    val planId: String = "",
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFin: Timestamp = Timestamp.now(),
    var estado: String = "activa", // "activa", "pendiente", "cancelada"
    var activa: Boolean = true,    // Añade este campo
    val cambiosTotales: Int = 100,
    val cambiosRealizados: Int = 0,
    val cambiosRestantes: Int = 100,
    val isPaqueteAdicional: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun isValid(): Boolean {
        val now = Timestamp.now()
        return activa && estado == "activa" && fechaFin > now && cambiosRestantes > 0
    }
}
fun Suscripcion.toFirebaseMap(): Map<String, Any> {
    return mapOf(
        "lubricentroId" to lubricentroId,
        "planId" to planId,
        "fechaInicio" to fechaInicio,
        "fechaFin" to fechaFin,
        "estado" to estado,
        "activa" to (estado == "activa"),  // Sincronizar con el campo estado
        "cambiosTotales" to cambiosTotales,
        "cambiosRealizados" to cambiosRealizados,
        "cambiosRestantes" to cambiosRestantes,
        "isPaqueteAdicional" to isPaqueteAdicional,
        "createdAt" to createdAt,
        "updatedAt" to Timestamp.now()
    )
}
package com.example.hismaadm.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Modelo para representar la suscripción de un lubricentro
 */
data class LubricentroSubscription(
    @DocumentId val id: String = "",
    val lubricentroId: String = "",
    val planId: String = "",
    val planNombre: String = "",
    val fechaInicio: Timestamp = Timestamp.now(),
    val fechaFin: Timestamp = Timestamp.now(),
    val activa: Boolean = true,
    val metodoPago: String = "",
    val ultimoPago: Timestamp? = null,
    val proximoPago: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val limiteEmpleados: Int = 3,  // Valor predeterminado
    val limiteCambiosAceite: Int = 100  // Valor predeterminado
)
package com.example.hismaadm.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Modelo para representar un plan de suscripción
 */
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val precio: Double = 0.0,
    val duracionMeses: Int = 1,
    val limiteEmpleados: Int = 5,
    val limiteCambiosAceite: Int = 100,
    val activo: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
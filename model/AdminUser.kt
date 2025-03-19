package com.example.hismaadm.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

/**
 * Modelo para el usuario administrador
 */
data class AdminUser(
    @DocumentId val id: String = "",
    val nombre: String = "",
    val email: String = "",
    val rol: String = "admin",  // admin, superadmin
    val activo: Boolean = true,
    val ultimoAcceso: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now()
)
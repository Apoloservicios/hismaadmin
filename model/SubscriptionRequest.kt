package com.example.hismaadm.model

import com.google.firebase.Timestamp

data class SubscriptionRequest(
    val id: String = "",
    val lubricentroId: String = "",
    val lubricentroName: String = "",
    val tipoSuscripcion: String = "",
    val isPaqueteAdicional: Boolean = false,
    val estado: String = "pendiente",
    val createdAt: Timestamp = Timestamp.now()
)
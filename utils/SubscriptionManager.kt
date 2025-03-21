package com.example.hismaadm.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.hismaadm.model.Suscripcion
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Clase para manejar todas las operaciones relacionadas con las suscripciones
 * en la aplicación de administración
 */
class SubscriptionManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val TAG = "AdminSubscriptionManager"

    /**
     * Obtiene todas las suscripciones en el sistema
     */
    suspend fun getAllSubscriptions(): List<Suscripcion> {
        try {
            val snapshot = db.collection("suscripciones")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val lubricentroId = doc.getString("lubricentroId") ?: ""
                    val planId = doc.getString("planId") ?: ""
                    val fechaInicio = doc.getTimestamp("fechaInicio") ?: Timestamp.now()
                    val fechaFin = doc.getTimestamp("fechaFin") ?: Timestamp.now()
                    val estado = doc.getString("estado") ?: "inactiva"
                    val cambiosTotales = doc.getLong("cambiosTotales")?.toInt() ?: 0
                    val cambiosRealizados = doc.getLong("cambiosRealizados")?.toInt() ?: 0
                    val cambiosRestantes = doc.getLong("cambiosRestantes")?.toInt() ?: 0
                    val isPaqueteAdicional = doc.getBoolean("isPaqueteAdicional") ?: false
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()

                    Suscripcion(
                        id = id,
                        lubricentroId = lubricentroId,
                        planId = planId,
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin,
                        estado = estado,
                        cambiosTotales = cambiosTotales,
                        cambiosRealizados = cambiosRealizados,
                        cambiosRestantes = cambiosRestantes,
                        isPaqueteAdicional = isPaqueteAdicional,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando documento: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo suscripciones: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Obtiene las suscripciones de un lubricentro específico
     */
    suspend fun getSubscriptionsForLubricentro(lubricentroId: String): List<Suscripcion> {
        try {
            val snapshot = db.collection("suscripciones")
                .whereEqualTo("lubricentroId", lubricentroId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            return snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val planId = doc.getString("planId") ?: ""
                    val fechaInicio = doc.getTimestamp("fechaInicio") ?: Timestamp.now()
                    val fechaFin = doc.getTimestamp("fechaFin") ?: Timestamp.now()
                    val estado = doc.getString("estado") ?: "inactiva"
                    val cambiosTotales = doc.getLong("cambiosTotales")?.toInt() ?: 0
                    val cambiosRealizados = doc.getLong("cambiosRealizados")?.toInt() ?: 0
                    val cambiosRestantes = doc.getLong("cambiosRestantes")?.toInt() ?: 0
                    val isPaqueteAdicional = doc.getBoolean("isPaqueteAdicional") ?: false
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()

                    Suscripcion(
                        id = id,
                        lubricentroId = lubricentroId,
                        planId = planId,
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin,
                        estado = estado,
                        cambiosTotales = cambiosTotales,
                        cambiosRealizados = cambiosRealizados,
                        cambiosRestantes = cambiosRestantes,
                        isPaqueteAdicional = isPaqueteAdicional,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error mapeando documento: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo suscripciones de lubricentro: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Crea una nueva suscripción
     */
    suspend fun createSubscription(suscripcion: Suscripcion): Boolean {
        return try {
            // Verificar si hay suscripciones activas no paquete para evitar duplicados
            if (!suscripcion.isPaqueteAdicional) {
                val activasPrincipales = getActiveMainSubscriptions(suscripcion.lubricentroId)
                if (activasPrincipales.isNotEmpty()) {
                    // Si hay suscripciones principales activas, marcar como pendiente
                    val susData = suscripcion.toFirebaseMap().toMutableMap()
                    susData["estado"] = "pendiente"
                    db.collection("suscripciones").document().set(susData).await()
                    return true
                }
            }

            // Si no hay problemas o es paquete adicional, crear normalmente
            db.collection("suscripciones").document().set(suscripcion.toFirebaseMap()).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creando suscripción: ${e.message}", e)
            false
        }
    }

    /**
     * Obtener suscripciones principales activas (no paquetes adicionales)
     */
    private suspend fun getActiveMainSubscriptions(lubricentroId: String): List<Suscripcion> {
        return try {
            val snapshot = db.collection("suscripciones")
                .whereEqualTo("lubricentroId", lubricentroId)
                .whereEqualTo("estado", "activa")
                .whereEqualTo("isPaqueteAdicional", false)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                try {
                    val id = doc.id
                    val planId = doc.getString("planId") ?: ""
                    val fechaInicio = doc.getTimestamp("fechaInicio") ?: Timestamp.now()
                    val fechaFin = doc.getTimestamp("fechaFin") ?: Timestamp.now()
                    val cambiosTotales = doc.getLong("cambiosTotales")?.toInt() ?: 0
                    val cambiosRealizados = doc.getLong("cambiosRealizados")?.toInt() ?: 0
                    val cambiosRestantes = doc.getLong("cambiosRestantes")?.toInt() ?: 0
                    val createdAt = doc.getTimestamp("createdAt") ?: Timestamp.now()

                    Suscripcion(
                        id = id,
                        lubricentroId = lubricentroId,
                        planId = planId,
                        fechaInicio = fechaInicio,
                        fechaFin = fechaFin,
                        estado = "activa",
                        cambiosTotales = cambiosTotales,
                        cambiosRealizados = cambiosRealizados,
                        cambiosRestantes = cambiosRestantes,
                        isPaqueteAdicional = false,
                        createdAt = createdAt
                    )
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun changeSubscriptionStatus(suscripcionId: String, nuevoEstado: String): Boolean {
        return try {
            // Actualizar ambos campos: estado (string) y activa (booleano)
            val updateData = mapOf(
                "estado" to nuevoEstado,
                "activa" to (nuevoEstado == "activa"),  // true si estado="activa", false en otro caso
                "updatedAt" to Timestamp.now()
            )

            db.collection("suscripciones")
                .document(suscripcionId)
                .update(updateData)
                .await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error cambiando estado de suscripción: ${e.message}", e)
            false
        }
    }

    /**
     * Obtener solicitudes de suscripción pendientes
     */
    suspend fun getPendingRequests(): List<Map<String, Any>> {
        try {
            val snapshot = db.collection("solicitudesSuscripcion")
                .whereEqualTo("estado", "pendiente")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d(TAG, "Solicitudes encontradas: ${snapshot.documents.size}")

            return snapshot.documents.mapNotNull { doc ->
                try {
                    Log.d(TAG, "Procesando documento ${doc.id}: ${doc.data}")
                    if (doc.data != null) {
                        val data = HashMap<String, Any>(doc.data!!)
                        data["id"] = doc.id
                        data
                    } else null
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando solicitud: ${e.message}", e)
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo solicitudes: ${e.message}", e)
            return emptyList()
        }
    }

    /**
     * Aprobar una solicitud de suscripción
     */
    suspend fun approveSubscriptionRequest(
        solicitudId: String,
        cambiosTotales: Int,
        duracionMeses: Int
    ): Boolean {
        try {
            // Obtener datos de la solicitud
            val solicitudDoc = db.collection("solicitudesSuscripcion")
                .document(solicitudId)
                .get()
                .await()

            if (!solicitudDoc.exists()) {
                return false
            }

            val lubricentroId = solicitudDoc.getString("lubricentroId") ?: return false
            val tipoSuscripcion = solicitudDoc.getString("tipoSuscripcion") ?: return false
            val isPaqueteAdicional = solicitudDoc.getBoolean("isPaqueteAdicional") ?: false

            // Crear nueva suscripción
            val fechaInicio = Timestamp.now()
            val calendar = Calendar.getInstance()
            calendar.time = fechaInicio.toDate()
            calendar.add(Calendar.MONTH, duracionMeses)
            val fechaFin = Timestamp(calendar.time)

            val suscripcion = Suscripcion(
                lubricentroId = lubricentroId,
                planId = tipoSuscripcion,
                fechaInicio = fechaInicio,
                fechaFin = fechaFin,
                estado = if (isPaqueteAdicional) "activa" else "pendiente", // Verificaremos después
                cambiosTotales = cambiosTotales,
                cambiosRealizados = 0,
                cambiosRestantes = cambiosTotales,
                isPaqueteAdicional = isPaqueteAdicional,
                createdAt = Timestamp.now()
            )

            // Si no es paquete adicional, verificar si hay suscripciones principales activas
            val inicialmenteActiva = isPaqueteAdicional
            if (!inicialmenteActiva) {
                val activasPrincipales = getActiveMainSubscriptions(lubricentroId)
                if (activasPrincipales.isEmpty()) {
                    // Si no hay activas, esta será activa
                    suscripcion.estado = "activa"
                }
            }

            // Guardar suscripción
            val success = createSubscription(suscripcion)
            if (!success) {
                return false
            }

            // Actualizar estado de la solicitud
            db.collection("solicitudesSuscripcion")
                .document(solicitudId)
                .update(
                    mapOf(
                        "estado" to "aprobada",
                        "updatedAt" to Timestamp.now()
                    )
                ).await()

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error aprobando solicitud: ${e.message}", e)
            return false
        }
    }

    /**
     * Rechazar una solicitud de suscripción
     */
    suspend fun rejectSubscriptionRequest(solicitudId: String): Boolean {
        return try {
            db.collection("solicitudesSuscripcion")
                .document(solicitudId)
                .update(
                    mapOf(
                        "estado" to "rechazada",
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error rechazando solicitud: ${e.message}", e)
            false
        }
    }
}

/**
 * Extensión para convertir Suscripcion a mapa para Firestore
 */
fun Suscripcion.toFirebaseMap(): Map<String, Any> {
    return mapOf(
        "lubricentroId" to lubricentroId,
        "planId" to planId,
        "fechaInicio" to fechaInicio,
        "fechaFin" to fechaFin,
        "estado" to estado,
        "cambiosTotales" to cambiosTotales,
        "cambiosRealizados" to cambiosRealizados,
        "cambiosRestantes" to cambiosRestantes,
        "isPaqueteAdicional" to isPaqueteAdicional,
        "createdAt" to createdAt,
        "updatedAt" to Timestamp.now()
    )
}
package com.example.hismaadm.repository

import com.example.hismaadm.model.Suscripcion
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class SuscripcionRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    // Obtener suscripciones activas de un lubricentro
    suspend fun obtenerSuscripcionesActivas(lubricentroId: String): List<Suscripcion> {
        return db.collection("suscripciones")
            .whereEqualTo("lubricentroId", lubricentroId)
            .whereEqualTo("estado", "activa")
            .get()
            .await()
            .documents.mapNotNull { doc ->
                doc.toObject(Suscripcion::class.java)?.copy(id = doc.id)
            }
    }

    // Obtener suscripciones pendientes de un lubricentro
    suspend fun obtenerSuscripcionesPendientes(lubricentroId: String): List<Suscripcion> {
        return db.collection("suscripciones")
            .whereEqualTo("lubricentroId", lubricentroId)
            .whereEqualTo("estado", "pendiente")
            .get()
            .await()
            .documents.mapNotNull { doc ->
                doc.toObject(Suscripcion::class.java)?.copy(id = doc.id)
            }
    }

    // Crear nueva suscripción
    suspend fun crearSuscripcion(suscripcion: Suscripcion): String {
        val suscripcionesActivas = obtenerSuscripcionesActivas(suscripcion.lubricentroId)

        val nuevaSuscripcion = if (suscripcionesActivas.isNotEmpty() && !suscripcion.isPaqueteAdicional) {
            // Si hay suscripciones activas y no es paquete adicional, se crea como pendiente
            val ultimaSuscripcion = suscripcionesActivas
                .filter { !it.isPaqueteAdicional }
                .maxByOrNull { it.fechaFin }

            suscripcion.copy(
                estado = "pendiente",
                fechaInicio = ultimaSuscripcion?.fechaFin ?: suscripcion.fechaInicio
            )
        } else {
            // Si no hay suscripciones activas o es un paquete adicional, se activa inmediatamente
            suscripcion
        }

        val docRef = db.collection("suscripciones").document()
        docRef.set(nuevaSuscripcion.copy(id = docRef.id)).await()
        return docRef.id
    }

    // Desactivar suscripción y activar pendiente si existe
    suspend fun desactivarSuscripcion(suscripcionId: String, activarPendiente: Boolean = false): Result<Unit> {
        try {
            val suscripcionRef = db.collection("suscripciones").document(suscripcionId)
            val suscripcionDoc = suscripcionRef.get().await()
            val suscripcion = suscripcionDoc.toObject(Suscripcion::class.java)?.copy(id = suscripcionDoc.id)
                ?: return Result.failure(Exception("Suscripción no encontrada"))

            // Desactivar la suscripción actual
            suscripcionRef.update(
                mapOf(
                    "estado" to "cancelada",
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            if (activarPendiente) {
                // Buscar suscripciones pendientes
                val pendientes = obtenerSuscripcionesPendientes(suscripcion.lubricentroId)
                if (pendientes.isNotEmpty()) {
                    // Ordenar por fecha de creación (las más antiguas primero)
                    val proximaSuscripcion = pendientes.minByOrNull { it.createdAt }
                    if (proximaSuscripcion != null) {
                        // Activar la próxima suscripción
                        db.collection("suscripciones").document(proximaSuscripcion.id)
                            .update(
                                mapOf(
                                    "estado" to "activa",
                                    "fechaInicio" to Timestamp.now(),
                                    "updatedAt" to Timestamp.now()
                                )
                            ).await()
                    }
                }
            }

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    // Obtener todas las suscripciones
    suspend fun obtenerTodasSuscripciones(): List<Suscripcion> {
        return db.collection("suscripciones")
            .get()
            .await()
            .documents.mapNotNull { doc ->
                doc.toObject(Suscripcion::class.java)?.copy(id = doc.id)
            }
    }

    // Procesar solicitud de suscripción
    suspend fun procesarSolicitudSuscripcion(
        solicitudId: String,
        aprobar: Boolean,
        cambiosTotales: Int,
        duracionMeses: Int
    ): Result<Unit> {
        try {
            val solicitudRef = db.collection("solicitudesSuscripcion").document(solicitudId)
            val solicitudDoc = solicitudRef.get().await()

            if (!solicitudDoc.exists()) {
                return Result.failure(Exception("Solicitud no encontrada"))
            }

            val data = solicitudDoc.data
            if (data == null) {
                return Result.failure(Exception("Datos de solicitud inválidos"))
            }

            if (aprobar) {
                val lubricentroId = data["lubricentroId"] as? String
                    ?: return Result.failure(Exception("ID de lubricentro inválido"))

                val tipoSuscripcion = data["tipoSuscripcion"] as? String
                    ?: return Result.failure(Exception("Tipo de suscripción inválido"))

                val isPaqueteAdicional = data["isPaqueteAdicional"] as? Boolean ?: false

                // Crear la suscripción
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
                    estado = "activa",
                    cambiosTotales = cambiosTotales,
                    cambiosRealizados = 0,
                    cambiosRestantes = cambiosTotales,
                    isPaqueteAdicional = isPaqueteAdicional,
                    createdAt = Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                crearSuscripcion(suscripcion)
            }

            // Actualizar estado de la solicitud
            solicitudRef.update(
                mapOf(
                    "estado" to if (aprobar) "aprobada" else "rechazada",
                    "updatedAt" to Timestamp.now()
                )
            ).await()

            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }
}
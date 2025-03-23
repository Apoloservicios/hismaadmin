package com.example.hismaadmin.utils

import android.util.Log
import com.example.hismaadmin.model.Plan
import com.example.hismaadmin.model.Subscription
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Clase utilitaria para gestionar suscripciones desde la aplicación de administración
 */
class SubscriptionManager(private val db: FirebaseFirestore) {

    private val TAG = "SubscriptionManager"

    /**
     * Obtiene la suscripción actual de un lubricentro
     */
    suspend fun getCurrentSubscription(lubricentroId: String): Subscription? {
        return try {
            val subscriptionDoc = db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription")
                .document("current")
                .get()
                .await()

            if (subscriptionDoc.exists()) {
                val subscription = subscriptionDoc.toObject(Subscription::class.java)
                subscription?.copy(id = subscriptionDoc.id)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo suscripción actual", e)
            null
        }
    }

    /**
     * Aplica un nuevo plan a un lubricentro
     */
    suspend fun aplicarNuevoPlan(lubricentroId: String, plan: Plan): Boolean {
        return try {
            // Crear nueva suscripción
            val fechaCreacion = Timestamp.now()
            val calendar = Calendar.getInstance()
            calendar.time = fechaCreacion.toDate()
            calendar.add(Calendar.DAY_OF_MONTH, plan.diasDuracion)
            val fechaVencimiento = Timestamp(calendar.time)

            val nuevaSuscripcion = mapOf(
                "lubricentroId" to lubricentroId,
                "cambiosRestantes" to plan.cantidadCambios,
                "cambiosUtilizados" to 0,
                "fechaVencimiento" to fechaVencimiento,
                "fechaCreacion" to fechaCreacion,
                "activa" to true,
                "planId" to plan.id,
                "tipoSuscripcion" to plan.nombre
            )

            // Actualizar o crear el documento de suscripción actual
            db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription")
                .document("current")
                .set(nuevaSuscripcion)
                .await()

            // Registrar en el historial de suscripciones
            db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription_history")
                .add(nuevaSuscripcion)
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error aplicando nuevo plan", e)
            false
        }
    }

    /**
     * Modifica manualmente una suscripción existente
     */
    suspend fun modificarSuscripcion(
        lubricentroId: String,
        cambiosRestantes: Int,
        diasExtension: Int
    ): Boolean {
        return try {
            val subscriptionRef = db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription")
                .document("current")

            val subscriptionDoc = subscriptionRef.get().await()

            if (!subscriptionDoc.exists()) {
                return false
            }

            // Obtener fecha de vencimiento actual y extenderla
            val fechaVencimientoActual = subscriptionDoc.getTimestamp("fechaVencimiento")
            val calendar = Calendar.getInstance()

            if (fechaVencimientoActual != null) {
                calendar.time = fechaVencimientoActual.toDate()
            }

            calendar.add(Calendar.DAY_OF_MONTH, diasExtension)
            val nuevaFechaVencimiento = Timestamp(calendar.time)

            // Actualizar suscripción
            val actualizacion = mutableMapOf<String, Any>()
            actualizacion["cambiosRestantes"] = cambiosRestantes
            actualizacion["fechaVencimiento"] = nuevaFechaVencimiento
            actualizacion["activa"] = true

            subscriptionRef.update(actualizacion).await()

            // Registrar modificación en historial
            val historyData = mutableMapOf<String, Any>()
            historyData.putAll(actualizacion)
            historyData["lubricentroId"] = lubricentroId
            historyData["fechaModificacion"] = Timestamp.now()
            historyData["tipoOperacion"] = "Modificación Manual"

            db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription_history")
                .add(historyData)
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error modificando suscripción", e)
            false
        }
    }

    /**
     * Desactiva una suscripción
     */
    suspend fun desactivarSuscripcion(lubricentroId: String): Boolean {
        return try {
            val subscriptionRef = db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription")
                .document("current")

            subscriptionRef.update("activa", false).await()

            // Registrar en el historial
            val historyData = mapOf(
                "lubricentroId" to lubricentroId,
                "fechaModificacion" to Timestamp.now(),
                "tipoOperacion" to "Desactivación",
                "activa" to false
            )

            db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription_history")
                .add(historyData)
                .await()

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error desactivando suscripción", e)
            false
        }
    }

    /**
     * Obtiene el historial de suscripciones de un lubricentro
     */
    suspend fun getSubscriptionHistory(lubricentroId: String): List<Map<String, Any>> {
        return try {
            val historySnapshot = db.collection("lubricentros")
                .document(lubricentroId)
                .collection("subscription_history")
                .orderBy("fechaCreacion")
                .get()
                .await()

            historySnapshot.documents.map { doc ->
                val data = doc.data ?: mapOf()
                val result = mutableMapOf<String, Any>()
                result.putAll(data)
                result["id"] = doc.id
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo historial de suscripciones", e)
            emptyList()
        }
    }
}
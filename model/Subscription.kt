package com.example.hismaadmin.model

import com.google.firebase.Timestamp
import java.util.*

data class Subscription(
    val id: String = "",
    val lubricentroId: String = "",
    val cambiosRestantes: Int = 0,
    val cambiosUtilizados: Int = 0,
    val fechaVencimiento: Timestamp = Timestamp.now(),
    val fechaCreacion: Timestamp = Timestamp.now(),
    val activa: Boolean = true,
    val planId: String = "",
    val tipoSuscripcion: String = ""
) {
    fun isValid(): Boolean {
        val currentTime = Timestamp.now()
        return activa &&
                fechaVencimiento.compareTo(currentTime) > 0 &&
                cambiosRestantes > 0
    }

    fun getDiasRestantes(): Int {
        val currentTimeMillis = System.currentTimeMillis()
        val vencimientoMillis = fechaVencimiento.toDate().time
        val diffMillis = vencimientoMillis - currentTimeMillis
        return (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    }
}

/**
 * Modelo para definir planes predefinidos en la aplicación
 */
data class Plan(
    val id: String = "",
    val nombre: String = "",
    val descripcion: String = "",
    val diasDuracion: Int = 0,
    val cantidadCambios: Int = 0,
    val precio: Double = 0.0
)

/**
 * Planes predefinidos para la aplicación
 */
object PlanesPredefinidos {
    val PLAN_BASICO = Plan(
        id = "basico",
        nombre = "Plan Básico",
        descripcion = "Ideal para lubricentros pequeños",
        diasDuracion = 30,
        cantidadCambios = 20,
        precio = 5000.0
    )

    val PLAN_ESTANDAR = Plan(
        id = "estandar",
        nombre = "Plan Estándar",
        descripcion = "Para lubricentros medianos",
        diasDuracion = 30,
        cantidadCambios = 50,
        precio = 10000.0
    )

    val PLAN_PREMIUM = Plan(
        id = "premium",
        nombre = "Plan Premium",
        descripcion = "Para lubricentros grandes",
        diasDuracion = 30,
        cantidadCambios = 100,
        precio = 18000.0
    )

    val PLAN_ANUAL = Plan(
        id = "anual",
        nombre = "Plan Anual",
        descripcion = "Plan por todo el año",
        diasDuracion = 365,
        cantidadCambios = 500,
        precio = 150000.0
    )

    fun getAllPlanes(): List<Plan> {
        return listOf(PLAN_BASICO, PLAN_ESTANDAR, PLAN_PREMIUM, PLAN_ANUAL)
    }

    fun getPlanById(id: String): Plan? {
        return when (id) {
            "basico" -> PLAN_BASICO
            "estandar" -> PLAN_ESTANDAR
            "premium" -> PLAN_PREMIUM
            "anual" -> PLAN_ANUAL
            else -> null
        }
    }
}
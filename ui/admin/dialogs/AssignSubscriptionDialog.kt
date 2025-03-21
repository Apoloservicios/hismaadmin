package com.example.hismaadm.ui.admin.dialogs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hismaadm.model.LubricentroSubscription
import com.example.hismaadm.model.SubscriptionPlan
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubscriptionDialog(
    lubricentroId: String,
    currentSubscription: LubricentroSubscription?,
    onDismiss: () -> Unit,
    onAssign: () -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<SubscriptionPlan>>(emptyList()) }
    var selectedPlanId by remember { mutableStateOf("") }
    var duracionMeses by remember { mutableStateOf(1) }
    var metodoPago by remember { mutableStateOf("Transferencia Bancaria") }
    // Nuevo estado para controlar si se extiende la suscripción actual
    var extendCurrentSubscription by remember { mutableStateOf(false) }

    // Cargar planes disponibles
    LaunchedEffect(Unit) {
        try {
            // Simulamos planes para pruebas
            // En una implementación real, cargamos desde Firestore
            plans = listOf(
                SubscriptionPlan(
                    id = "plan1",
                    nombre = "Plan Básico",
                    descripcion = "Para lubricentros pequeños",
                    precio = 1999.0,
                    duracionMeses = 1,
                    limiteEmpleados = 3,
                    limiteCambiosAceite = 100
                ),
                SubscriptionPlan(
                    id = "plan2",
                    nombre = "Plan Estándar",
                    descripcion = "Para lubricentros medianos",
                    precio = 3999.0,
                    duracionMeses = 1,
                    limiteEmpleados = 5,
                    limiteCambiosAceite = 300
                ),
                SubscriptionPlan(
                    id = "plan3",
                    nombre = "Plan Premium",
                    descripcion = "Para lubricentros grandes",
                    precio = 7999.0,
                    duracionMeses = 1,
                    limiteEmpleados = 10,
                    limiteCambiosAceite = 1000
                )
            )

            // Seleccionar plan actual o el primero
            selectedPlanId = currentSubscription?.planId ?: plans.firstOrNull()?.id ?: ""

        } catch (e: Exception) {
            errorMessage = "Error al cargar planes: ${e.message}"
            Log.e("AssignSubscription", "Error cargando planes", e)
        } finally {
            isLoading = false
        }
    }

    // Guardar suscripción
    fun assignPlan() {
        if (selectedPlanId.isBlank()) {
            errorMessage = "Selecciona un plan"
            return
        }

        val selectedPlan = plans.find { it.id == selectedPlanId }
        if (selectedPlan == null) {
            errorMessage = "Plan seleccionado inválido"
            return
        }

        scope.launch {
            try {
                isLoading = true

                // Calcular fechas
                val now = Calendar.getInstance()
                val startDate = Timestamp(now.time)

                val endDate = Calendar.getInstance()
                // Si estamos extendiendo, usar la fecha fin actual como base
                if (currentSubscription != null && extendCurrentSubscription) {
                    val currentEndDate = currentSubscription.fechaFin.toDate()
                    endDate.time = currentEndDate
                } else {
                    endDate.time = now.time
                }
                // Añadir los meses de duración
                endDate.add(Calendar.MONTH, duracionMeses)
                val endTimestamp = Timestamp(endDate.time)

                if (currentSubscription != null && extendCurrentSubscription) {
                    // Extender suscripción existente
                    // Obtener los cambios disponibles actuales
                    val lubDoc = db.collection("lubricentros")
                        .document(lubricentroId)
                        .get()
                        .await()

                    val subscriptionMap = lubDoc.get("subscription") as? Map<*, *>
                    val currentAvailableChanges = (subscriptionMap?.get("availableChanges") as? Number)?.toInt() ?: 0

                    // Actualizar la suscripción existente en la colección "suscripciones"
                    val updateFields = mapOf(
                        "fechaFin" to endTimestamp,
                        "planId" to selectedPlan.id,
                        "planNombre" to selectedPlan.nombre,
                        "precio" to selectedPlan.precio,
                        "updatedAt" to Timestamp.now()
                    )

                    db.collection("suscripciones")
                        .document(currentSubscription.id)
                        .update(updateFields)
                        .await()

                    // Actualizar los datos de suscripción en el documento del lubricentro
                    val totalAvailableChanges = currentAvailableChanges + selectedPlan.limiteCambiosAceite
                    val subscriptionUpdateData = mapOf(
                        "subscription.plan" to selectedPlan.id,
                        "subscription.endDate" to endTimestamp,
                        "subscription.totalChangesAllowed" to selectedPlan.limiteCambiosAceite,
                        "subscription.availableChanges" to totalAvailableChanges,
                        "planActual" to selectedPlan.nombre
                    )

                    db.collection("lubricentros")
                        .document(lubricentroId)
                        .update(subscriptionUpdateData)
                        .await()

                } else {
                    // Crear nueva suscripción (desactivando la anterior si existe)

                    // Crear datos de la suscripción
                    // Crear datos de la suscripción
                    val subscriptionData = hashMapOf(
                        "lubricentroId" to lubricentroId,
                        "planId" to selectedPlan.id,
                        "planNombre" to selectedPlan.nombre,
                        "fechaInicio" to startDate,
                        "fechaFin" to endTimestamp,
                        "estado" to "activa",  // Añadir campo estado como string
                        "activa" to true,      // Mantener campo activa como booleano
                        "metodoPago" to metodoPago,
                        "precio" to selectedPlan.precio,
                        "createdAt" to Timestamp.now()
                    )

                    // Si hay una suscripción actual, la desactivamos
                    if (currentSubscription != null) {
                        db.collection("suscripciones")
                            .document(currentSubscription.id)
                            .update("activa", false)
                            .await()
                    }

                    // Crear nueva suscripción
                    db.collection("suscripciones")
                        .add(subscriptionData)
                        .await()

                    // Actualizar estado del lubricentro con datos desnormalizados
                    val subscription = mapOf(
                        "active" to true,
                        "plan" to selectedPlan.id,
                        "startDate" to startDate,
                        "endDate" to endTimestamp,
                        "totalChangesAllowed" to selectedPlan.limiteCambiosAceite,
                        "availableChanges" to selectedPlan.limiteCambiosAceite,
                        "changesUsed" to 0,
                        "remainingDays" to duracionMeses * 30,
                        "trialActivated" to false
                    )

                    val updateData = mapOf(
                        "subscription" to subscription,
                        "planActual" to selectedPlan.nombre,
                        "suscripcionActiva" to true
                    )

                    db.collection("lubricentros")
                        .document(lubricentroId)
                        .update(updateData)
                        .await()
                }

                onAssign()
            } catch (e: Exception) {
                errorMessage = "Error al asignar plan: ${e.message}"
                Log.e("AssignSubscription", "Error asignando plan", e)
            } finally {
                isLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (currentSubscription == null) "Asignar Plan" else "Renovar Plan") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Mostrar error si existe
                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                    }

                    // Lista de planes
                    Text(
                        text = "Selecciona un plan",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        plans.forEach { plan ->
                            val isSelected = plan.id == selectedPlanId
                            val backgroundColor = if (isSelected)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surface

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = backgroundColor
                                ),
                                onClick = { selectedPlanId = plan.id }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = plan.nombre,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = plan.descripcion,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                        Text(
                                            text = "${plan.limiteEmpleados} empleados · ${plan.limiteCambiosAceite} cambios",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Text(
                                        text = "$${plan.precio}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selección de duración
                    Text(
                        text = "Duración del plan",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Slider para duración
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("1 mes")
                        Slider(
                            value = duracionMeses.toFloat(),
                            onValueChange = { duracionMeses = it.toInt() },
                            valueRange = 1f..12f,
                            steps = 11,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )
                        Text("12 meses")
                    }
                    Text("Duración seleccionada: $duracionMeses meses")

                    Spacer(modifier = Modifier.height(16.dp))

                    // Método de pago
                    Text(
                        text = "Método de pago",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    var expandedMetodoPago by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expandedMetodoPago,
                        onExpandedChange = { expandedMetodoPago = it }
                    ) {
                        OutlinedTextField(
                            value = metodoPago,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Método de pago") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMetodoPago)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = expandedMetodoPago,
                            onDismissRequest = { expandedMetodoPago = false }
                        ) {
                            listOf(
                                "Transferencia Bancaria",
                                "Tarjeta de Crédito",
                                "Efectivo"
                            ).forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option) },
                                    onClick = {
                                        metodoPago = option
                                        expandedMetodoPago = false
                                    }
                                )
                            }
                        }
                    }

                    // Añadir opción para extender suscripción si hay una actual
                    if (currentSubscription != null) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (extendCurrentSubscription)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Extender suscripción actual",
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = extendCurrentSubscription,
                                        onCheckedChange = { extendCurrentSubscription = it }
                                    )
                                }

                                Text(
                                    text = if (extendCurrentSubscription)
                                        "Se extenderá la fecha de vencimiento y se añadirán los cambios de aceite a la suscripción actual"
                                    else
                                        "Se desactivará la suscripción actual y se creará una nueva",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                // Mostrar datos actuales
                                if (extendCurrentSubscription) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Detalles de extensión:",
                                        style = MaterialTheme.typography.titleSmall
                                    )

                                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                    val currentEndDateStr = sdf.format(currentSubscription.fechaFin.toDate())

                                    val newEndDate = Calendar.getInstance()
                                    newEndDate.time = currentSubscription.fechaFin.toDate()
                                    newEndDate.add(Calendar.MONTH, duracionMeses)
                                    val newEndDateStr = sdf.format(newEndDate.time)

                                    Text(
                                        text = "Fecha fin actual: $currentEndDateStr",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "Nueva fecha fin: $newEndDateStr",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Resumen de la compra
                    plans.find { it.id == selectedPlanId }?.let { selectedPlan ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "Resumen",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Plan:")
                                    Text(selectedPlan.nombre)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Duración:")
                                    Text("$duracionMeses meses")
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Precio mensual:")
                                    Text("$${selectedPlan.precio}")
                                }

                                HorizontalDivider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total:",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = "$${selectedPlan.precio * duracionMeses}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { assignPlan() },
                enabled = !isLoading && selectedPlanId.isNotBlank()
            ) {
                Text(if (currentSubscription == null) "Asignar Plan" else "Renovar Plan")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
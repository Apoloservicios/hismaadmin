package com.example.hismaadm.ui.admin.dialogs

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.hismaadm.model.LubricentroSubscription
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSubscriptionDialog(
    subscription: LubricentroSubscription,
    onDismiss: () -> Unit,
    onSave: (LubricentroSubscription) -> Unit
) {
    // Añade logs para verificar los valores iniciales
    Log.d("SubscriptionDialog", "Valores iniciales: limiteEmpleados=${subscription.limiteEmpleados}, limiteCambios=${subscription.limiteCambiosAceite}")

    var fechaFin by remember { mutableStateOf(subscription.fechaFin.toDate()) }
    var activa by remember { mutableStateOf(subscription.activa) }
    var limiteEmpleados by remember { mutableStateOf(subscription.limiteEmpleados.toString()) }
    var limiteCambiosAceite by remember { mutableStateOf(subscription.limiteCambiosAceite.toString()) }

    // Mejorar el selector de fecha
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Sistema de fecha personalizado (ya que el DatePicker puede estar causando problemas)
    var day by remember { mutableStateOf(Calendar.getInstance().apply { time = fechaFin }.get(Calendar.DAY_OF_MONTH).toString()) }
    var month by remember { mutableStateOf((Calendar.getInstance().apply { time = fechaFin }.get(Calendar.MONTH) + 1).toString()) }
    var year by remember { mutableStateOf(Calendar.getInstance().apply { time = fechaFin }.get(Calendar.YEAR).toString()) }

    // Función para actualizar la fecha
    fun updateDate() {
        try {
            val cal = Calendar.getInstance()
            cal.set(
                year.toIntOrNull() ?: cal.get(Calendar.YEAR),
                (month.toIntOrNull() ?: 1) - 1,  // El mes en Calendar es 0-based
                day.toIntOrNull() ?: cal.get(Calendar.DAY_OF_MONTH)
            )
            fechaFin = cal.time
        } catch (e: Exception) {
            Log.e("DatePicker", "Error al actualizar fecha: ${e.message}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Suscripción") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Plan: ${subscription.planNombre}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Valores actuales (para comparación)
                Text(
                    text = "Valores actuales:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text("Fecha fin: ${dateFormatter.format(subscription.fechaFin.toDate())}")
                Text("Límite empleados: ${subscription.limiteEmpleados}")
                Text("Límite cambios: ${subscription.limiteCambiosAceite}")
                Text("Estado: ${if(subscription.activa) "Activa" else "Inactiva"}")

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nuevos valores:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                // Selector de fecha manual
                Text("Fecha de fin:", style = MaterialTheme.typography.bodyMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Día
                    OutlinedTextField(
                        value = day,
                        onValueChange = {
                            if (it.isEmpty() || (it.all { c -> c.isDigit() } && it.toIntOrNull() in 1..31)) {
                                day = it
                                updateDate()
                            }
                        },
                        label = { Text("Día") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Mes
                    OutlinedTextField(
                        value = month,
                        onValueChange = {
                            if (it.isEmpty() || (it.all { c -> c.isDigit() } && it.toIntOrNull() in 1..12)) {
                                month = it
                                updateDate()
                            }
                        },
                        label = { Text("Mes") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Año
                    OutlinedTextField(
                        value = year,
                        onValueChange = {
                            if (it.isEmpty() || (it.all { c -> c.isDigit() } && it.length <= 4)) {
                                year = it
                                updateDate()
                            }
                        },
                        label = { Text("Año") },
                        modifier = Modifier.weight(1.5f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                Text("Fecha seleccionada: ${dateFormatter.format(fechaFin)}")

                Spacer(modifier = Modifier.height(16.dp))

                // Límite de empleados
                OutlinedTextField(
                    value = limiteEmpleados,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            limiteEmpleados = it
                        }
                    },
                    label = { Text("Límite de empleados") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Límite de cambios de aceite
                OutlinedTextField(
                    value = limiteCambiosAceite,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            limiteCambiosAceite = it
                        }
                    },
                    label = { Text("Límite de cambios de aceite") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Estado activo/inactivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Estado de suscripción",
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = activa,
                        onCheckedChange = { activa = it }
                    )
                    Text(if (activa) "Activa" else "Inactiva")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Log los valores que se van a guardar
                    Log.d("SubscriptionDialog", "Guardando: limiteEmpleados=$limiteEmpleados, limiteCambios=$limiteCambiosAceite, fecha=${dateFormatter.format(fechaFin)}")

                    // Crear copia actualizada
                    val updatedSubscription = subscription.copy(
                        fechaFin = Timestamp(fechaFin),
                        activa = activa,
                        limiteEmpleados = limiteEmpleados.toIntOrNull() ?: subscription.limiteEmpleados,
                        limiteCambiosAceite = limiteCambiosAceite.toIntOrNull() ?: subscription.limiteCambiosAceite
                    )
                    onSave(updatedSubscription)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DatePickerDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        text = { content() }
    )
}

@Composable
fun rememberDatePickerState(initialSelectedDateMillis: Long? = null) = remember {
    object {
        var selectedDateMillis: Long? = initialSelectedDateMillis
    }
}

@Composable
fun DatePicker(
    state: Any,
    title: @Composable () -> Unit,
    headline: @Composable () -> Unit,
    showModeToggle: Boolean,
    onDateSelected: (Long?) -> Unit
) {
    // Implementación simplificada
    val calendar = Calendar.getInstance()

    Column {
        // Cabecera
        title()
        headline()

        // Selector de año
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            (calendar.get(Calendar.YEAR) - 5..calendar.get(Calendar.YEAR) + 5).forEach { year ->
                TextButton(
                    onClick = {
                        calendar.set(Calendar.YEAR, year)
                        onDateSelected(calendar.timeInMillis)
                    }
                ) {
                    Text(year.toString())
                }
            }
        }

        // Selector de mes
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val months = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
            months.forEachIndexed { index, month ->
                TextButton(
                    onClick = {
                        calendar.set(Calendar.MONTH, index)
                        onDateSelected(calendar.timeInMillis)
                    }
                ) {
                    Text(month)
                }
            }
        }

        // Selector de día
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            (1..30).forEach { day ->
                TextButton(
                    onClick = {
                        calendar.set(Calendar.DAY_OF_MONTH, day)
                        onDateSelected(calendar.timeInMillis)
                    }
                ) {
                    Text(day.toString())
                }
            }
        }
    }
}
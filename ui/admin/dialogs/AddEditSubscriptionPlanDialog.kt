package com.example.hismaadm.ui.admin.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.hismaadm.model.SubscriptionPlan

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionPlanDialog2(
    plan: SubscriptionPlan?,
    onDismiss: () -> Unit,
    onSave: (SubscriptionPlan) -> Unit
) {
    val isNew = plan == null
    var nombre by remember { mutableStateOf(plan?.nombre ?: "") }
    var descripcion by remember { mutableStateOf(plan?.descripcion ?: "") }
    var precio by remember { mutableStateOf((plan?.precio ?: 0.0).toString()) }
    var duracionMeses by remember { mutableStateOf((plan?.duracionMeses ?: 1).toString()) }
    var limiteEmpleados by remember { mutableStateOf((plan?.limiteEmpleados ?: 3).toString()) }
    var limiteCambiosAceite by remember { mutableStateOf((plan?.limiteCambiosAceite ?: 100).toString()) }
    var activo by remember { mutableStateOf(plan?.activo ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Nuevo Plan" else "Editar Plan") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = descripcion,
                    onValueChange = { descripcion = it },
                    label = { Text("Descripción") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = precio,
                    onValueChange = { precio = it },
                    label = { Text("Precio") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = duracionMeses,
                    onValueChange = { duracionMeses = it },
                    label = { Text("Duración (meses)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = limiteEmpleados,
                    onValueChange = { limiteEmpleados = it },
                    label = { Text("Límite de empleados") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = limiteCambiosAceite,
                    onValueChange = { limiteCambiosAceite = it },
                    label = { Text("Límite de cambios de aceite") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text("Activo", modifier = Modifier.weight(1f))
                    Switch(checked = activo, onCheckedChange = { activo = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val precioDouble = precio.toDoubleOrNull() ?: 0.0
                    val duracionInt = duracionMeses.toIntOrNull() ?: 1
                    val limiteEmpInt = limiteEmpleados.toIntOrNull() ?: 3
                    val limiteCambiosInt = limiteCambiosAceite.toIntOrNull() ?: 100

                    val newPlan = SubscriptionPlan(
                        id = plan?.id ?: "",
                        nombre = nombre,
                        descripcion = descripcion,
                        precio = precioDouble,
                        duracionMeses = duracionInt,
                        limiteEmpleados = limiteEmpInt,
                        limiteCambiosAceite = limiteCambiosInt,
                        activo = activo
                    )
                    onSave(newPlan)
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
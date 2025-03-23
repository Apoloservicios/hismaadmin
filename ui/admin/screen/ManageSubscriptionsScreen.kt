package com.example.hismaadmin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadmin.model.Lubricentro
import com.example.hismaadmin.model.Plan
import com.example.hismaadmin.model.PlanesPredefinidos
import com.example.hismaadmin.model.Subscription
import com.example.hismaadmin.utils.SubscriptionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageSubscriptionsScreen(
    navController: NavController,
    lubricentro: Lubricentro
) {
    val db = FirebaseFirestore.getInstance()
    val subscriptionManager = remember { SubscriptionManager(db) }
    val scope = rememberCoroutineScope()

    var subscription by remember { mutableStateOf<Subscription?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showApplyPlanDialog by remember { mutableStateOf(false) }
    var showModifyDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var subscriptionHistory by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    // Cargar suscripción actual
    LaunchedEffect(lubricentro.uid) {
        isLoading = true
        subscription = subscriptionManager.getCurrentSubscription(lubricentro.uid)
        isLoading = false
    }

    // Función para recargar los datos
    fun reloadData() {
        scope.launch {
            isLoading = true
            subscription = subscriptionManager.getCurrentSubscription(lubricentro.uid)
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestión de Suscripción") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Información del lubricentro
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = lubricentro.nombreFantasia,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "CUIT: ${lubricentro.cuit}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Email: ${lubricentro.email}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Text(
                        text = "Teléfono: ${lubricentro.telefono}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Información de suscripción
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Información de Suscripción",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (subscription != null) {
                            val sub = subscription!!
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val fechaVencimiento = dateFormat.format(sub.fechaVencimiento.toDate())
                            val fechaCreacion = dateFormat.format(sub.fechaCreacion.toDate())

                            // Estado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estado:",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                val isValid = sub.isValid()
                                val statusColor = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                                val statusText = if (isValid) "Activa" else "Inactiva"

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = statusColor
                                )

                                Spacer(modifier = Modifier.weight(1f))

                                // Icono de estado
                                Icon(
                                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = statusText,
                                    tint = statusColor
                                )
                            }

                            Divider(modifier = Modifier.padding(vertical = 8.dp))

                            // Detalles de la suscripción
                            SubscriptionDetailRow("Plan", sub.tipoSuscripcion)
                            SubscriptionDetailRow("Cambios restantes", "${sub.cambiosRestantes}")
                            SubscriptionDetailRow("Cambios utilizados", "${sub.cambiosUtilizados}")
                            SubscriptionDetailRow("Fecha de creación", fechaCreacion)
                            SubscriptionDetailRow("Fecha de vencimiento", fechaVencimiento)
                            SubscriptionDetailRow("Días restantes", "${sub.getDiasRestantes()}")

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                OutlinedButton(
                                    onClick = { showModifyDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Modificar")
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Button(
                                    onClick = { showApplyPlanDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Aplicar Plan")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        subscriptionHistory = subscriptionManager.getSubscriptionHistory(lubricentro.uid)
                                        showHistoryDialog = true
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ver Historial")
                            }

                        } else {
                            Text(
                                text = "No hay suscripción activa",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { showApplyPlanDialog = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aplicar Plan")
                            }
                        }
                    }
                }

                // Sección de Acciones Rápidas
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Acciones Rápidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Botones de planes predefinidos
                        Column(modifier = Modifier.fillMaxWidth()) {
                            PlanesPredefinidos.getAllPlanes().forEach { plan ->
                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            isLoading = true
                                            val success = subscriptionManager.aplicarNuevoPlan(lubricentro.uid, plan)
                                            isLoading = false
                                            if (success) {
                                                reloadData()
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text("${plan.nombre} (${plan.cantidadCambios} cambios, ${plan.diasDuracion} días)")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (subscription != null) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        val success = subscriptionManager.desactivarSuscripcion(lubricentro.uid)
                                        isLoading = false
                                        if (success) {
                                            reloadData()
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) {
                                Text("Desactivar Suscripción")
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo para aplicar un plan
    if (showApplyPlanDialog) {
        ApplyPlanDialog(
            planes = PlanesPredefinidos.getAllPlanes(),
            onDismiss = { showApplyPlanDialog = false },
            onApplyPlan = { plan ->
                scope.launch {
                    isLoading = true
                    val success = subscriptionManager.aplicarNuevoPlan(lubricentro.uid, plan)
                    isLoading = false
                    if (success) {
                        reloadData()
                    }
                    showApplyPlanDialog = false
                }
            }
        )
    }

    // Diálogo para modificar suscripción
    if (showModifyDialog && subscription != null) {
        ModifySubscriptionDialog(
            subscription = subscription!!,
            onDismiss = { showModifyDialog = false },
            onModify = { cambiosRestantes, diasExtension ->
                scope.launch {
                    isLoading = true
                    val success = subscriptionManager.modificarSuscripcion(
                        lubricentro.uid,
                        cambiosRestantes,
                        diasExtension
                    )
                    isLoading = false
                    if (success) {
                        reloadData()
                    }
                    showModifyDialog = false
                }
            }
        )
    }

    // Diálogo para ver historial
    if (showHistoryDialog) {
        SubscriptionHistoryDialog(
            history = subscriptionHistory,
            onDismiss = { showHistoryDialog = false }
        )
    }
}

@Composable
fun SubscriptionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ApplyPlanDialog(
    planes: List<Plan>,
    onDismiss: () -> Unit,
    onApplyPlan: (Plan) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar Plan") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("Selecciona un plan para aplicar:")

                Spacer(modifier = Modifier.height(16.dp))

                planes.forEach { plan ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onApplyPlan(plan) }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            Text(
                                text = plan.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = plan.descripcion,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Cambios: ${plan.cantidadCambios}")
                                Text("Duración: ${plan.diasDuracion} días")
                            }

                            Text(
                                text = "Precio: $${plan.precio}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
        dismissButton = {}
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModifySubscriptionDialog(
    subscription: Subscription,
    onDismiss: () -> Unit,
    onModify: (cambiosRestantes: Int, diasExtension: Int) -> Unit
) {
    var cambiosRestantes by remember { mutableStateOf(subscription.cambiosRestantes.toString()) }
    var diasExtension by remember { mutableStateOf("0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar Suscripción") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = cambiosRestantes,
                    onValueChange = { cambiosRestantes = it.filter { char -> char.isDigit() } },
                    label = { Text("Cambios Restantes") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = diasExtension,
                    onValueChange = { diasExtension = it.filter { char -> char.isDigit() } },
                    label = { Text("Días de Extensión") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Esto modificará la suscripción actual. Los cambios restantes se establecerán al valor indicado y la fecha de vencimiento se extenderá por los días especificados.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cambios = cambiosRestantes.toIntOrNull() ?: subscription.cambiosRestantes
                    val dias = diasExtension.toIntOrNull() ?: 0
                    onModify(cambios, dias)
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
fun SubscriptionHistoryDialog(
    history: List<Map<String, Any>>,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial de Suscripciones") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                if (history.isEmpty()) {
                    Text(
                        text = "No hay registros en el historial",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                } else {
                    history.forEach { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                // Determinamos qué tipo de operación fue
                                val tipoOperacion = item["tipoOperacion"] as? String ?: "Plan Aplicado"
                                val fechaModif = item["fechaModificacion"] as? com.google.firebase.Timestamp
                                val fechaCreacion = item["fechaCreacion"] as? com.google.firebase.Timestamp
                                val fechaTimestamp = fechaModif ?: fechaCreacion
                                val fecha = if (fechaTimestamp != null) {
                                    dateFormat.format(fechaTimestamp.toDate())
                                } else {
                                    "Fecha desconocida"
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = tipoOperacion,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Text(
                                        text = fecha,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 4.dp))

                                // Mostramos los detalles relevantes según el tipo de operación
                                if (tipoOperacion == "Desactivación") {
                                    Text(
                                        text = "La suscripción fue desactivada",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.Red
                                    )
                                } else {
                                    // Para planes y modificaciones mostramos los detalles
                                    val cambiosRestantes = item["cambiosRestantes"] as? Number
                                    val fechaVencimiento = item["fechaVencimiento"] as? com.google.firebase.Timestamp
                                    val tipoSuscripcion = item["tipoSuscripcion"] as? String
                                    val planId = item["planId"] as? String

                                    if (tipoSuscripcion != null) {
                                        Text("Plan: $tipoSuscripcion")
                                    } else if (planId != null) {
                                        Text("Plan ID: $planId")
                                    }

                                    if (cambiosRestantes != null) {
                                        Text("Cambios asignados: ${cambiosRestantes.toInt()}")
                                    }

                                    if (fechaVencimiento != null) {
                                        Text("Vencimiento: ${dateFormat.format(fechaVencimiento.toDate())}")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        dismissButton = {}
    )
}
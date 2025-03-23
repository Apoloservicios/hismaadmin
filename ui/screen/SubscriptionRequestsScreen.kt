package com.example.hismaadm.ui.screen

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadm.model.SubscriptionRequest
import com.example.hismaadm.utils.SubscriptionManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionRequestsScreen(navController: NavController, subscriptionManager: SubscriptionManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var requests by remember { mutableStateOf<List<SubscriptionRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedRequest by remember { mutableStateOf<SubscriptionRequest?>(null) }
    var showApproveDialog by remember { mutableStateOf(false) }

    // Cargar solicitudes al iniciar
    LaunchedEffect(Unit) {
        scope.launch {
            isLoading = true
            try {
                val result = subscriptionManager.getPendingRequests()
                requests = result
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    // Función para recargar solicitudes
    fun reloadRequests() {
        scope.launch {
            isLoading = true
            try {
                val result = subscriptionManager.getPendingRequests()
                requests = result
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitudes de Suscripción") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                if (requests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay solicitudes pendientes",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn {
                        items(requests) { request ->
                            SubscriptionRequestCard(
                                request = request,
                                onApprove = {
                                    selectedRequest = request
                                    showApproveDialog = true
                                },
                                onReject = {
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val success = subscriptionManager.rejectSubscriptionRequest(request.id)
                                            if (success) {
                                                Toast.makeText(context, "Solicitud rechazada", Toast.LENGTH_SHORT).show()
                                                reloadRequests()
                                            } else {
                                                Toast.makeText(context, "Error al rechazar solicitud", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showApproveDialog && selectedRequest != null) {
        ApproveSubscriptionDialog(
            request = selectedRequest!!,
            onDismiss = { showApproveDialog = false },
            onApprove = { cambiosTotales, duracionMeses ->
                scope.launch {
                    isLoading = true
                    try {
                        val success = subscriptionManager.approveSubscriptionRequest(
                            selectedRequest!!.id,
                            cambiosTotales,
                            duracionMeses
                        )
                        if (success) {
                            Toast.makeText(context, "Solicitud aprobada con éxito", Toast.LENGTH_SHORT).show()
                            reloadRequests()
                        } else {
                            Toast.makeText(context, "Error al aprobar solicitud", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isLoading = false
                        showApproveDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun SubscriptionRequestCard(
    request: SubscriptionRequest,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Solicitud de ${if (request.isPaqueteAdicional) "Paquete Adicional" else "Suscripción"}",
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text("Lubricentro: ${request.lubricentroName}")
            Text("Plan: ${getTipoPlan(request.tipoSuscripcion)}")

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val createdAtStr = dateFormat.format(request.createdAt.toDate())

            Text("Fecha de solicitud: $createdAtStr")

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Aprobar")
                }

                Button(
                    onClick = onReject,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Rechazar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApproveSubscriptionDialog(
    request: SubscriptionRequest,
    onDismiss: () -> Unit,
    onApprove: (cambiosTotales: Int, duracionMeses: Int) -> Unit
) {
    var cambiosTotales by remember { mutableStateOf(getTotalCambios(request.tipoSuscripcion)) }
    var duracionMeses by remember { mutableStateOf(getDuracionMeses(request.tipoSuscripcion)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aprobar Solicitud") },
        text = {
            Column {
                Text("Tipo de suscripción: ${getTipoPlan(request.tipoSuscripcion)}")

                Spacer(modifier = Modifier.height(16.dp))

                Text("Cambios totales:")
                OutlinedTextField(
                    value = cambiosTotales.toString(),
                    onValueChange = {
                        cambiosTotales = it.toIntOrNull() ?: cambiosTotales
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text("Duración (meses):")
                OutlinedTextField(
                    value = duracionMeses.toString(),
                    onValueChange = {
                        duracionMeses = it.toIntOrNull() ?: duracionMeses
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onApprove(cambiosTotales, duracionMeses) }
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun getTipoPlan(tipoSuscripcion: String): String {
    return when (tipoSuscripcion) {
        "basica" -> "Plan Básico"
        "premium" -> "Plan Premium"
        "paquete50" -> "Paquete de 50 cambios"
        "paquete100" -> "Paquete de 100 cambios"
        else -> tipoSuscripcion
    }
}

fun getTotalCambios(tipoSuscripcion: String): Int {
    return when (tipoSuscripcion) {
        "basica" -> 100
        "premium" -> 300
        "paquete50" -> 50
        "paquete100" -> 100
        else -> 50
    }
}

fun getDuracionMeses(tipoSuscripcion: String): Int {
    return when (tipoSuscripcion) {
        "basica" -> 6
        "premium" -> 12
        "paquete50", "paquete100" -> 12
        else -> 6
    }
}


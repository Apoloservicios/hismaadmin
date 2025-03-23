package com.example.hismaadm.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadmin.model.Lubricentro
import com.example.hismaadmin.model.Subscription
import com.example.hismaadmin.ui.screens.ManageSubscriptionsScreen
import com.example.hismaadmin.ui.screens.SubscriptionDetailRow
import com.example.hismaadmin.utils.SubscriptionManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LubricentroDetailScreen(
    navController: NavController,
    lubricentroId: String
) {
    val db = FirebaseFirestore.getInstance()
    val subscriptionManager = remember { SubscriptionManager(db) }
    val scope = rememberCoroutineScope()

    var lubricentro by remember { mutableStateOf<Lubricentro?>(null) }
    var subscription by remember { mutableStateOf<Subscription?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showSubscriptionManagementScreen by remember { mutableStateOf(false) }

    // Cargar datos del lubricentro y suscripción
    LaunchedEffect(lubricentroId) {
        isLoading = true
        try {
            // Cargar datos del lubricentro
            val docLub = db.collection("lubricentros")
                .document(lubricentroId)
                .get()
                .await()

            if (docLub.exists()) {
                lubricentro = docLub.toObject(Lubricentro::class.java)?.copy(uid = docLub.id)

                // Cargar información de suscripción
                subscription = subscriptionManager.getCurrentSubscription(lubricentroId)
            }
        } catch (e: Exception) {
            // Manejar errores
        } finally {
            isLoading = false
        }
    }

    if (showSubscriptionManagementScreen && lubricentro != null) {
        ManageSubscriptionsScreen(
            navController = navController,
            lubricentro = lubricentro!!
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de Lubricentro") },
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
        } else if (lubricentro == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Lubricentro no encontrado")
            }
        } else {
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
                            text = lubricentro!!.nombreFantasia,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        InfoItem(
                            icon = Icons.Default.Person,
                            label = "Responsable",
                            value = lubricentro!!.responsable
                        )

                        InfoItem(
                            icon = Icons.Default.Email,
                            label = "Email",
                            value = lubricentro!!.email
                        )

                        InfoItem(
                            icon = Icons.Default.Phone,
                            label = "Teléfono",
                            value = lubricentro!!.telefono
                        )

                        InfoItem(
                            icon = Icons.Default.LocationOn,
                            label = "Dirección",
                            value = lubricentro!!.direccion
                        )

                        InfoItem(
                            icon = Icons.Default.Assignment,
                            label = "CUIT",
                            value = lubricentro!!.cuit
                        )
                    }
                }

                // Resumen de suscripción
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Estado de Suscripción",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (subscription != null) {
                                val isValid = subscription!!.isValid()
                                val statusColor = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                                val statusText = if (isValid) "Activa" else "Inactiva"

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                        contentDescription = statusText,
                                        tint = statusColor
                                    )

                                    Spacer(modifier = Modifier.width(4.dp))

                                    Text(
                                        text = statusText,
                                        color = statusColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            } else {
                                Text(
                                    text = "Sin suscripción",
                                    color = Color.Red,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        if (subscription != null) {
                            val sub = subscription!!
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val fechaVencimiento = dateFormat.format(sub.fechaVencimiento.toDate())

                            SubscriptionDetailRow("Plan", sub.tipoSuscripcion)
                            SubscriptionDetailRow("Cambios restantes", "${sub.cambiosRestantes}")
                            SubscriptionDetailRow("Vence el", fechaVencimiento)
                            SubscriptionDetailRow("Días restantes", "${sub.getDiasRestantes()}")

                            // Mostrar advertencia si está cerca de expirar
                            if (sub.cambiosRestantes <= 3 || sub.getDiasRestantes() <= 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFFFF3E0))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Advertencia",
                                        tint = Color(0xFFF57C00)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Suscripción próxima a vencer",
                                        color = Color(0xFFF57C00),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Este lubricentro no tiene una suscripción activa. Necesita activar un plan para poder usar el sistema.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { showSubscriptionManagementScreen = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gestionar Suscripción")
                        }
                    }
                }

                // Otras secciones podrían ir aquí (empleados, configuraciones, etc.)
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Acciones Adicionales",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { /* Navegar a pantalla de empleados */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.People, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Empleados")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { /* Navegar a pantalla de cambios de aceite */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Ver Cambios de Aceite")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { /* Implementar funcionalidad */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.Red
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Desactivar Cuenta")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InfoItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
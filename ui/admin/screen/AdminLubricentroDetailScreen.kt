package com.example.hismaadm.ui.admin.screen

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadm.model.LubricentroExtended
import com.example.hismaadm.model.LubricentroSubscription
import com.example.hismaadm.ui.admin.dialogs.AssignSubscriptionDialog
import com.example.hismaadm.ui.admin.dialogs.EditLubricentroDialog
import com.example.hismaadm.ui.admin.dialogs.EditSubscriptionDialog

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLubricentroDetailScreen(
    lubricentroId: String,
    navController: NavController
) {
    val tag = "LubricentroDetail"
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorState by remember { mutableStateOf<String?>(null) }
    var lubricentro by remember { mutableStateOf<LubricentroExtended?>(null) }
    var subscription by remember { mutableStateOf<LubricentroSubscription?>(null) }


    // Estados para diálogos
    var showEditDialog by remember { mutableStateOf(false) }
    var showAssignPlanDialog by remember { mutableStateOf(false) }
    var showEditSubscriptionDialog by remember { mutableStateOf(false) } // Añade esta línea

    // Función para cargar los datos del lubricentro y su suscripción
    fun loadData() {
        isLoading = true
        errorState = null

        scope.launch {
            Log.d(tag, "Cargando lubricentro con ID: $lubricentroId")
            if (lubricentroId.isBlank()) {
                errorState = "ID de lubricentro inválido"
                isLoading = false
                return@launch
            }

            try {
                // Cargar datos del lubricentro
                val docRef = db.collection("lubricentros").document(lubricentroId)
                val doc = docRef.get().await()

                if (doc.exists()) {
                    Log.d(tag, "Documento encontrado: ${doc.data}")

                    lubricentro = LubricentroExtended(
                        id = doc.id,
                        uid = doc.getString("uid") ?: doc.id,
                        nombreFantasia = doc.getString("nombreFantasia") ?: "",
                        responsable = doc.getString("responsable") ?: "",
                        cuit = doc.getString("cuit") ?: "",
                        direccion = doc.getString("direccion") ?: "",
                        telefono = doc.getString("telefono") ?: "",
                        email = doc.getString("email") ?: "",
                        logoUrl = doc.getString("logoUrl") ?: "",
                        activo = doc.getBoolean("activo") ?: true,
                        // Otros campos opcionales pueden ser null
                        verificado = doc.getBoolean("verificado") ?: false,
                        fechaRegistro = doc.getTimestamp("fechaRegistro") ?: com.google.firebase.Timestamp.now(),
                        ultimoAcceso = doc.getTimestamp("ultimoAcceso"),
                        totalCambiosAceite = 0,
                        totalEmpleados = 0,
                        planActual = "",
                        suscripcionActiva = false
                    )

                    // Intentar cargar suscripción
                    try {
                        // En tu función loadData() en AdminLubricentroDetailScreen.kt
                        // Cuando cargas la suscripción, asegúrate de obtener todos los campos correctamente:

                        val suscripcionQuery = db.collection("suscripciones")
                            .whereEqualTo("lubricentroId", lubricentroId)
                            .whereEqualTo("activa", true)
                            .limit(1)
                            .get()
                            .await()

                        if (!suscripcionQuery.isEmpty) {
                            val suscripcionDoc = suscripcionQuery.documents[0]
                            subscription = LubricentroSubscription(
                                id = suscripcionDoc.id,
                                lubricentroId = suscripcionDoc.getString("lubricentroId") ?: "",
                                planId = suscripcionDoc.getString("planId") ?: "",
                                planNombre = suscripcionDoc.getString("planNombre") ?: "",
                                fechaInicio = suscripcionDoc.getTimestamp("fechaInicio") ?: com.google.firebase.Timestamp.now(),
                                fechaFin = suscripcionDoc.getTimestamp("fechaFin") ?: com.google.firebase.Timestamp.now(),
                                activa = suscripcionDoc.getBoolean("activa") ?: false,
                                metodoPago = suscripcionDoc.getString("metodoPago") ?: "",
                                // Añadir los campos nuevos
                                limiteEmpleados = suscripcionDoc.getLong("limiteEmpleados")?.toInt() ?: 2,
                                limiteCambiosAceite = suscripcionDoc.getLong("limiteCambiosAceite")?.toInt() ?: 80
                            )

                            // También registra estos valores para depuración
                            Log.d("Subscription", "Cargando suscripción: limiteEmpleados=${subscription?.limiteEmpleados}, " +
                                    "limiteCambiosAceite=${subscription?.limiteCambiosAceite}")

                            lubricentro = lubricentro?.copy(
                                planActual = subscription?.planNombre ?: "",
                                suscripcionActiva = true,
                                suscripcionId = suscripcionDoc.id,
                                suscripcionFechaFin = suscripcionDoc.getTimestamp("fechaFin")
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error cargando suscripción: ${e.message}", e)
                        // No mostramos error al usuario porque es información secundaria
                    }
                } else {
                    errorState = "No se encontró el lubricentro"
                    Log.e(tag, "Documento no encontrado para ID: $lubricentroId")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error cargando lubricentro: ${e.message}", e)
                errorState = "Error: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar datos al iniciar
    LaunchedEffect(lubricentroId) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(lubricentro?.nombreFantasia ?: "Detalle de Lubricentro") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (lubricentro != null) {
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> CircularProgressIndicator()

                errorState != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorState ?: "Error desconocido",
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { navController.navigateUp() }) {
                            Text("Volver")
                        }
                    }
                }

                lubricentro == null -> {
                    Text("No se pudo cargar la información del lubricentro")
                }

                else -> {
                    // Creamos una referencia local no-nullable
                    val data = lubricentro!!

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Información básica
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Información Básica",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Nombre:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = data.nombreFantasia,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Responsable:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = data.responsable,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "CUIT:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = data.cuit,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Email:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = data.email,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Teléfono:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = data.telefono,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Dirección:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = data.direccion,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Estado:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = if (data.activo) "Activo" else "Inactivo",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (data.activo)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.error
                                    )
                                }

                                // Estado de verificación
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Verificado:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.width(120.dp)
                                    )
                                    Text(
                                        text = if (data.verificado) "Sí" else "No",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                        // Información de suscripción
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Suscripción",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                if (data.suscripcionActiva) {
                                    Text(
                                        text = "Plan: ${data.planActual}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Fecha fin si existe
                                    data.suscripcionFechaFin?.let { fechaFin ->
                                        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                                        val fechaFormateada = sdf.format(fechaFin.toDate())
                                        Text(
                                            text = "Vence: $fechaFormateada",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { showAssignPlanDialog = true }
                                    ) {
                                        Text("Renovar Plan")
                                    }

                                    Button(
                                        onClick = { showEditSubscriptionDialog = true },
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Text("Editar Suscripción")
                                    }
                                } else {
                                    Text(
                                        text = "No tiene suscripción activa",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.error
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Button(
                                        onClick = { showAssignPlanDialog = true }
                                    ) {
                                        Text("Asignar Plan")
                                    }
                                }
                            }
                        }

                        // Botones de acción
                        Button(
                            onClick = { showEditDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Text("Editar Información")
                        }

                        Button(
                            onClick = {
                                // Cambiar estado (activar/desactivar)
                                scope.launch {
                                    try {
                                        val newStatus = !data.activo
                                        db.collection("lubricentros")
                                            .document(lubricentroId)
                                            .update("activo", newStatus)
                                            .await()

                                        // Actualizar estado local
                                        lubricentro = lubricentro?.copy(activo = newStatus)
                                    } catch (e: Exception) {
                                        Log.e(tag, "Error cambiando estado", e)
                                        errorState = "Error al cambiar estado: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(if (data.activo) "Desactivar Lubricentro" else "Activar Lubricentro")
                        }
                    }
                }
            }
        }
    }

    // Mostrar el diálogo EditLubricentroDialog cuando showEditDialog es true
    if (showEditDialog && lubricentro != null) {
        EditLubricentroDialog(
            lubricentro = lubricentro!!,
            onDismiss = { showEditDialog = false },
            onSave = { updatedLubricentro ->
                scope.launch {
                    try {
                        val data = mapOf(
                            "nombreFantasia" to updatedLubricentro.nombreFantasia,
                            "responsable" to updatedLubricentro.responsable,
                            "cuit" to updatedLubricentro.cuit,
                            "direccion" to updatedLubricentro.direccion,
                            "telefono" to updatedLubricentro.telefono,
                            "email" to updatedLubricentro.email,
                            "activo" to updatedLubricentro.activo,
                            "verificado" to updatedLubricentro.verificado
                        )

                        db.collection("lubricentros")
                            .document(lubricentroId)
                            .update(data)
                            .await()

                        // Recargar datos
                        loadData()
                    } catch (e: Exception) {
                        Log.e("LubricentroDetail", "Error actualizando lubricentro", e)
                        errorState = "Error al guardar: ${e.message}"
                    }

                    showEditDialog = false
                }
            }
        )
    }

    // Mostrar el diálogo AssignSubscriptionDialog cuando showAssignPlanDialog es true
    if (showAssignPlanDialog) {
        AssignSubscriptionDialog(
            lubricentroId = lubricentroId,
            currentSubscription = subscription,
            onDismiss = { showAssignPlanDialog = false },
            onAssign = {
                // Recargar datos
                loadData()
                showAssignPlanDialog = false
            }
        )
    }
    if (showEditSubscriptionDialog) {
        val currentSubscription = subscription
        if (currentSubscription != null) {
            EditSubscriptionDialog(
                subscription = currentSubscription,
                onDismiss = { showEditSubscriptionDialog = false },
                onSave = { updatedSubscription ->
                    scope.launch {
                        try {
                            Log.d("Subscription", "Actualizando suscripción con ID: ${updatedSubscription.id}")
                            Log.d("Subscription", "Valores a actualizar: " +
                                    "limiteEmpleados=${updatedSubscription.limiteEmpleados}, " +
                                    "limiteCambios=${updatedSubscription.limiteCambiosAceite}, " +
                                    "fechaFin=${updatedSubscription.fechaFin.toDate()}")

                            // 1. Actualizar la colección "suscripciones"
                            val suscripcionesUpdate = mapOf(
                                "activa" to updatedSubscription.activa,
                                "fechaFin" to updatedSubscription.fechaFin,
                                "limiteCambiosAceite" to updatedSubscription.limiteCambiosAceite,
                                "limiteEmpleados" to updatedSubscription.limiteEmpleados
                            )

                            db.collection("suscripciones")
                                .document(updatedSubscription.id)
                                .update(suscripcionesUpdate)
                                .await()

                            Log.d("Subscription", "Suscripción actualizada correctamente")

                            // 2. Actualizar todos los campos posibles en el documento del lubricentro
                            val lubricentroUpdates = mapOf(
                                // Campo subscription
                                "subscription.limiteEmpleados" to updatedSubscription.limiteEmpleados,
                                "subscription.totalChangesAllowed" to updatedSubscription.limiteCambiosAceite,
                                "subscription.endDate" to updatedSubscription.fechaFin,
                                "subscription.active" to updatedSubscription.activa,

                                // Campos directos (por si existen)
                                "limiteEmpleados" to updatedSubscription.limiteEmpleados,
                                "limiteCambiosAceite" to updatedSubscription.limiteCambiosAceite,
                                "suscripcionFechaFin" to updatedSubscription.fechaFin,
                                "suscripcionActiva" to updatedSubscription.activa
                            )

                            db.collection("lubricentros")
                                .document(lubricentroId)
                                .update(lubricentroUpdates)
                                .await()

                            Log.d("Subscription", "Lubricentro actualizado correctamente")

                            // Actualizar variable local y recargar
                            subscription = updatedSubscription
                            loadData()

                        } catch (e: Exception) {
                            Log.e("Subscription", "Error actualizando suscripción: ${e.message}", e)
                            errorState = "Error al guardar suscripción: ${e.message}"
                        }

                        showEditSubscriptionDialog = false
                    }
                }
            )
        } else {
            showEditSubscriptionDialog = false
        }
    }

}
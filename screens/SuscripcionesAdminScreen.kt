package com.example.hismaadm.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuscripcionesAdminScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    // Estados para la UI
    var solicitudes by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            // ⚠️ IMPORTANTE: Agregar logging para debug
            Log.d("SuscripcionesAdmin", "Cargando solicitudes pendientes...")

            // Cargar solicitudes pendientes
            val snapshot = db.collection("solicitudesSuscripcion")
                .whereEqualTo("estado", "pendiente")
                .get()
                .await()

            Log.d("SuscripcionesAdmin", "Solicitudes encontradas: ${snapshot.documents.size}")

            val tempList = mutableListOf<Map<String, Any>>()
            for (doc in snapshot.documents) {
                Log.d("SuscripcionesAdmin", "Documento: ${doc.id}, datos: ${doc.data}")
                if (doc.data != null) {
                    // Crear un mapa con los datos más el ID
                    val itemData = HashMap<String, Any>(doc.data!!)
                    itemData["id"] = doc.id
                    tempList.add(itemData)
                }
            }
            solicitudes = tempList

            // ⚠️ Verificar el resultado final
            Log.d("SuscripcionesAdmin", "Lista de solicitudes final: ${solicitudes.size}")
        } catch (e: Exception) {
            // Manejo de error con logging detallado
            Log.e("SuscripcionesAdmin", "Error al cargar solicitudes: ${e.message}", e)
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solicitudes de Suscripción") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            if (solicitudes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay solicitudes pendientes")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    items(solicitudes) { solicitud ->
                        SolicitudCard(
                            solicitud = solicitud,
                            onAprobar = {
                                scope.launch {
                                    try {
                                        val solicitudId = solicitud["id"] as String

                                        // Actualizar estado de la solicitud
                                        // Actualizar estado de la solicitud
                                        db.collection("solicitudesSuscripcion")
                                            .document(solicitudId)
                                            .update(
                                                mapOf(
                                                    "estado" to "aprobada",
                                                    "activa" to true,      // Añadir este campo
                                                    "updatedAt" to Timestamp.now()
                                                )
                                            ).await()

                                        // Refrescar la lista
                                        val newList = solicitudes.filter {
                                            it["id"] != solicitudId
                                        }
                                        solicitudes = newList

                                        Toast.makeText(
                                            context,
                                            "Solicitud aprobada",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Error: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            onRechazar = {
                                scope.launch {
                                    try {
                                        val solicitudId = solicitud["id"] as String

                                        // Actualizar estado de la solicitud
                                        // Actualizar estado de la solicitud
                                        db.collection("solicitudesSuscripcion")
                                            .document(solicitudId)
                                            .update(
                                                mapOf(
                                                    "estado" to "rechazada",
                                                    "activa" to false,     // Añadir este campo
                                                    "updatedAt" to Timestamp.now()
                                                )
                                            ).await()

                                        // Refrescar la lista
                                        val newList = solicitudes.filter {
                                            it["id"] != solicitudId
                                        }
                                        solicitudes = newList

                                        Toast.makeText(
                                            context,
                                            "Solicitud rechazada",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Error: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
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

@Composable
fun SolicitudCard(
    solicitud: Map<String, Any>,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
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
                text = "Lubricentro ID: ${(solicitud["lubricentroId"] as? String)?.take(8) ?: ""}...",
                style = MaterialTheme.typography.titleMedium
            )

            val tipoSuscripcion = solicitud["tipoSuscripcion"] as? String ?: ""
            val isPaqueteAdicional = solicitud["isPaqueteAdicional"] as? Boolean ?: false

            Text("Tipo: ${if (isPaqueteAdicional) "Paquete Adicional" else "Suscripción completa"}")
            Text("Plan: $tipoSuscripcion")

            val createdAt = solicitud["createdAt"] as? Timestamp
            val fechaSolicitud = if (createdAt != null) {
                SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    .format(createdAt.toDate())
            } else {
                "Fecha desconocida"
            }

            Text("Fecha: $fechaSolicitud")

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onRechazar,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Rechazar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rechazar")
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onAprobar,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Green
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Aprobar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Aprobar")
                }
            }
        }
    }
}
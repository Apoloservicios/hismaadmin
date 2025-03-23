package com.example.hismaadmin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadmin.model.Lubricentro
import com.example.hismaadmin.model.Subscription
import com.example.hismaadmin.utils.SubscriptionManager
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LubricentrosListScreen(navController: NavController) {
    val db = FirebaseFirestore.getInstance()
    val subscriptionManager = remember { SubscriptionManager(db) }
    val scope = rememberCoroutineScope()

    var lubricentros by remember { mutableStateOf<List<Lubricentro>>(emptyList()) }
    var filteredLubricentros by remember { mutableStateOf<List<Lubricentro>>(emptyList()) }
    var subscriptionStatus by remember { mutableStateOf<Map<String, Subscription?>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Cargar datos iniciales
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val snapshot = db.collection("lubricentros")
                .get()
                .await()

            val lista = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Lubricentro::class.java)?.copy(uid = doc.id)
            }

            lubricentros = lista
            filteredLubricentros = lista

            // Cargar estado de suscripciones
            val statusMap = mutableMapOf<String, Subscription?>()

            lista.forEach { lub ->
                val subscription = subscriptionManager.getCurrentSubscription(lub.uid)
                statusMap[lub.uid] = subscription
            }

            subscriptionStatus = statusMap

        } catch (e: Exception) {
            // Manejar errores
        } finally {
            isLoading = false
        }
    }

    // Función para filtrar lubricentros
    fun filterLubricentros(query: String) {
        if (query.isBlank()) {
            filteredLubricentros = lubricentros
        } else {
            filteredLubricentros = lubricentros.filter { lub ->
                lub.nombreFantasia.contains(query, ignoreCase = true) ||
                        lub.email.contains(query, ignoreCase = true) ||
                        lub.cuit.contains(query, ignoreCase = true)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lubricentros") },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isLoading = true
                            try {
                                val snapshot = db.collection("lubricentros")
                                    .get()
                                    .await()

                                val lista = snapshot.documents.mapNotNull { doc ->
                                    doc.toObject(Lubricentro::class.java)?.copy(uid = doc.id)
                                }

                                lubricentros = lista
                                filterLubricentros(searchQuery)

                                // Recargar suscripciones
                                val statusMap = mutableMapOf<String, Subscription?>()

                                lista.forEach { lub ->
                                    val subscription = subscriptionManager.getCurrentSubscription(lub.uid)
                                    statusMap[lub.uid] = subscription
                                }

                                subscriptionStatus = statusMap

                            } catch (e: Exception) {
                                // Manejar errores
                            } finally {
                                isLoading = false
                            }
                        }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refrescar")
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
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    filterLubricentros(it)
                },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar por nombre, email o CUIT") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            filterLubricentros("")
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                },
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredLubricentros.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "No hay lubricentros registrados"
                        } else {
                            "No se encontraron resultados para: $searchQuery"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                // Lista de lubricentros
                LazyColumn {
                    items(filteredLubricentros) { lub ->
                        LubricentroItem(
                            lubricentro = lub,
                            subscription = subscriptionStatus[lub.uid],
                            onClick = {
                                // Navegar a detalles de lubricentro
                                navController.navigate("lubricentro_detail/${lub.uid}")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LubricentroItem(
    lubricentro: Lubricentro,
    subscription: Subscription?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick)
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
                    text = lubricentro.nombreFantasia,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Indicador de estado de suscripción
                if (subscription != null) {
                    val isValid = subscription.isValid()
                    val statusColor = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = if (isValid) "Activa" else "Inactiva",
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = if (isValid) "Activa" else "Inactiva",
                            color = statusColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                } else {
                    Text(
                        text = "Sin suscripción",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = lubricentro.email,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "CUIT: ${lubricentro.cuit}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (subscription != null) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cambios: ${subscription.cambiosRestantes}",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "Días: ${subscription.getDiasRestantes()}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
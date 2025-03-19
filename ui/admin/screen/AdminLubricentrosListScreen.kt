package com.example.hismaadm.ui.admin.screen


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadm.model.LubricentroExtended
import com.example.hismaadm.ui.admin.components.LubricentroItem
import com.example.hismaadm.ui.navigation.AdminScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminLubricentrosListScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var lubricentros by remember { mutableStateOf(listOf<LubricentroExtended>()) }
    var filteredLubricentros by remember { mutableStateOf(listOf<LubricentroExtended>()) }

    // Estados para el filtrado
    var searchQuery by remember { mutableStateOf("") }
    var showActiveOnly by remember { mutableStateOf(false) }
    var showWithSubscriptionOnly by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("nombre") } // nombre, fecha, plan

    // Cargar la lista de lubricentros
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                val snapshot = db.collection("lubricentros")
                    .orderBy("nombreFantasia", Query.Direction.ASCENDING)
                    .get()
                    .await()

                val list = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null

                    val lubricentro = LubricentroExtended(
                        id = doc.id,
                        uid = doc.id,
                        nombreFantasia = data["nombreFantasia"] as? String ?: "",
                        responsable = data["responsable"] as? String ?: "",
                        cuit = data["cuit"] as? String ?: "",
                        direccion = data["direccion"] as? String ?: "",
                        telefono = data["telefono"] as? String ?: "",
                        email = data["email"] as? String ?: "",
                        logoUrl = data["logoUrl"] as? String ?: "",
                        activo = data["activo"] as? Boolean ?: true,
                        verificado = data["verificado"] as? Boolean ?: false,
                        fechaRegistro = data["fechaRegistro"] as? com.google.firebase.Timestamp
                            ?: com.google.firebase.Timestamp.now()
                    )

                    // Buscar suscripción activa (si existe)
                    try {
                        val suscripcionQuery = db.collection("suscripciones")
                            .whereEqualTo("lubricentroId", doc.id)
                            .whereEqualTo("activa", true)
                            .get()
                            .await()

                        if (!suscripcionQuery.isEmpty) {
                            val suscripcionDoc = suscripcionQuery.documents[0]
                            val suscripcionData = suscripcionDoc.data ?: mapOf<String, Any>()

                            lubricentro.copy(
                                planActual = suscripcionData["planNombre"] as? String ?: "",
                                suscripcionActiva = true,
                                suscripcionId = suscripcionDoc.id,
                                suscripcionFechaFin = suscripcionData["fechaFin"] as? com.google.firebase.Timestamp
                            )
                        } else {
                            lubricentro
                        }
                    } catch (e: Exception) {
                        lubricentro
                    }
                }

                lubricentros = list
                filteredLubricentros = list
            } catch (e: Exception) {
                // Manejar error
            } finally {
                isLoading = false
            }
        } else {
            navController.navigate(AdminScreen.Login.route) {
                popUpTo(AdminScreen.LubricentrosList.route) { inclusive = true }
            }
        }
    }

    // Función para aplicar filtros
    fun applyFilters() {
        var result = lubricentros

        // Filtro por nombre, responsable, email o CUIT
        if (searchQuery.isNotBlank()) {
            val query = searchQuery.lowercase()
            result = result.filter {
                it.nombreFantasia.lowercase().contains(query) ||
                        it.responsable.lowercase().contains(query) ||
                        it.email.lowercase().contains(query) ||
                        it.cuit.contains(query)
            }
        }

        // Filtro por activo
        if (showActiveOnly) {
            result = result.filter { it.activo }
        }

        // Filtro por suscripción activa
        if (showWithSubscriptionOnly) {
            result = result.filter { it.suscripcionActiva }
        }

        // Ordenamiento
        result = when (sortBy) {
            "nombre" -> result.sortedBy { it.nombreFantasia }
            "fecha" -> result.sortedByDescending { it.fechaRegistro }
            "plan" -> result.sortedWith(
                compareByDescending<LubricentroExtended> { it.suscripcionActiva }
                    .thenBy { it.planActual }
            )
            else -> result
        }

        filteredLubricentros = result
    }

    // Aplicar filtros cuando cambien
    LaunchedEffect(searchQuery, showActiveOnly, showWithSubscriptionOnly, sortBy) {
        applyFilters()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lubricentros") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Barra de búsqueda
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por nombre, responsable, email o CUIT") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Filtros
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Filtro "Solo activos"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = showActiveOnly,
                        onCheckedChange = { showActiveOnly = it }
                    )
                    Text("Solo activos")
                }

                // Filtro "Con suscripción"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Checkbox(
                        checked = showWithSubscriptionOnly,
                        onCheckedChange = { showWithSubscriptionOnly = it }
                    )
                    Text("Con suscripción")
                }

                // Dropdown para ordenar
                Box(modifier = Modifier.weight(1f)) {
                    var expanded by remember { mutableStateOf(false) }

                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = when (sortBy) {
                                "nombre" -> "Por nombre"
                                "fecha" -> "Por fecha"
                                "plan" -> "Por plan"
                                else -> "Ordenar por"
                            },
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            colors = ExposedDropdownMenuDefaults.textFieldColors()
                        )

                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Por nombre") },
                                onClick = {
                                    sortBy = "nombre"
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Por fecha") },
                                onClick = {
                                    sortBy = "fecha"
                                    expanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Por plan") },
                                onClick = {
                                    sortBy = "plan"
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Contador de resultados
            Text(
                text = "${filteredLubricentros.size} lubricentros encontrados",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredLubricentros.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No se encontraron lubricentros con los filtros aplicados")
                }
            } else {
                // Lista de lubricentros
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredLubricentros) { lubricentro ->
                        // En el componente LubricentroItem
                        LubricentroItem(
                            lubricentro = lubricentro,
                            onItemClick = {
                                Log.d("AdminList", "Navegando a lubricentro: ${lubricentro.id}")
                                navController.navigate(AdminScreen.LubricentroDetail.createRoute(lubricentro.id))
                            }
                        )
                    }
                }
            }
        }
    }
}
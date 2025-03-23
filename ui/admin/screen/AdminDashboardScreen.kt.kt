package com.example.hismaadm.ui.admin.screen

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadm.ui.admin.components.StatCard
import com.example.hismaadm.ui.admin.components.LubricentroItem
import com.example.hismaadm.ui.navigation.AdminScreen
import com.example.hismaadm.model.LubricentroExtended
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var adminName by remember { mutableStateOf("Administrador") }

    // Estadísticas
    var totalLubricentros by remember { mutableStateOf(0) }
    var lubricentrosActivos by remember { mutableStateOf(0) }
    var totalSuscripcionesActivas by remember { mutableStateOf(0) }

    // Lista de lubricentros recientes
    var recentLubricentros by remember { mutableStateOf(listOf<LubricentroExtended>()) }

    // Drawer state
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope2 = rememberCoroutineScope()

    // Cargar datos
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            try {
                // Cargar información del admin
                val adminDoc = db.collection("admins")
                    .document(currentUser.uid)
                    .get()
                    .await()

                if (adminDoc.exists()) {
                    adminName = adminDoc.getString("nombre") ?: "Administrador"
                }

                // Cargar estadísticas
                val lubricentrosSnapshot = db.collection("lubricentros")
                    .get()
                    .await()

                totalLubricentros = lubricentrosSnapshot.size()

                val activosSnapshot = db.collection("lubricentros")
                    .whereEqualTo("activo", true)
                    .get()
                    .await()

                lubricentrosActivos = activosSnapshot.size()

                val suscripcionesSnapshot = db.collection("suscripciones")
                    .whereEqualTo("activa", true)
                    .get()
                    .await()

                totalSuscripcionesActivas = suscripcionesSnapshot.size()

                // Cargar lubricentros recientes
                val recentSnapshot = db.collection("lubricentros")
                    .orderBy("fechaRegistro", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()

                recentLubricentros = recentSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null

                    LubricentroExtended(
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
                            ?: com.google.firebase.Timestamp.now(),
                        // Otros campos podrían no estar presentes aún en la base de datos
                    )
                }

            } catch (e: Exception) {
                // Manejar error
            } finally {
                isLoading = false
            }
        } else {
            // Usuario no autenticado, redirigir a login
            navController.navigate(AdminScreen.Login.route) {
                popUpTo(AdminScreen.Dashboard.route) { inclusive = true }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "HISMA Admin",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = adminName,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Divider(modifier = Modifier.padding(vertical = 16.dp))

                // Menú de navegación
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = true,
                    onClick = {
                        scope2.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) }
                )

                NavigationDrawerItem(
                    label = { Text("Lubricentros") },
                    selected = false,
                    onClick = {
                        scope2.launch {
                            drawerState.close()
                            navController.navigate(AdminScreen.LubricentrosList.route)
                        }
                    },
                    icon = { Icon(Icons.Default.Business, contentDescription = null) }
                )

                NavigationDrawerItem(
                    label = { Text("Planes de Suscripción") },
                    selected = false,
                    onClick = {
                        scope2.launch {
                            drawerState.close()
                            navController.navigate(AdminScreen.SubscriptionPlans.route)
                        }
                    },
                    icon = { Icon(Icons.Default.CardMembership, contentDescription = null) }
                )

                Spacer(modifier = Modifier.weight(1f))

                NavigationDrawerItem(
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        auth.signOut()
                        navController.navigate(AdminScreen.Login.route) {
                            popUpTo(AdminScreen.Dashboard.route) { inclusive = true }
                        }
                    },
                    icon = { Icon(Icons.Default.ExitToApp, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Panel de Administración") },
                    navigationIcon = {
                        IconButton(onClick = { scope2.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            auth.signOut()
                            navController.navigate(AdminScreen.Login.route) {
                                popUpTo(AdminScreen.Dashboard.route) { inclusive = true }
                            }
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar Sesión")
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
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Tarjetas de estadísticas
                    item {
                        Text(
                            text = "Estadísticas Generales",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                title = "Total Lubricentros",
                                value = totalLubricentros.toString(),
                                icon = Icons.Default.Business,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                title = "Lubricentros Activos",
                                value = lubricentrosActivos.toString(),
                                icon = Icons.Default.CheckCircle,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            StatCard(
                                title = "Suscripciones Activas",
                                value = totalSuscripcionesActivas.toString(),
                                icon = Icons.Default.CardMembership,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )

                            StatCard(
                                title = "% Conversión",
                                value = if (totalLubricentros > 0)
                                    "${(totalSuscripcionesActivas * 100 / totalLubricentros)}%"
                                else "0%",
                                icon = Icons.Default.TrendingUp,
                                color = Color(0xFFE91E63),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Acciones rápidas
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Acciones Rápidas",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { navController.navigate(AdminScreen.LubricentrosList.route) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.List, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ver Lubricentros")
                            }

                            Button(
                                onClick = { navController.navigate(AdminScreen.SubscriptionPlans.route) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gestionar Planes")
                            }
                        }

// Botón para gestionar solicitudes como un elemento separado
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { navController.navigate(AdminScreen.SubscriptionRequests.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gestionar Solicitudes de Suscripción")
                        }
                    }

                    // Lubricentros recientes
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lubricentros Recientes",
                                style = MaterialTheme.typography.titleLarge
                            )

                            TextButton(
                                onClick = { navController.navigate(AdminScreen.LubricentrosList.route) }
                            ) {
                                Text("Ver Todos")
                                Icon(Icons.Default.ArrowForward, contentDescription = null)
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (recentLubricentros.isEmpty()) {
                        item {
                            Text(
                                text = "No hay lubricentros registrados aún.",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                    } else {
                        items(recentLubricentros) { lubricentro ->
                            LubricentroItem(
                                lubricentro = lubricentro,
                                onItemClick = {
                                    navController.navigate("${AdminScreen.LubricentroDetail.route}/${lubricentro.id}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
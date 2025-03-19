package com.example.hismaadm.ui.admin.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.hismaadm.model.SubscriptionPlan
import com.example.hismaadm.ui.admin.components.SubscriptionPlanCard
import com.example.hismaadm.ui.admin.dialogs.AddEditSubscriptionPlanDialog2
import com.example.hismaadm.ui.navigation.AdminScreen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubscriptionPlansScreen(navController: NavController) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var plans by remember { mutableStateOf<List<SubscriptionPlan>>(emptyList()) }

    // Estados para diálogos
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var selectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Función para cargar planes
    fun loadPlans() {
        scope.launch {
            isLoading = true
            try {
                val snapshot = db.collection("planes")
                    .get()
                    .await()

                val plansList = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null

                    SubscriptionPlan(
                        id = doc.id,
                        nombre = data["nombre"] as? String ?: "",
                        descripcion = data["descripcion"] as? String ?: "",
                        precio = (data["precio"] as? Number)?.toDouble() ?: 0.0,
                        duracionMeses = (data["duracionMeses"] as? Number)?.toInt() ?: 1,
                        limiteEmpleados = (data["limiteEmpleados"] as? Number)?.toInt() ?: 5,
                        limiteCambiosAceite = (data["limiteCambiosAceite"] as? Number)?.toInt() ?: 100,
                        activo = data["activo"] as? Boolean ?: true
                    )
                }

                plans = plansList.sortedBy { it.precio }
            } catch (e: Exception) {
                // Manejar error
            } finally {
                isLoading = false
            }
        }
    }

    // Cargar planes al iniciar
    LaunchedEffect(Unit) {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            loadPlans()
        } else {
            navController.navigate(AdminScreen.Login.route) {
                popUpTo(AdminScreen.SubscriptionPlans.route) { inclusive = true }
            }
        }
    }

    // Función para guardar un plan
    fun savePlan(plan: SubscriptionPlan, isNew: Boolean) {
        scope.launch {
            try {
                val planData = hashMapOf(
                    "nombre" to plan.nombre,
                    "descripcion" to plan.descripcion,
                    "precio" to plan.precio,
                    "duracionMeses" to plan.duracionMeses,
                    "limiteEmpleados" to plan.limiteEmpleados,
                    "limiteCambiosAceite" to plan.limiteCambiosAceite,
                    "activo" to plan.activo,
                    "updatedAt" to com.google.firebase.Timestamp.now()
                )

                if (isNew) {
                    planData["createdAt"] = com.google.firebase.Timestamp.now()
                    db.collection("planes")
                        .add(planData)
                        .await()
                } else {
                    db.collection("planes")
                        .document(plan.id)
                        .update(planData.toMap())
                        .await()
                }

                // Recargar planes
                loadPlans()

            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    // Función para eliminar un plan
    fun deletePlan(planId: String) {
        if (planId.isBlank()) return

        scope.launch {
            try {
                // Verificar si el plan está en uso
                val subscriptionsQuery = db.collection("suscripciones")
                    .whereEqualTo("planId", planId)
                    .whereEqualTo("activa", true)
                    .get()
                    .await()

                if (!subscriptionsQuery.isEmpty) {
                    // El plan está en uso, desactivarlo en lugar de eliminarlo
                    db.collection("planes")
                        .document(planId)
                        .update("activo", false)
                        .await()
                } else {
                    // El plan no está en uso, eliminarlo
                    db.collection("planes")
                        .document(planId)
                        .delete()
                        .await()
                }

                // Recargar planes
                loadPlans()

            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Planes de Suscripción") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar Plan")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar Plan")
            }
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
        } else if (plans.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CardMembership,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "No hay planes de suscripción",
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Crea tu primer plan para ofrecer a los lubricentros",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { showAddDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Plan")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(plans) { plan ->
                    SubscriptionPlanCard(
                        plan = plan,
                        onEditClick = {
                            selectedPlan = plan
                            showEditDialog = true
                        },
                        onDeleteClick = {
                            selectedPlan = plan
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Diálogo para agregar plan
    if (showAddDialog) {
        AddEditSubscriptionPlanDialog2(
            plan = null,
            onDismiss = { showAddDialog = false },
            onSave = { newPlan ->
                savePlan(newPlan, true)
                showAddDialog = false
            }
        )
    }

    // Diálogo para editar plan
    if (showEditDialog && selectedPlan != null) {
        AddEditSubscriptionPlanDialog2(
            plan = selectedPlan,
            onDismiss = { showEditDialog = false },
            onSave = { updatedPlan ->
                savePlan(updatedPlan, false)
                showEditDialog = false
            }
        )
    }

    // Diálogo para confirmar eliminación
    if (showDeleteDialog && selectedPlan != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar Plan") },
            text = {
                Text("¿Está seguro que desea eliminar el plan ${selectedPlan?.nombre}? Si hay lubricentros utilizando este plan, se desactivará en lugar de eliminarse.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        deletePlan(selectedPlan?.id ?: "")
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
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
import com.example.hismaadm.model.LubricentroExtended

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditLubricentroDialog(
    lubricentro: LubricentroExtended,
    onDismiss: () -> Unit,
    onSave: (LubricentroExtended) -> Unit
) {
    var nombreFantasia by remember { mutableStateOf(lubricentro.nombreFantasia) }
    var responsable by remember { mutableStateOf(lubricentro.responsable) }
    var cuit by remember { mutableStateOf(lubricentro.cuit) }
    var direccion by remember { mutableStateOf(lubricentro.direccion) }
    var telefono by remember { mutableStateOf(lubricentro.telefono) }
    var email by remember { mutableStateOf(lubricentro.email) }
    var activo by remember { mutableStateOf(lubricentro.activo) }
    var verificado by remember { mutableStateOf(lubricentro.verificado) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar Lubricentro") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = nombreFantasia,
                    onValueChange = { nombreFantasia = it },
                    label = { Text("Nombre Fantasía") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = responsable,
                    onValueChange = { responsable = it },
                    label = { Text("Responsable") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = cuit,
                    onValueChange = { cuit = it },
                    label = { Text("CUIT") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = direccion,
                    onValueChange = { direccion = it },
                    label = { Text("Dirección") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text("Teléfono") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Estado", modifier = Modifier.weight(1f))
                    Switch(
                        checked = activo,
                        onCheckedChange = { activo = it }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Verificado", modifier = Modifier.weight(1f))
                    Switch(
                        checked = verificado,
                        onCheckedChange = { verificado = it }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedLubricentro = lubricentro.copy(
                        nombreFantasia = nombreFantasia,
                        responsable = responsable,
                        cuit = cuit,
                        direccion = direccion,
                        telefono = telefono,
                        email = email,
                        activo = activo,
                        verificado = verificado
                    )
                    onSave(updatedLubricentro)
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
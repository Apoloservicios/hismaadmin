package com.example.hismaadm.ui.admin.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.hismaadm.model.LubricentroExtended
import java.text.SimpleDateFormat
import java.util.*



@Composable
fun LubricentroItem(
    lubricentro: LubricentroExtended,
    onItemClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onItemClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Estado (punto de color)
            Surface(
                modifier = Modifier.size(12.dp),
                shape = MaterialTheme.shapes.small,
                color = if (lubricentro.activo) Color.Green else Color.Red
            ) {}

            Spacer(modifier = Modifier.width(16.dp))

            // Información principal
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = lubricentro.nombreFantasia,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = lubricentro.responsable,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = lubricentro.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Estado de suscripción simplificado
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = if (lubricentro.suscripcionActiva)
                        "Plan: ${lubricentro.planActual}"
                    else
                        "Sin plan activo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (lubricentro.suscripcionActiva)
                        Color(0xFF4CAF50)
                    else
                        Color(0xFFFF9800)
                )

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                val fecha = sdf.format(lubricentro.fechaRegistro.toDate())
                Text(
                    text = "Registro: $fecha",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Icono de navegación
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Ver detalles"
            )
        }
    }
}
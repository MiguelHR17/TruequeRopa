package dev.miguelehr.truequeropa.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Modelo simple (mock)
private data class NotificationItem(
    val id: String,
    val title: String,
    val body: String,
    val unread: Boolean = true
)

/**
 * Pantalla de Notificaciones (mock).
 * onSent es opcional por si luego quieres lanzar alguna acción rápida desde aquí.
 */
@Composable
fun NotificationScreen(
    padding: PaddingValues,
    onSent: (() -> Unit)? = null
) {
    val notifications = remember {
        listOf(
            NotificationItem("n1", "¡Nuevo trueque!", "Andrea te propuso intercambiar su polo por tu jean."),
            NotificationItem("n2", "Propuesta aceptada", "Marco aceptó tu propuesta."),
            NotificationItem("n3", "Recordatorio", "Tienes 2 propuestas pendientes por revisar.", unread = false),
        )
    }

    Column(
        Modifier
            .padding(padding)
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("Notificaciones", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))

        if (notifications.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tienes notificaciones")
            }
            return
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(notifications, key = { it.id }) { n ->
                ElevatedCard(
                    colors = if (n.unread)
                        CardDefaults.elevatedCardColors()
                    else
                        CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(n.title, style = MaterialTheme.typography.titleMedium)
                            Text(n.body, style = MaterialTheme.typography.bodyMedium)
                        }
                        // Botón mock
                        TextButton(onClick = { onSent?.invoke() }) {
                            Text("OK")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
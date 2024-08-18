package com.example.controlderefaccionesvestible.presentation

import android.content.Context
import android.util.Log
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pusher.client.Pusher
import com.pusher.client.PusherOptions
import com.pusher.client.channel.ChannelEventListener
import com.pusher.client.connection.ConnectionEventListener
import com.pusher.client.connection.ConnectionState
import com.pusher.client.connection.ConnectionStateChange
import com.example.controlderefaccionesvestible.R
import com.example.controlderefaccionesvestible.presentation.theme.ControlDeRefaccionesVestibleTheme
import org.json.JSONObject
import org.json.JSONArray
import android.app.NotificationManager
import android.app.NotificationChannel
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.util.TimeZone

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Mexico_City"))
        super.onCreate(savedInstanceState)

        // Crea el canal de notificaciones
        createNotificationChannel()

        setContent {
            WearApp(greetingName = "WearOS")
        }

        // Configuración de Pusher
        val options = PusherOptions().apply {
            setCluster("mt1") // Configura tu clúster aquí
        }

        val pusher = Pusher("68ed89d2578cc3e7b121", options) // Reemplaza "your-app-key" con tu clave de aplicación de Pusher

        Log.i("Pusher", "Intentando conectar a Pusher...")

        // Conectar a Pusher
        pusher.connect(object : ConnectionEventListener {
            override fun onConnectionStateChange(change: ConnectionStateChange) {
                Log.i("Pusher", "Estado de conexión cambiado de ${change.previousState} a ${change.currentState}")

                if (change.currentState == ConnectionState.CONNECTED) {
                    Log.i("Pusher", "Conexión establecida con éxito")
                    subscribeToChannel(pusher)
                } else if (change.currentState == ConnectionState.DISCONNECTED) {
                    Log.e("Pusher", "Conexión fallida o desconectada")
                }
            }

            override fun onError(message: String, code: String?, e: Exception?) {
                if (code != null) {
                    Log.e("Pusher", "Error al conectar! código: $code, mensaje: $message, excepción: ${e?.message ?: "No hay excepción"}")
                } else {
                    Log.e("Pusher", "Error al conectar! código desconocido, mensaje: $message, excepción: ${e?.message ?: "No hay excepción"}")
                }
            }

        }, ConnectionState.ALL)
    }

    private fun subscribeToChannel(pusher: Pusher) {
        // Suscribirse al canal y manejar el evento report-notifications
        val channel = pusher.subscribe("controldealmacen")

        // Verificar que la suscripción fue exitosa
        channel.bind("new-report-created", object : ChannelEventListener {
            override fun onSubscriptionSucceeded(channelName: String) {
                Log.i("Pusher", "Suscripción al canal $channelName exitosa")
            }

            override fun onEvent(event: com.pusher.client.channel.PusherEvent) {
                try {
                    // Aquí tratamos el evento como un objeto JSON
                    val jsonData = JSONObject(event.data)
                    val arrRefactions = jsonData.getJSONArray("data")
                    val message = buildNotificationMessage(arrRefactions)
                    Log.i("Pusher", "Mensaje construido para la notificación: $message")
                    showNotification(this@MainActivity, "Refacción tomada", message)
                } catch (e: Exception) {
                    Log.e("Pusher", "Error al procesar el evento: ${e.message}")
                }
            }
        })

        Log.i("Pusher", "Intentando suscribirse al canal controldealmacen...")
    }

    private fun createNotificationChannel() {
        val name = "Pusher Notifications"
        val descriptionText = "Canal para recibir notificaciones de Pusher"
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("pusher-channel", name, importance).apply {
            description = descriptionText
        }
        // Registrar el canal con el sistema
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.createNotificationChannel(channel)
        Log.i("Pusher", "Canal de notificaciones creado")
    }

    private fun buildNotificationMessage(arrRefactions: JSONArray): String {
        val messageBuilder = StringBuilder()
        for (i in 0 until arrRefactions.length()) {
            val refactionData = arrRefactions.getJSONObject(i)
            val userName = refactionData.getString("user")
            val refaction = refactionData.getString("refaction")
            val quantity = refactionData.getInt("quantity")
            val date = refactionData.getString("date")

            // Convertir la fecha a la zona horaria de Guadalajara
            val zonedDateTime = ZonedDateTime.parse(date).withZoneSameInstant(ZoneId.of("America/Mexico_City"))
            val formattedDate = zonedDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))

            messageBuilder.append("Usuario: $userName\n")
                .append("Refacción: $refaction\n")
                .append("Cantidad: $quantity\n")
                .append("Fecha: $formattedDate\n\n")
        }
        return messageBuilder.toString().trim()
    }

    private fun showNotification(context: Context, title: String, message: String) {
        Log.i("Pusher", "Preparando para mostrar notificación: $title - $message")
        sendNotification(context, title, message)
    }

    private fun sendNotification(context: Context, title: String, message: String) {
        Log.i("Pusher", "Enviando notificación: $title - $message")
        val builder = NotificationCompat.Builder(context, "pusher-channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Para mostrar mensajes largos
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(2, builder.build())
        Log.i("Pusher", "Notificación enviada")
    }
}


@Composable
fun WearApp(greetingName: String) {
    ControlDeRefaccionesVestibleTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Greeting(greetingName = greetingName)
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        text = stringResource(R.string.hello_world, greetingName)
    )
}

@Preview(device = Devices.WEAR_OS_RECT, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Preview Android")
}

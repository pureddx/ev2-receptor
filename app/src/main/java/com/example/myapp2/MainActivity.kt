package com.example.myapp2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface
import java.net.ServerSocket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // MaterialTheme puro usa automáticamente el morado predeterminado de la plantilla
            MaterialTheme {
                PantallaReceptorIoT()
            }
        }
    }
}

@Composable
fun PantallaReceptorIoT() {
    var alarmaSonando by remember { mutableStateOf(false) }
    var ipLocal by remember { mutableStateOf("Buscando IP...") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val ipDetectada = obtenerIpSegura()
            withContext(Dispatchers.Main) {
                ipLocal = ipDetectada
            }

            try {
                val serverSocket = ServerSocket(8080)
                while (true) {
                    val socket = serverSocket.accept()
                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val mensajeRecibido = reader.readLine()

                    withContext(Dispatchers.Main) {
                        if (mensajeRecibido == "ENCENDER") {
                            alarmaSonando = true
                        } else if (mensajeRecibido == "APAGAR") {
                            alarmaSonando = false
                        }
                    }
                    socket.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (alarmaSonando) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Módulo Alarma IoT (Receptor)",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "IP Local (Ingresar en el Emisor):", fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = ipLocal,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Puerto de escucha: 8080", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (alarmaSonando) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (alarmaSonando) "¡ALARMA ACTIVADA!" else "SISTEMA SEGURO",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Button(
            onClick = { alarmaSonando = false },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (alarmaSonando) Color.Gray else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("Apagar Alarma Manualmente", fontWeight = FontWeight.Bold)
        }
    }
}

fun obtenerIpSegura(): String {
    try {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        var ipFallback = ""

        while (interfaces.hasMoreElements()) {
            val networkInterface = interfaces.nextElement()
            val name = networkInterface.name.lowercase()
            val addresses = networkInterface.inetAddresses

            while (addresses.hasMoreElements()) {
                val inetAddress = addresses.nextElement()
                if (!inetAddress.isLoopbackAddress && inetAddress is Inet4Address) {
                    val ip = inetAddress.hostAddress ?: continue
                    if (!ip.startsWith("127.")) {
                        if (name.contains("wlan") || name.contains("ap") || name.contains("swlan") || name.contains("eth")) {
                            return ip
                        }
                        ipFallback = ip
                    }
                }
            }
        }
        if (ipFallback.isNotBlank()) return ipFallback
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return "127.0.0.1 (Sin Wi-Fi)"
}

@Preview(showBackground = true, name = "Vista Previa Receptor")
@Composable
fun PreviewReceptor() {
    MaterialTheme {
        PantallaReceptorIoT()
    }
}
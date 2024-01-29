package com.example.iot2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MainActivity : ComponentActivity() {

    // MQTT-brokerin tiedot
    private val broker = "tcp://test.mosquitto.org:1883"
    private val clientId = MqttClient.generateClientId()
    private val topicdistance = "mittaus_ovi_ultrasonic_sensoriilla"
    private val topicmotion = "mittaus_liikesensoriilla_kulku"
    private var motionflag = false
    private var distanceflag = true

    private lateinit var mqttClient: MqttClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val composeView = findViewById<ComposeView>(R.id.composeView)
        composeView.setContent {
            var topic1Value by remember { mutableStateOf("") }
            var topic2Value by remember { mutableStateOf("") }

            // Yhdistetään MQTT:hen ja tilataan aiheet täällä
            try {
                val persistence = MemoryPersistence()
                mqttClient = MqttClient(broker, clientId, persistence)
                val options = MqttConnectOptions()
                options.isCleanSession = true

                // Yritetään yhdistää MQTT-brokeriin
                mqttClient.connect(options)

                // Tarkistetaan MQTT-yhteyden tila ja kirjataan tila lokitiedostoon
                if (mqttClient.isConnected) {
                    Log.e("MQTT", "Yhdistetty MQTT-brokeriin onnistuneesti")
                } else {
                    Log.e("MQTT", "Yhdistäminen MQTT-brokeriin epäonnistui")
                }

                mqttClient.subscribe(topicdistance) { _, message ->
                    val payload = String(message.payload)
                    Log.e("MQTT", "Vastaanotettu viesti aiheesta 1: $payload")
                    topic1Value = payload
                    val value = payload.toIntOrNull() ?: 0
                    if (value > 5) {
                        if(distanceflag==false) {
                            showNotification("Etäisyysarvo on yli 5")
                            distanceflag=true
                        }
                    }else{
                        distanceflag=false
                    }
                }

                mqttClient.subscribe(topicmotion) { _, message ->
                    val payload = String(message.payload)
                    Log.e("MQTT", "Vastaanotettu viesti aiheesta 2: $payload")
                    if (payload.toIntOrNull() == 1) {
                        topic2Value = "havaittu"
                        if(motionflag==false) {
                            showNotification("Liike havaittu")
                            motionflag=true
                        }
                    }else{
                        motionflag=false
                        topic2Value = "ei havaittu"
                    }
                }

            } catch (e: MqttException) {
                Log.e("MQTT", "MQTT-poikkeus: ${e.message}")
                e.printStackTrace()
            }

            // Luo käyttöliittymä
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Text(text = "Etäisyys: $topic1Value cm",
                    style = TextStyle(fontSize = 32.sp)
                )

                Text(text = "Liike: $topic2Value",
                    style = TextStyle(fontSize = 32.sp)
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mqttClient.disconnect()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    // Luo ilmoitus
    private fun showNotification(message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                MyApplication.CHANNEL_ID,
                "Oma kanava",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val builder = NotificationCompat.Builder(this, MyApplication.CHANNEL_ID)
            .setContentTitle("MQTT-ilmoitus")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)

        notificationManager.notify(1, builder.build())
    }
}

package com.allenarcher

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.Base64
import java.util.Date

val json = Json { ignoreUnknownKeys = true }
val httpClient: HttpClient = HttpClient.newHttpClient()
lateinit var config: Config
lateinit var logLevel: LogLevel
lateinit var snapshotOptionsQueryString: String
var ntfyAuth: String? = null
val tagsMap = mutableMapOf<String, String>()
val disabledCameras = mutableMapOf<String, Set<String>>()
val disabledObjects = mutableSetOf<String>()
var supervisorToken: String? = null

@Volatile
var lastNotificationDate: Date? = null

fun main() {
    // Setup configs
    config = json.decodeFromString(File("./data/options.json").readText())
    logLevel = LogLevel.valueOf(config.logLevel.uppercase())
    snapshotOptionsQueryString = config.snapshotOptions?.toQueryString() ?: ""
    if (!config.ntfyUser.isNullOrEmpty() && !config.ntfyPassword.isNullOrEmpty()) {
        val encoded = Base64.getEncoder().encodeToString("${config.ntfyUser}:${config.ntfyPassword}".toByteArray())
        ntfyAuth = "Basic $encoded"
    } else if (!config.ntfyToken.isNullOrEmpty()) {
        ntfyAuth = "Bearer ${config.ntfyToken}"
    }
    config.ntfyTags?.forEach {
        tag -> tagsMap[tag.objectName] = tag.tags
    }
    supervisorToken = System.getenv("TOKEN")
    config.disabledCameras?.forEach { camera ->
        val objects = camera.disabledObjects?.map { it.lowercase() }?.toSet() ?: emptySet()
        disabledCameras[camera.cameraName.lowercase()] = objects
    }
    config.disabledObjects?.forEach {
        disabledObjects.add(it.lowercase())
    }
    val brokerUrl = "${config.mqttAddress}:${config.mqttPort ?: 1883}"
    // Setup the MQTT client
    val mqttOptions = MqttConnectOptions().apply {
        isAutomaticReconnect = true
        isCleanSession = true
        if (!config.mqttUsername.isNullOrEmpty()) {
            userName = config.mqttUsername
        }
        if (!config.mqttPassword.isNullOrEmpty()) {
            password = config.mqttPassword?.toCharArray()
        }
    }
    try {
        val client = MqttClient(brokerUrl, MqttClient.generateClientId(), MemoryPersistence())
        client.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                client.subscribe(config.mqttTopic)
                if (reconnect) logInfo("Reconnected to MQTT and re-subscribed to '${config.mqttTopic}'")
            }
            override fun connectionLost(cause: Throwable?) {
                logError("MQTT connection lost: ${cause?.message}")
            }
            override fun messageArrived(topic: String, message: MqttMessage) {
                handleMessage(message.toString())
            }
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })
        client.connect(mqttOptions)
        logInfo("Connected to MQTT at '${config.mqttAddress}'")
        // This blocks the main thread because MQTT runs on its own thread
        Thread.currentThread().join()
    } catch (e: Exception) {
        logError("Error connecting to MQTT broker at ${config.mqttAddress}: $e")
    }
}

fun handleMessage(payload: String) {
    val event = json.decodeFromString<FrigateEvent>(payload)
    val camera = event.after.camera
    val label = event.after.label
    val id = event.after.id
    logDebug("Message received: camera=$camera, label=$label, id=$id, before.hasSnapshot=${event.before.hasSnapshot}, after.hasSnapshot=${event.after.hasSnapshot}")
    if (!event.before.hasSnapshot && event.after.hasSnapshot) {
        val cameraLower = camera.lowercase()
        val labelLower = label.lowercase()
        var doSend = true
        if (disabledCameras.containsKey(cameraLower)) {
            val objects = disabledCameras[cameraLower]!!
            if (objects.isEmpty() || objects.contains(labelLower)) doSend = false
        }
        if (disabledObjects.contains(labelLower)) {
            doSend = false
        }
        if (doSend) {
            sendNotification(camera, label, id)
        }
    }
}

fun sendNotification(camera: String, label: String, id: String) {
    logDebug("Sending notification: camera=$camera, label=$label, id=$id")
    if (config.ntfyEnabled) {
        sendNtfyNotification(camera, label, id)
    }
    if (config.haEnabled) {
        config.haEntityIds?.forEach { sendHaNotification(camera, label, id, it) }
    }
}

fun sendNtfyNotification(camera: String, label: String, id: String) {
    var priority = config.ntfyNormalPriority
    if (config.groupingEnabled) {
        val now = Date()
        val last = lastNotificationDate
        if (last != null) {
            val diffMinutes = (now.time - last.time) / 60000
            if (diffMinutes >= (config.groupingMinutes ?: 5)) {
                lastNotificationDate = now
            } else {
                priority = config.ntfyLowerPriority ?: priority
            }
        } else {
            lastNotificationDate = now
        }
    }
    val requestBuilder = HttpRequest.newBuilder()
        .uri(URI.create("${config.ntfyUrl}/${config.ntfyTopic}"))
        .POST(HttpRequest.BodyPublishers.ofString(camera.capitalizeFirst()))
        .header("Title", label.capitalizeFirst())
        .header("Attach", "${config.frigateUrl}/api/events/$id/snapshot.jpg$snapshotOptionsQueryString")
        .header("Click", "${config.frigateUrl}/api/events/$id/clip.mp4")
        .header("Priority", priority.toString())
    tagsMap[label]?.let { requestBuilder.header("Tags", it) }
    ntfyAuth?.let { requestBuilder.header("Authorization", it) }
    try {
        val response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.discarding())
        if (response.statusCode() != 200) {
            logError("Non-successful response from ntfy request: ${response.statusCode()}")
        }
    } catch (e: Exception) {
        logError("Error sending request to ntfy at ${config.ntfyUrl}: $e")
    }
}

fun sendHaNotification(camera: String, label: String, id: String, entityId: String) {
    val bodyJson = json.encodeToString(HaNotification(
        title = label.capitalizeFirst(),
        message = camera.capitalizeFirst(),
        data = HaNotificationData(
            image = "${config.frigateUrl}/api/events/$id/snapshot.jpg$snapshotOptionsQueryString",
            clickAction = "${config.frigateUrl}/api/events/$id/clip.mp4"
        )
    ))
    val request = HttpRequest.newBuilder()
        .uri(URI.create("http://supervisor/core/api/services/notify/$entityId"))
        .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
        .header("Authorization", "Bearer $supervisorToken")
        .header("Content-Type", "application/json")
        .build()
    try {
        val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
        if (response.statusCode() != 200) {
            logError("Non-successful response from Home Assistant API request: ${response.statusCode()}")
        }
    } catch (e: Exception) {
        logError("Error sending request to Home Assistant: $e")
    }
}

fun String.capitalizeFirst(): String = this.replaceFirstChar { it.uppercase() }
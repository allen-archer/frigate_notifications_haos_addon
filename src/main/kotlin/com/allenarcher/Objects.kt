package com.allenarcher

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FrigateEventSnapshot(
    @SerialName("frame_time") val frameTime: Double,
    val box: List<Int>,
    val area: Int,
    val region: List<Int>,
    val score: Double
)

@Serializable
data class FrigateDetection(
    val id: String,
    val camera: String,
    @SerialName("frame_time") val frameTime: Double,
    val snapshot: FrigateEventSnapshot? = null,
    val label: String,
    @SerialName("sub_label") val subLabel: String? = null,
    @SerialName("top_score") val topScore: Double,
    @SerialName("false_positive") val falsePositive: Boolean,
    @SerialName("start_time") val startTime: Double,
    @SerialName("end_time") val endTime: Double? = null,
    val score: Double,
    val box: List<Int>,
    val area: Int,
    val ratio: Double,
    val region: List<Int>,
    val stationary: Boolean,
    @SerialName("motionless_count") val motionlessCount: Int,
    @SerialName("position_changes") val positionChanges: Int,
    @SerialName("current_zones") val currentZones: List<String>,
    @SerialName("entered_zones") val enteredZones: List<String>,
    @SerialName("has_clip") val hasClip: Boolean,
    @SerialName("has_snapshot") val hasSnapshot: Boolean
)

@Serializable
data class FrigateEvent(
    val before: FrigateDetection,
    val after: FrigateDetection,
    val type: String
)

@Serializable
data class NtfyTag(@SerialName("object") val objectName: String, val tags: String)

@Serializable
data class DisabledCamera(@SerialName("camera_name") val cameraName: String, @SerialName("disabled_objects") val disabledObjects: List<String>? = null)

@Serializable
data class SnapshotOptions(
    val bbox: Int? = null,
    val timestamp: Int? = null,
    val crop: Int? = null,
    val h: Int? = null,
    val quality: Int? = null
) {
    fun toQueryString(): String {
        val params = buildList {
            bbox?.let { add("bbox=$it") }
            timestamp?.let { add("timestamp=$it") }
            crop?.let { add("crop=$it") }
            h?.let { add("h=$it") }
            quality?.let { add("quality=$it") }
        }
        return if (params.isEmpty()) "" else "?" + params.joinToString("&")
    }
}

@Serializable
data class HaNotificationData(val image: String, @SerialName("clickAction") val clickAction: String)

@Serializable
data class HaNotification(val title: String, val message: String, val data: HaNotificationData)

@Serializable
data class Config(
    @SerialName("ntfy_enabled") val ntfyEnabled: Boolean,
    @SerialName("ntfy_url") val ntfyUrl: String,
    @SerialName("ntfy_user") val ntfyUser: String? = null,
    @SerialName("ntfy_password") val ntfyPassword: String? = null,
    @SerialName("ntfy_token") val ntfyToken: String? = null,
    @SerialName("ntfy_topic") val ntfyTopic: String,
    @SerialName("ntfy_tags") val ntfyTags: List<NtfyTag>? = null,
    @SerialName("grouping_enabled") val groupingEnabled: Boolean,
    @SerialName("grouping_minutes") val groupingMinutes: Int? = null,
    @SerialName("ntfy_normal_priority") val ntfyNormalPriority: Int,
    @SerialName("ntfy_lower_priority") val ntfyLowerPriority: Int? = null,
    @SerialName("ha_enabled") val haEnabled: Boolean,
    @SerialName("ha_entity_ids") val haEntityIds: List<String>? = null,
    @SerialName("frigate_url") val frigateUrl: String,
    @SerialName("disabled_cameras") val disabledCameras: List<DisabledCamera>? = null,
    @SerialName("disabled_objects") val disabledObjects: List<String>? = null,
    @SerialName("snapshot_options") val snapshotOptions: SnapshotOptions? = null,
    @SerialName("mqtt_address") val mqttAddress: String,
    @SerialName("mqtt_port") val mqttPort: Int? = null,
    @SerialName("mqtt_topic") val mqttTopic: String,
    @SerialName("mqtt_username") val mqttUsername: String? = null,
    @SerialName("mqtt_password") val mqttPassword: String? = null,
    @SerialName("log_level") val logLevel: String = "error"
)
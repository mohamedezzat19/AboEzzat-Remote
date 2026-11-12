package com.aboezzat.remote

import org.json.JSONObject

data class RemoteButtonProfile(
    val id: String,
    val label: String,
    val description: String,
    val group: String,
    val carrierFrequencyHz: Int?,
    val patternMicros: IntArray?,
    val verified: Boolean
)

data class RemoteProfile(
    val id: String,
    val brand: String,
    val model: String,
    val displayName: String,
    val verification: String,
    val buttons: List<RemoteButtonProfile>
)

object RemoteProfileParser {
    fun parse(json: String): RemoteProfile {
        val root = JSONObject(json)
        val defaultFrequency = root.optInt("carrierFrequencyHz", 38_000)
        val commands = root.optJSONObject("commands") ?: JSONObject()
        val buttonsJson = root.getJSONArray("buttons")
        val buttons = buildList {
            for (index in 0 until buttonsJson.length()) {
                val item = buttonsJson.getJSONObject(index)
                val commandId = item.optString("id")
                val command = commands.optJSONObject(commandId)
                val rawPattern = command?.optJSONArray("patternMicros")
                val pattern = if (rawPattern == null) null else IntArray(rawPattern.length()) { i ->
                    rawPattern.optInt(i, 0)
                }
                add(
                    RemoteButtonProfile(
                        id = commandId,
                        label = item.optString("label"),
                        description = item.optString("description"),
                        group = item.optString("group", "other"),
                        carrierFrequencyHz = command?.optInt("carrierFrequencyHz", defaultFrequency),
                        patternMicros = pattern,
                        verified = command?.optBoolean("verified", false) == true
                    )
                )
            }
        }
        return RemoteProfile(
            id = root.getString("id"),
            brand = root.optString("brand"),
            model = root.optString("model"),
            displayName = root.optString("displayName"),
            verification = root.optString("verification", "pending"),
            buttons = buttons
        )
    }
}

package uk.co.m66ike.mathsdash

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ScoreEntry(
    val name: String,
    val points: Int,
    val settingsKey: String,
    val settingsDescription: String,
    val timestamp: Long
)

class ScoreStore(context: Context) {
    private val prefs = context.getSharedPreferences("maths_dash", Context.MODE_PRIVATE)

    fun loadSettings(): GameSettings {
        return try {
            val raw = prefs.getString("settings", null) ?: return GameSettings()
            val json = JSONObject(raw)
            val tablesArray = json.optJSONArray("tables") ?: JSONArray()
            val tables = (0 until tablesArray.length()).map { tablesArray.optInt(it) }.toSet()
            val loaded = GameSettings(
                operation = Operation.valueOf(json.optString("operation", "ADD")),
                tileCount = json.optInt("tileCount", 6),
                maxNumber = json.optInt("maxNumber", 30),
                selectedTables = tables,
                multiplierLimit = json.optInt("multiplierLimit", 12),
                timeSeconds = json.optInt("timeSeconds", 60)
            )
            if (loaded.isValid()) loaded else GameSettings()
        } catch (_: Exception) {
            GameSettings()
        }
    }

    fun saveSettings(settings: GameSettings) {
        val json = JSONObject().apply {
            put("operation", settings.operation.name)
            put("tileCount", settings.tileCount)
            put("maxNumber", settings.maxNumber)
            put("tables", JSONArray(settings.selectedTables.sorted()))
            put("multiplierLimit", settings.multiplierLimit)
            put("timeSeconds", settings.timeSeconds)
        }
        prefs.edit().putString("settings", json.toString()).apply()
    }

    fun lastName(): String = prefs.getString("last_name", "") ?: ""

    fun saveLastName(name: String) {
        prefs.edit().putString("last_name", name).apply()
    }

    fun load(): List<ScoreEntry> {
        return try {
            val data = JSONArray(prefs.getString("scores", "[]") ?: "[]")
            (0 until data.length()).mapNotNull { index ->
                val item = data.optJSONObject(index) ?: return@mapNotNull null
                val name = item.optString("name")
                if (name.isBlank()) return@mapNotNull null
                ScoreEntry(
                    name = name,
                    points = item.optInt("points"),
                    settingsKey = item.optString("settingsKey"),
                    settingsDescription = item.optString("settingsDescription"),
                    timestamp = item.optLong("timestamp")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(entry: ScoreEntry): List<ScoreEntry> {
        val updated = load() + entry
        val json = JSONArray()
        updated.forEach {
            json.put(JSONObject().apply {
                put("name", it.name)
                put("points", it.points)
                put("settingsKey", it.settingsKey)
                put("settingsDescription", it.settingsDescription)
                put("timestamp", it.timestamp)
            })
        }
        prefs.edit().putString("scores", json.toString()).apply()
        return updated
    }
}

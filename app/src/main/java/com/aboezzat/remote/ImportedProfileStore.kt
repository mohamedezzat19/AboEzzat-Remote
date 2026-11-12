package com.aboezzat.remote

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale

/**
 * Imports user-supplied remote profiles into app-private storage.
 * Imported commands are always downgraded to unverified before saving.
 */
class ImportedProfileStore(context: Context) {
    private val directory = File(context.filesDir, "remote_profiles").apply { mkdirs() }

    fun importJson(source: String): RemoteProfile {
        require(source.toByteArray(Charsets.UTF_8).size <= MAX_FILE_BYTES) {
            "حجم الملف أكبر من 512 كيلوبايت"
        }

        val root = JSONObject(source)
        val id = root.getString("id").trim()
        require(ID_PATTERN.matches(id) && id != "." && id != "..") {
            "معرّف الريموت غير صالح"
        }

        val brand = root.getString("brand").trim()
        val model = root.getString("model").trim()
        val displayName = root.getString("displayName").trim()
        require(brand.isNotBlank() && model.isNotBlank() && displayName.isNotBlank()) {
            "بيانات الشركة أو الموديل أو الاسم ناقصة"
        }

        val buttons = root.getJSONArray("buttons")
        require(buttons.length() in 1..100) { "عدد الأزرار يجب أن يكون بين 1 و100" }
        val buttonIds = mutableSetOf<String>()
        for (index in 0 until buttons.length()) {
            val button = buttons.getJSONObject(index)
            val buttonId = button.getString("id").trim()
            require(ID_PATTERN.matches(buttonId) && buttonIds.add(buttonId)) {
                "معرّف زر غير صالح أو مكرر: $buttonId"
            }
            require(button.optString("label").isNotBlank()) { "يوجد زر بدون اسم" }
        }

        val commands = root.optJSONObject("commands") ?: JSONObject()
        val sanitizedCommands = JSONObject()
        val keys = commands.keys()
        while (keys.hasNext()) {
            val commandId = keys.next()
            require(commandId in buttonIds) { "الأمر $commandId لا يطابق أي زر في الملف" }
            val command = commands.getJSONObject(commandId)
            val frequency = command.optInt(
                "carrierFrequencyHz",
                root.optInt("carrierFrequencyHz", DEFAULT_FREQUENCY)
            )
            require(frequency in 20_000..60_000) { "تردد IR غير صالح للزر $commandId" }

            val timings = command.optJSONArray("patternMicros")
            require(timings != null) { "توقيتات IR مفقودة للزر $commandId" }
            require(timings.length() in 4..MAX_TIMINGS && timings.length() % 2 == 0) {
                "عدد توقيتات IR غير صالح للزر $commandId"
            }
            val safeTimings = JSONArray()
            for (i in 0 until timings.length()) {
                val rawDuration = timings.get(i)
                require(rawDuration is Number && rawDuration.toDouble() == rawDuration.toInt().toDouble()) {
                    "توقيت IR يجب أن يكون عددًا صحيحًا للزر $commandId"
                }
                val duration = (rawDuration as Number).toInt()
                require(duration in 1..MAX_DURATION_MICROS) {
                    "قيمة توقيت IR غير صالحة للزر $commandId"
                }
                safeTimings.put(duration)
            }

            sanitizedCommands.put(
                commandId,
                JSONObject()
                    .put("carrierFrequencyHz", frequency)
                    .put("patternMicros", safeTimings)
                    .put("verified", false)
            )
        }

        val safeRoot = JSONObject(source)
            .put("id", id)
            .put("brand", brand)
            .put("model", model)
            .put("displayName", displayName)
            .put("verification", "imported_unverified")
            .put("commands", sanitizedCommands)

        val destination = profileFile(id)
        require(!destination.exists()) { "يوجد ريموت بنفس المعرّف بالفعل: $id" }
        destination.writeText(safeRoot.toString(2), Charsets.UTF_8)
        return RemoteProfileParser.parse(safeRoot.toString())
    }

    fun listProfiles(): List<RemoteProfile> =
        directory.listFiles { file -> file.isFile && file.extension.lowercase(Locale.ROOT) == "json" }
            .orEmpty()
            .mapNotNull { file ->
                runCatching { RemoteProfileParser.parse(file.readText(Charsets.UTF_8)) }.getOrNull()
            }
            .sortedBy { it.displayName.lowercase(Locale.ROOT) }

    fun load(id: String): RemoteProfile {
        require(ID_PATTERN.matches(id) && id != "." && id != "..") { "معرّف ريموت غير صالح" }
        val file = profileFile(id)
        require(file.isFile) { "ملف الريموت غير موجود" }
        return RemoteProfileParser.parse(file.readText(Charsets.UTF_8))
    }

    fun exportJson(id: String): String {
        load(id) // Validate the stored profile before exporting it.
        return profileFile(id).readText(Charsets.UTF_8)
    }

    fun rename(id: String, newDisplayName: String): RemoteProfile {
        require(ID_PATTERN.matches(id) && id != "." && id != "..") { "معرّف ريموت غير صالح" }
        val name = newDisplayName.trim()
        require(name.isNotBlank() && name.length <= 80) { "الاسم يجب أن يكون من 1 إلى 80 حرفًا" }
        val file = profileFile(id)
        require(file.isFile) { "ملف الريموت غير موجود" }
        val root = JSONObject(file.readText(Charsets.UTF_8))
        root.put("displayName", name)
        root.put("verification", "imported_unverified")
        val commands = root.optJSONObject("commands") ?: JSONObject()
        val keys = commands.keys()
        while (keys.hasNext()) commands.getJSONObject(keys.next()).put("verified", false)
        val temporary = File(directory, "$id.json.tmp")
        temporary.writeText(root.toString(2), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw IllegalStateException("تعذر حفظ الاسم الجديد")
        }
        return RemoteProfileParser.parse(root.toString())
    }

    fun moveButton(id: String, buttonId: String, direction: Int): RemoteProfile {
        require(ID_PATTERN.matches(id) && id != "." && id != "..") { "معرّف ريموت غير صالح" }
        require(direction == -1 || direction == 1) { "اتجاه الترتيب غير صالح" }
        val file = profileFile(id)
        require(file.isFile) { "ملف الريموت غير موجود" }
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val buttons = root.getJSONArray("buttons")
        val currentIndex = (0 until buttons.length()).firstOrNull {
            buttons.getJSONObject(it).optString("id") == buttonId
        } ?: throw IllegalArgumentException("الزر غير موجود")
        val current = buttons.getJSONObject(currentIndex)
        val group = current.optString("group", "other")
        val groupIndexes = (0 until buttons.length()).filter {
            buttons.getJSONObject(it).optString("group", "other") == group
        }
        val groupPosition = groupIndexes.indexOf(currentIndex)
        val targetPosition = groupPosition + direction
        require(targetPosition in groupIndexes.indices) { "وصلت إلى نهاية المجموعة" }
        val targetIndex = groupIndexes[targetPosition]
        val target = buttons.getJSONObject(targetIndex)
        buttons.put(currentIndex, target)
        buttons.put(targetIndex, current)
        root.put("verification", "imported_unverified")
        val commands = root.optJSONObject("commands") ?: JSONObject()
        val keys = commands.keys()
        while (keys.hasNext()) commands.getJSONObject(keys.next()).put("verified", false)
        val temporary = File(directory, "$id.json.tmp")
        temporary.writeText(root.toString(2), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw IllegalStateException("تعذر حفظ ترتيب الأزرار")
        }
        return RemoteProfileParser.parse(root.toString())
    }

    fun renameGroup(id: String, group: String, newGroup: String): RemoteProfile {
        require(ID_PATTERN.matches(id) && id != "." && id != "..") { "معرّف ريموت غير صالح" }
        val oldName = group.trim()
        val newName = newGroup.trim()
        require(oldName.isNotBlank() && newName.isNotBlank() && newName.length <= 40) {
            "اسم المجموعة يجب أن يكون من 1 إلى 40 حرفًا"
        }
        val file = profileFile(id)
        require(file.isFile) { "ملف الريموت غير موجود" }
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val buttons = root.getJSONArray("buttons")
        var changed = false
        for (index in 0 until buttons.length()) {
            val button = buttons.getJSONObject(index)
            if (button.optString("group", "other") == oldName) {
                button.put("group", newName)
                changed = true
            }
        }
        require(changed) { "المجموعة غير موجودة" }
        return saveUnverifiedProfile(id, file, root, "تعذر حفظ اسم المجموعة")
    }

    fun moveGroup(id: String, group: String, direction: Int): RemoteProfile {
        require(ID_PATTERN.matches(id) && id != "." && id != "..") { "معرّف ريموت غير صالح" }
        require(direction == -1 || direction == 1) { "اتجاه الترتيب غير صالح" }
        val file = profileFile(id)
        require(file.isFile) { "ملف الريموت غير موجود" }
        val root = JSONObject(file.readText(Charsets.UTF_8))
        val buttons = root.getJSONArray("buttons")
        val groups = (0 until buttons.length())
            .map { buttons.getJSONObject(it).optString("group", "other") }
            .distinct()
        val current = groups.indexOf(group)
        require(current >= 0) { "المجموعة غير موجودة" }
        val target = current + direction
        require(target in groups.indices) { "وصلت إلى نهاية ترتيب المجموعات" }
        val reordered = groups.toMutableList().apply {
            val item = removeAt(current)
            add(target, item)
        }
        val groupedButtons = reordered.flatMap { name ->
            (0 until buttons.length()).map { buttons.getJSONObject(it) }
                .filter { it.optString("group", "other") == name }
        }
        val replacement = JSONArray()
        groupedButtons.forEach { replacement.put(it) }
        root.put("buttons", replacement)
        return saveUnverifiedProfile(id, file, root, "تعذر حفظ ترتيب المجموعات")
    }

    private fun saveUnverifiedProfile(
        id: String,
        file: File,
        root: JSONObject,
        errorMessage: String
    ): RemoteProfile {
        root.put("verification", "imported_unverified")
        val commands = root.optJSONObject("commands") ?: JSONObject()
        val keys = commands.keys()
        while (keys.hasNext()) commands.getJSONObject(keys.next()).put("verified", false)
        val temporary = File(directory, "$id.json.tmp")
        temporary.writeText(root.toString(2), Charsets.UTF_8)
        if (!temporary.renameTo(file)) {
            temporary.delete()
            throw IllegalStateException(errorMessage)
        }
        return RemoteProfileParser.parse(root.toString())
    }

    fun delete(id: String): Boolean {
        require(ID_PATTERN.matches(id) && id != "." && id != "..") { "معرّف ريموت غير صالح" }
        val file = profileFile(id)
        require(file.isFile) { "ملف الريموت غير موجود" }
        return file.delete()
    }

    private fun profileFile(id: String) = File(directory, "$id.json")

    companion object {
        private const val MAX_FILE_BYTES = 512 * 1024
        private const val MAX_TIMINGS = 1000
        private const val MAX_DURATION_MICROS = 100_000
        private const val DEFAULT_FREQUENCY = 38_000
        private val ID_PATTERN = Regex("[A-Za-z0-9._-]{1,64}")
    }
}

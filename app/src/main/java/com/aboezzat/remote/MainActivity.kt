package com.aboezzat.remote

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import java.io.ByteArrayOutputStream
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private val bg = Color.rgb(10, 16, 30)
    private val panel = Color.rgb(21, 31, 51)
    private val accent = Color.rgb(83, 156, 255)
    private val muted = Color.rgb(158, 174, 199)
    private var showingJvc = false
    private lateinit var root: LinearLayout
    private lateinit var irTransmitter: InfraredTransmitter
    private lateinit var profileRepository: RemoteProfileRepository
    private var pendingExportProfileId: String? = null

    companion object {
        private const val REQUEST_IMPORT_PROFILE = 4102
        private const val REQUEST_EXPORT_PROFILE = 4103
        private const val MAX_IMPORT_BYTES = 512 * 1024
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        irTransmitter = InfraredTransmitter(this)
        profileRepository = RemoteProfileRepository(this)
        showHome()
    }

    private fun background(color: Int, radius: Float = 22f): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius * resources.displayMetrics.density
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun text(value: String, size: Float, color: Int = Color.WHITE, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun column(padding: Int = 20): LinearLayout =
        LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(padding), dp(padding), dp(padding), dp(padding))
        }

    private fun baseScreen(): ScrollView {
        root = column(20).apply { setBackgroundColor(bg) }
        return ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            addView(root)
        }
    }

    private fun heading(title: String, subtitle: String) {
        root.addView(text("ABOEZZAT  •  REMOTE", 12f, accent, true))
        val titleView = text(title, 28f, Color.WHITE, true)
        val titleParams = LinearLayout.LayoutParams(-1, -2)
        titleParams.topMargin = dp(8)
        root.addView(titleView, titleParams)
        val sub = text(subtitle, 14f, muted)
        val subParams = LinearLayout.LayoutParams(-1, -2)
        subParams.topMargin = dp(5)
        root.addView(sub, subParams)
    }

    private fun card(title: String, subtitle: String, symbol: String, onClick: () -> Unit) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = background(panel)
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
        }
        val icon = TextView(this).apply {
            text = symbol
            textSize = 27f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = background(Color.rgb(34, 54, 83), 18f)
        }
        item.addView(icon, LinearLayout.LayoutParams(dp(54), dp(54)))
        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), 0, 0, 0)
            addView(text(title, 17f, Color.WHITE, true))
            val detail = text(subtitle, 12f, muted)
            val p = LinearLayout.LayoutParams(-1, -2)
            p.topMargin = dp(4)
            addView(detail, p)
        }
        item.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        item.addView(text("›", 28f, muted, true))
        val params = LinearLayout.LayoutParams(-1, -2)
        params.topMargin = dp(10)
        root.addView(item, params)
    }

    private fun showHome() {
        showingJvc = false
        val scroll = baseScreen()
        heading("أبو عزت ريموت", "كل أجهزتك في مكان واحد")
        val hasIr = irTransmitter.isAvailable()
        val status = if (hasIr) "تم اكتشاف مرسل أشعة تحت الحمراء في الهاتف"
        else "لم يتم اكتشاف مرسل IR مدمج في هذا الهاتف"
        val statusCard = TextView(this).apply {
            text = "●  $status"
            textSize = 12f
            setTextColor(if (hasIr) Color.rgb(112, 220, 165) else Color.rgb(255, 190, 100))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = background(panel, 16f)
        }
        val statusParams = LinearLayout.LayoutParams(-1, -2)
        statusParams.topMargin = dp(20)
        root.addView(statusCard, statusParams)

        val section = text("اختر فئة الجهاز", 18f, Color.WHITE, true)
        val sectionParams = LinearLayout.LayoutParams(-1, -2)
        sectionParams.topMargin = dp(24)
        root.addView(section, sectionParams)

        card("التلفزيونات", "LG • Samsung • Sony وغيرها", "▣") { comingSoon("التلفزيونات") }
        card("التكييفات", "الحرارة • الوضع • سرعة المروحة", "❄") { comingSoon("التكييفات") }
        card("المراوح", "السرعة • التذبذب • المؤقت", "✺") { comingSoon("المراوح") }
        card("المسجلات والراديو", "CD • USB • Cassette • FM", "♫") { showJvc() }
        card("أنظمة الصوت", "مكبرات الصوت وأجهزة الاستريو", "◖))") { comingSoon("أنظمة الصوت") }
        card("أجهزتي المخصصة", "إضافة موديلات وريموتات جديدة", "+") { comingSoon("إضافة جهاز") }
        card("استيراد ريموت من ملف", "اختيار ملف JSON من الهاتف", "⇧") { pickRemoteProfile() }
        val importedCount = profileRepository.listImportedProfiles().size
        card("ريموتاتي المستوردة", "$importedCount ريموت محفوظ داخل التطبيق", "⌁") { showImportedProfiles() }

        val footer = text("الإصدار 1.0.0  •  منصة ريموتات قابلة للتوسع", 11f, muted)
        root.addView(footer, LinearLayout.LayoutParams(-1, dp(52)))
        setContentView(scroll)
    }


    private fun pickRemoteProfile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/json", "text/json", "text/plain"))
        }
        try {
            startActivityForResult(
                Intent.createChooser(intent, "اختيار ملف ريموت JSON"),
                REQUEST_IMPORT_PROFILE
            )
        } catch (error: Exception) {
            Toast.makeText(this, "تعذر فتح مدير الملفات: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        if (requestCode == REQUEST_EXPORT_PROFILE) {
            val profileId = pendingExportProfileId
            pendingExportProfileId = null
            if (profileId == null) return
            try {
                val json = profileRepository.exportImportedProfile(profileId)
                contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(json) }
                    ?: throw IllegalStateException("تعذر فتح الملف للحفظ")
                Toast.makeText(this, "تم تصدير ملف الريموت بنجاح", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(this, "فشل التصدير: ${error.message}", Toast.LENGTH_LONG).show()
            }
            return
        }

        if (requestCode != REQUEST_IMPORT_PROFILE) return
        try {
            val json = readLimitedText(uri)
            val imported = profileRepository.importProfile(json)
            Toast.makeText(
                this,
                "تم استيراد ${imported.displayName} وحفظه كملف غير موثّق الأوامر",
                Toast.LENGTH_LONG
            ).show()
            showHome()
        } catch (error: Exception) {
            Toast.makeText(
                this,
                "لم يتم استيراد الملف: ${error.message ?: "ملف غير صالح"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun readLimitedText(uri: Uri): String {
        val input = contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("تعذر قراءة الملف المحدد")
        input.use { stream ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var total = 0
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                total += count
                require(total <= MAX_IMPORT_BYTES) { "حجم الملف أكبر من 512 كيلوبايت" }
                output.write(buffer, 0, count)
            }
            return output.toString(Charsets.UTF_8.name())
        }
    }

    private fun exportRemoteProfile(id: String) {
        val profile = runCatching { profileRepository.loadImportedProfile(id) }.getOrElse {
            Toast.makeText(this, "تعذر تجهيز التصدير: ${it.message}", Toast.LENGTH_LONG).show()
            return
        }
        val safeName = profile.id.replace(Regex("[^A-Za-z0-9._-]"), "_")
        pendingExportProfileId = id
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "$safeName.json")
        }
        try {
            startActivityForResult(Intent.createChooser(intent, "حفظ نسخة من الريموت"), REQUEST_EXPORT_PROFILE)
        } catch (error: Exception) {
            pendingExportProfileId = null
            Toast.makeText(this, "تعذر فتح نافذة الحفظ: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun renameRemoteProfile(id: String) {
        val profile = runCatching { profileRepository.loadImportedProfile(id) }.getOrElse {
            Toast.makeText(this, "تعذر تحميل الريموت: ${it.message}", Toast.LENGTH_LONG).show()
            return
        }
        val field = EditText(this).apply {
            setText(profile.displayName)
            setSelection(text.length)
            hint = "اسم الريموت"
            setSingleLine(true)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("تعديل اسم الريموت")
            .setView(field)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                try {
                    val updated = profileRepository.renameImportedProfile(id, field.text.toString())
                    Toast.makeText(this, "تم حفظ الاسم الجديد", Toast.LENGTH_SHORT).show()
                    showImportedProfile(updated.id)
                } catch (error: Exception) {
                    Toast.makeText(this, "تعذر حفظ الاسم: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun confirmDeleteRemoteProfile(id: String) {
        val profile = runCatching { profileRepository.loadImportedProfile(id) }.getOrElse {
            Toast.makeText(this, "تعذر تحميل بيانات الريموت: ${it.message}", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("حذف الريموت")
            .setMessage("هل تريد حذف «${profile.displayName}» من الريموتات المستوردة؟ لا يمكن التراجع عن الحذف.")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حذف") { _, _ ->
                try {
                    if (profileRepository.deleteImportedProfile(id)) {
                        Toast.makeText(this, "تم حذف الريموت المستورد", Toast.LENGTH_SHORT).show()
                        showImportedProfiles()
                    } else {
                        Toast.makeText(this, "لم يتم حذف الملف", Toast.LENGTH_LONG).show()
                    }
                } catch (error: Exception) {
                    Toast.makeText(this, "تعذر الحذف: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun showImportedProfiles() {
        showingJvc = false
        val profiles = profileRepository.listImportedProfiles()
        val scroll = baseScreen()
        val back = Button(this).apply {
            text = "‹   الرئيسية"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showHome() }
        }
        root.addView(back, LinearLayout.LayoutParams(-2, dp(48)))
        heading("ريموتاتي المستوردة", "ملفات محفوظة داخل أبو عزت ريموت")
        if (profiles.isEmpty()) {
            root.addView(
                text("لسه ما استوردتش أي ريموت. اختار ملف JSON من الصفحة الرئيسية لإضافته هنا.", 14f, muted),
                LinearLayout.LayoutParams(-1, -2)
            )
        } else {
            profiles.forEach { profile ->
                card(profile.displayName, "${profile.brand} • ${profile.model}", "⌁") {
                    showImportedProfile(profile.id)
                }
            }
        }
        setContentView(scroll)
    }

    private fun showImportedProfile(id: String) {
        val profile = try {
            profileRepository.loadImportedProfile(id)
        } catch (error: Exception) {
            Toast.makeText(this, "تعذر فتح الريموت: ${error.message}", Toast.LENGTH_LONG).show()
            showImportedProfiles()
            return
        }
        showingJvc = false
        val scroll = baseScreen()
        val back = Button(this).apply {
            text = "‹   ريموتاتي"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showImportedProfiles() }
        }
        root.addView(back, LinearLayout.LayoutParams(-2, dp(48)))
        heading(profile.displayName, "${profile.brand} • ${profile.model}")

        val remote = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(20))
            background = background(Color.rgb(37, 43, 55), 30f)
        }
        val brand = text(profile.brand.uppercase(), 22f, Color.WHITE, true)
        brand.gravity = Gravity.CENTER
        remote.addView(brand, LinearLayout.LayoutParams(-1, dp(48)))
        val display = TextView(this).apply {
            text = profile.displayName
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(165, 205, 190))
            background = background(Color.rgb(15, 23, 28), 10f)
        }
        val displayParams = LinearLayout.LayoutParams(-1, dp(58))
        displayParams.topMargin = dp(8)
        displayParams.bottomMargin = dp(12)
        remote.addView(display, displayParams)

        val importedGroups = profile.buttons.map { it.group }.distinct()
        importedGroups.forEachIndexed { groupIndex, group ->
            addImportedButtonGroup(remote, profile, group, groupIndex, importedGroups.size)
        }
        root.addView(remote, LinearLayout.LayoutParams(-1, -2))
        card("تعديل اسم الريموت", "تغيير الاسم الظاهر في التطبيق", "✎") {
            renameRemoteProfile(profile.id)
        }
        card("تصدير نسخة JSON", "حفظ الريموت في ملف لمشاركته أو نقله", "⇩") {
            exportRemoteProfile(profile.id)
        }
        card("حذف هذا الريموت", "إزالة النسخة المستوردة من التطبيق", "×") {
            confirmDeleteRemoteProfile(profile.id)
        }
        val note = text(
            "هذا ريموت مستورد. جميع أوامر IR تم تعطيل التحقق منها تلقائيًا؛ لن يتم إرسال أي أمر حتى يُختبر ويُعتمد بشكل مستقل.",
            12f,
            muted
        )
        val noteParams = LinearLayout.LayoutParams(-1, -2)
        noteParams.topMargin = dp(16)
        root.addView(note, noteParams)
        setContentView(scroll)
    }


    private fun addImportedButtonGroup(
        parent: LinearLayout,
        profile: RemoteProfile,
        group: String,
        groupIndex: Int,
        groupCount: Int
    ) {
        val buttons = profile.buttons.filter { it.group == group }
        if (buttons.isEmpty()) return
        val groupLabel = when (group) {
            "numeric" -> "لوحة الأرقام"
            "navigation" -> "التنقل والتحكم"
            "source" -> "مصادر التشغيل"
            "effects" -> "التكرار والبرمجة"
            "sound" -> "الصوت والمؤقت"
            "volume" -> "مستوى الصوت"
            else -> group.uppercase()
        }
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val label = text(groupLabel, 11f, muted, true)
        header.addView(label, LinearLayout.LayoutParams(0, dp(38), 1f))
        val rename = Button(this).apply {
            text = "✎"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = background(Color.rgb(47, 67, 91), 10f)
            setOnClickListener { renameImportedGroup(profile.id, group) }
        }
        val up = Button(this).apply {
            text = "↑"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = background(Color.rgb(47, 67, 91), 10f)
            isEnabled = groupIndex > 0
            alpha = if (isEnabled) 1f else 0.35f
            setOnClickListener { reorderImportedGroup(profile.id, group, -1) }
        }
        val down = Button(this).apply {
            text = "↓"
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = background(Color.rgb(47, 67, 91), 10f)
            isEnabled = groupIndex < groupCount - 1
            alpha = if (isEnabled) 1f else 0.35f
            setOnClickListener { reorderImportedGroup(profile.id, group, 1) }
        }
        header.addView(rename, LinearLayout.LayoutParams(dp(42), dp(36)))
        header.addView(up, LinearLayout.LayoutParams(dp(42), dp(36)))
        header.addView(down, LinearLayout.LayoutParams(dp(42), dp(36)))
        val labelParams = LinearLayout.LayoutParams(-1, -2)
        labelParams.topMargin = dp(8)
        parent.addView(header, labelParams)

        buttons.forEachIndexed { index, button ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val key = remoteKey(button)
            val keyParams = LinearLayout.LayoutParams(0, dp(46), 1f)
            keyParams.setMargins(dp(3), dp(3), dp(6), dp(3))
            row.addView(key, keyParams)

            val up = Button(this).apply {
                text = "↑"
                textSize = 16f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = background(Color.rgb(47, 67, 91), 12f)
                isEnabled = index > 0
                alpha = if (isEnabled) 1f else 0.35f
                setOnClickListener { reorderImportedButton(profile.id, button.id, -1) }
            }
            val down = Button(this).apply {
                text = "↓"
                textSize = 16f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = background(Color.rgb(47, 67, 91), 12f)
                isEnabled = index < buttons.lastIndex
                alpha = if (isEnabled) 1f else 0.35f
                setOnClickListener { reorderImportedButton(profile.id, button.id, 1) }
            }
            row.addView(up, LinearLayout.LayoutParams(dp(48), dp(44)))
            val downParams = LinearLayout.LayoutParams(dp(48), dp(44))
            downParams.leftMargin = dp(5)
            row.addView(down, downParams)
            parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun renameImportedGroup(id: String, group: String) {
        val field = EditText(this).apply {
            setText(group)
            setSelection(text.length)
            hint = "اسم المجموعة"
            setSingleLine(true)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("تغيير اسم المجموعة")
            .setView(field)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                try {
                    val updated = profileRepository.renameImportedGroup(id, group, field.text.toString())
                    showImportedProfile(updated.id)
                    Toast.makeText(this, "تم حفظ اسم المجموعة", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(this, "تعذر تغيير الاسم: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    private fun reorderImportedGroup(id: String, group: String, direction: Int) {
        try {
            val updated = profileRepository.moveImportedGroup(id, group, direction)
            showImportedProfile(updated.id)
            Toast.makeText(this, "تم تحديث ترتيب المجموعات", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "تعذر تغيير ترتيب المجموعة", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reorderImportedButton(id: String, buttonId: String, direction: Int) {
        try {
            val updated = profileRepository.moveImportedButton(id, buttonId, direction)
            showImportedProfile(updated.id)
            Toast.makeText(this, "تم تحديث ترتيب الأزرار", Toast.LENGTH_SHORT).show()
        } catch (error: Exception) {
            Toast.makeText(this, error.message ?: "تعذر تغيير الترتيب", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showJvc() {
        showingJvc = true
        val profile = try {
            profileRepository.loadJvcCassette()
        } catch (error: Exception) {
            Toast.makeText(this, "تعذر تحميل ملف ريموت JVC: ${error.message}", Toast.LENGTH_LONG).show()
            showHome()
            return
        }

        val scroll = baseScreen()
        val back = Button(this).apply {
            text = "‹   الأجهزة"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showHome() }
        }
        root.addView(back, LinearLayout.LayoutParams(-2, dp(48)))
        heading(profile.displayName, "ريموت مطابق لتخطيط RM-SRCEZ38A")

        val remote = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(20))
            background = background(Color.rgb(37, 43, 55), 30f)
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = text("JVC", 25f, Color.WHITE, true)
        topRow.addView(brand, LinearLayout.LayoutParams(0, dp(48), 1f))
        profile.buttons.firstOrNull { it.id == "power" }?.let {
            topRow.addView(remoteKey(it, Color.rgb(130, 49, 57)), LinearLayout.LayoutParams(dp(58), dp(52)))
        }
        remote.addView(topRow)

        val display = TextView(this).apply {
            text = "RC-EZ38S  •  CD / USB / FM"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(165, 205, 190))
            background = background(Color.rgb(15, 23, 28), 10f)
        }
        val displayParams = LinearLayout.LayoutParams(-1, dp(68))
        displayParams.topMargin = dp(10)
        displayParams.bottomMargin = dp(16)
        remote.addView(display, displayParams)

        addButtonGroup(remote, profile, "source", 2)
        addButtonGroup(remote, profile, "navigation", 3)
        addButtonGroup(remote, profile, "effects", 3)
        addButtonGroup(remote, profile, "sound", 3)
        addNumericPad(remote, profile)
        addButtonGroup(remote, profile, "volume", 2)

        root.addView(remote, LinearLayout.LayoutParams(-1, -2))

        val stateText = "تخطيط الأزرار مستند إلى دليل JVC RC-EZ38S والريموت RM-SRCEZ38A. أكواد الأشعة تحت الحمراء لم تُوثّق أو تُختبر بعد؛ لذلك الأزرار لن ترسل أوامر حتى تتوفر أكواد مؤكدة."
        val note = text(stateText, 12f, muted)
        val np = LinearLayout.LayoutParams(-1, -2)
        np.topMargin = dp(16)
        root.addView(note, np)
        setContentView(scroll)
    }

    private fun addButtonGroup(
        parent: LinearLayout,
        profile: RemoteProfile,
        group: String,
        columns: Int
    ) {
        val buttons = profile.buttons.filter { it.group == group }
        if (buttons.isEmpty()) return
        val groupLabel = when (group) {
            "source" -> "مصادر التشغيل"
            "navigation" -> "التنقل والتحكم"
            "effects" -> "التكرار والبرمجة"
            "sound" -> "الصوت والمؤقت"
            "volume" -> "مستوى الصوت"
            else -> group.uppercase()
        }
        val heading = text(groupLabel, 11f, muted, true)
        val headingParams = LinearLayout.LayoutParams(-1, dp(28))
        headingParams.topMargin = dp(8)
        parent.addView(heading, headingParams)
        var index = 0
        while (index < buttons.size) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            repeat(columns) {
                if (index < buttons.size) {
                    val button = buttons[index++]
                    val p = LinearLayout.LayoutParams(0, dp(if (group == "navigation") 54 else 48), 1f)
                    p.setMargins(dp(3), dp(4), dp(3), dp(4))
                    row.addView(remoteKey(button), p)
                } else {
                    row.addView(TextView(this), LinearLayout.LayoutParams(0, dp(48), 1f))
                }
            }
            parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun addNumericPad(parent: LinearLayout, profile: RemoteProfile) {
        val buttons = profile.buttons.filter { it.group == "numeric" }
        if (buttons.isEmpty()) return
        val heading = text("لوحة الأرقام", 11f, muted, true)
        val headingParams = LinearLayout.LayoutParams(-1, dp(28))
        headingParams.topMargin = dp(8)
        parent.addView(heading, headingParams)

        val byId = buttons.associateBy { it.id }
        val rows = listOf(
            listOf("digit_1", "digit_2", "digit_3"),
            listOf("digit_4", "digit_5", "digit_6"),
            listOf("digit_7", "digit_8", "digit_9"),
            listOf("", "digit_0", "")
        )
        rows.forEach { rowIds ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            rowIds.forEach { id ->
                val params = LinearLayout.LayoutParams(0, dp(46), 1f)
                params.setMargins(dp(3), dp(3), dp(3), dp(3))
                val button = byId[id]
                if (button == null) {
                    row.addView(TextView(this), params)
                } else {
                    row.addView(remoteKey(button), params)
                }
            }
            parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun remoteKey(
        button: RemoteButtonProfile,
        color: Int = Color.rgb(67, 79, 99)
    ): Button = Button(this).apply {
        text = button.label
        contentDescription = button.description
        textSize = if (button.label.length > 7) 10f else 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = background(color, 16f)
        setOnClickListener { sendRemoteButton(button) }
    }

    private fun sendRemoteButton(button: RemoteButtonProfile) {
        if (!button.verified || button.carrierFrequencyHz == null || button.patternMicros == null) {
            Toast.makeText(this, "${button.description}: كود IR موثوق غير متوفر لهذا الزر بعد", Toast.LENGTH_SHORT).show()
            return
        }
        irTransmitter.send(button.carrierFrequencyHz, button.patternMicros).fold(
            onSuccess = {
                Toast.makeText(this, "تم إرسال أمر ${button.description}", Toast.LENGTH_SHORT).show()
            },
            onFailure = { error ->
                Toast.makeText(this, "تعذر إرسال الأمر: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun comingSoon(category: String) {
        Toast.makeText(this, "$category: سيتم إضافة موديلاته وأكواد التحكم تباعاً", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (showingJvc) showHome() else super.onBackPressed()
    }
}

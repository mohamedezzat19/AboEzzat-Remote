package com.aboezzat.remote

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import java.io.ByteArrayOutputStream
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Random

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
    private val irHandler = Handler(Looper.getMainLooper())
    private var scanRunnable: Runnable? = null
    private var scanDialog: AlertDialog? = null
    private var scanStatusView: TextView? = null
    private var scanTargetButtonId: String? = null
    private var scanCandidates: List<Pair<Int, Int>> = emptyList()
    private var scanIndex = 0
    private var lastSentJvcCode: Pair<Int, Int>? = null

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
            val keyHeight = when (button.buttonSize) {
                "small" -> 40
                "large" -> 54
                else -> 46
            }
            val keyParams = LinearLayout.LayoutParams(0, dp(keyHeight), 1f)
            keyParams.setMargins(dp(3), dp(3), dp(6), dp(3))
            row.addView(key, keyParams)

            val style = Button(this).apply {
                text = "🎨"
                textSize = 14f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = background(Color.rgb(47, 67, 91), 12f)
                contentDescription = "تخصيص لون وشكل وحجم الزر"
                setOnClickListener { customizeImportedButton(profile.id, button.id) }
            }
            val edit = Button(this).apply {
                text = "✎"
                textSize = 15f
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = background(Color.rgb(47, 67, 91), 12f)
                contentDescription = "تعديل اسم الزر"
                setOnClickListener { renameImportedButton(profile.id, button.id) }
            }
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
            row.addView(style, LinearLayout.LayoutParams(dp(42), dp(44)))
            row.addView(edit, LinearLayout.LayoutParams(dp(42), dp(44)))
            row.addView(up, LinearLayout.LayoutParams(dp(40), dp(44)))
            val downParams = LinearLayout.LayoutParams(dp(44), dp(44))
            downParams.leftMargin = dp(5)
            row.addView(down, downParams.apply { width = dp(40) })
            parent.addView(row, LinearLayout.LayoutParams(-1, -2))
        }
    }

    private fun renameImportedButton(id: String, buttonId: String) {
        val profile = runCatching { profileRepository.loadImportedProfile(id) }.getOrElse {
            Toast.makeText(this, "تعذر تحميل الريموت: ${it.message}", Toast.LENGTH_LONG).show()
            return
        }
        val button = profile.buttons.firstOrNull { it.id == buttonId } ?: return
        val field = EditText(this).apply {
            setText(button.label)
            setSelection(text.length)
            hint = "اسم الزر"
            setSingleLine(true)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }
        AlertDialog.Builder(this)
            .setTitle("تعديل اسم الزر")
            .setMessage("التغيير يؤثر على اسم الزر الظاهر فقط، ولا يغيّر كود الأشعة تحت الحمراء.")
            .setView(field)
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("حفظ") { _, _ ->
                try {
                    val updated = profileRepository.renameImportedButton(id, buttonId, field.text.toString())
                    showImportedProfile(updated.id)
                    Toast.makeText(this, "تم حفظ اسم الزر", Toast.LENGTH_SHORT).show()
                } catch (error: Exception) {
                    Toast.makeText(this, "تعذر حفظ الاسم: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
            .show()
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

        addJvcBasicRow(remote, profile, "مصدر USB", listOf("usb"))
        addJvcBasicRow(remote, profile, "الراديو", listOf("tuner_band"))
        addJvcBasicRow(remote, profile, "التنقل (USB / محطات الراديو)", listOf("previous", "next"))
        addJvcBasicRow(remote, profile, "مستوى الصوت", listOf("volume_down", "volume_up"))

        root.addView(remote, LinearLayout.LayoutParams(-1, -2))

        val stateText = "اختبار JVC تجريبي: اختر الوظيفة وابدأ البحث. عند استجابة المسجل اضغط «نجح - حفظ» فوراً. الهاتف لا يستطيع اكتشاف استجابة المسجل تلقائياً."
        val note = text(stateText, 12f, muted)
        val np = LinearLayout.LayoutParams(-1, -2)
        np.topMargin = dp(16)
        root.addView(note, np)
        card("البحث التلقائي عن أكواد الأزرار", "1000 محاولة أولية، كود كل ثانيتين، مع حفظ الكود عند نجاحه", "⌁") {
            showJvcIrCandidateTests()
        }
        setContentView(scroll)
    }

    private fun showJvcIrCandidateTests() {
        val targets = listOf(
            "power" to "Power تشغيل / إيقاف",
            "usb" to "USB",
            "tuner_band" to "الراديو TUNER / BAND",
            "volume_up" to "رفع الصوت",
            "volume_down" to "خفض الصوت",
            "previous" to "السابق",
            "next" to "التالي"
        )
        AlertDialog.Builder(this)
            .setTitle("اختار الزر اللي هنبحث عن كوده")
            .setItems(targets.map { it.second }.toTypedArray()) { _, which ->
                startJvcScan(targets[which].first)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun buildJvcScanCandidates(buttonId: String): List<Pair<Int, Int>> {
        val prefs = getSharedPreferences("jvc_ir_tests", MODE_PRIVATE)
        val knownAddress = if (buttonId != "power" && prefs.getBoolean("jvc_verified_power", false))
            prefs.getInt("jvc_address_power", -1).takeIf { it in 0..255 } else null
        if (knownAddress != null) return (0..255).map { command -> knownAddress to command }

        // A reproducible, non-repeating sample across the complete 16-bit JVC
        // address/command space. This is a discovery sample, not a guaranteed code list.
        val random = Random(System.currentTimeMillis() xor buttonId.hashCode().toLong())
        val seen = HashSet<Int>()
        val candidates = ArrayList<Pair<Int, Int>>(1000)
        while (candidates.size < 1000) {
            val address = random.nextInt(256)
            val command = random.nextInt(256)
            val key = (address shl 8) or command
            if (seen.add(key)) candidates.add(address to command)
        }
        return candidates
    }

    private fun startJvcScan(buttonId: String) {
        if (!irTransmitter.isAvailable()) {
            Toast.makeText(this, "هذا الهاتف لا يحتوي على مرسل IR مدمج", Toast.LENGTH_LONG).show()
            return
        }
        scanTargetButtonId = buttonId
        scanCandidates = buildJvcScanCandidates(buttonId)
        scanIndex = 0
        lastSentJvcCode = null

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(16), dp(22), dp(8))
        }
        scanStatusView = text("جاهز لبدء الاختبار", 15f, Color.WHITE, true)
        layout.addView(scanStatusView, LinearLayout.LayoutParams(-1, dp(64)))
        val details = text(
            "وجّه أعلى الهاتف ناحية حساس المسجل وثبّت الهاتف. كل كود يُرسل مرة واحدة ثم ننتظر ثانيتين. قد تستجيب بعض الأكواد لوظائف أخرى؛ راقب المسجل واضغط حفظ فور حدوث الوظيفة المطلوبة.",
            12f, muted
        )
        layout.addView(details, LinearLayout.LayoutParams(-1, -2))

        val dialog = AlertDialog.Builder(this)
            .setTitle("بحث IR: ${targetsLabel(buttonId)}")
            .setView(layout)
            .setPositiveButton("نجح - حفظ الكود", null)
            .setNeutralButton("إيقاف", null)
            .setNegativeButton("إلغاء الاختبار", null)
            .create()
        scanDialog = dialog
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val code = lastSentJvcCode
                if (code == null) {
                    Toast.makeText(this, "استنى لحد ما أول كود يتبعت", Toast.LENGTH_SHORT).show()
                } else {
                    val id = scanTargetButtonId ?: return@setOnClickListener
                    getSharedPreferences("jvc_ir_tests", MODE_PRIVATE).edit()
                        .putBoolean("jvc_verified_$id", true)
                        .putInt("jvc_address_$id", code.first)
                        .putInt("jvc_command_$id", code.second)
                        .apply()
                    stopJvcScan()
                    dialog.dismiss()
                    Toast.makeText(
                        this,
                        "اتحفظ كود ${targetsLabel(id)}: العنوان 0x${code.first.toString(16).uppercase().padStart(2, '0')} والأمر 0x${code.second.toString(16).uppercase().padStart(2, '0')}",
                        Toast.LENGTH_LONG
                    ).show()
                    showJvc()
                }
            }
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                stopJvcScan()
                dialog.dismiss()
            }
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener {
                stopJvcScan()
                dialog.dismiss()
            }
        }
        dialog.setOnDismissListener { stopJvcScan() }
        dialog.show()
        sendNextJvcCandidate()
    }

    private fun targetsLabel(id: String): String = when (id) {
        "power" -> "Power"
        "usb" -> "USB"
        "tuner_band" -> "الراديو"
        "volume_up" -> "رفع الصوت"
        "volume_down" -> "خفض الصوت"
        "previous" -> "السابق"
        "next" -> "التالي"
        else -> id
    }

    private fun sendNextJvcCandidate() {
        if (scanIndex >= scanCandidates.size) {
            scanStatusView?.text = "انتهت المحاولات (${scanCandidates.size}). لم يتم حفظ كود. يمكنك إعادة البحث."
            scanRunnable = null
            return
        }
        val code = scanCandidates[scanIndex]
        lastSentJvcCode = code
        val sent = irTransmitter.send(
            JvcProtocolEncoder.CARRIER_FREQUENCY_HZ,
            JvcProtocolEncoder.encode(code.first, code.second)
        )
        scanStatusView?.text = "المحاولة ${scanIndex + 1} من ${scanCandidates.size}\nالعنوان: 0x${code.first.toString(16).uppercase().padStart(2, '0')}  •  الأمر: 0x${code.second.toString(16).uppercase().padStart(2, '0')}\n${if (sent.isSuccess) "تم الإرسال — انتظر الاستجابة" else "فشل الإرسال: ${sent.exceptionOrNull()?.message}"}"
        scanIndex++
        if (sent.isSuccess && scanIndex < scanCandidates.size) {
            val next = Runnable { sendNextJvcCandidate() }
            scanRunnable = next
            irHandler.postDelayed(next, 2_000L)
        } else if (scanIndex >= scanCandidates.size) {
            scanStatusView?.text = "انتهت المحاولات (${scanCandidates.size}). لم يتم حفظ كود."
        } else {
            scanRunnable = null
        }
    }

    private fun stopJvcScan() {
        scanRunnable?.let { irHandler.removeCallbacks(it) }
        scanRunnable = null
        scanDialog = null
    }

    private fun addJvcBasicRow(
        parent: LinearLayout,
        profile: RemoteProfile,
        label: String,
        ids: List<String>
    ) {
        val heading = text(label, 12f, muted, true)
        val headingParams = LinearLayout.LayoutParams(-1, dp(28))
        headingParams.topMargin = dp(10)
        parent.addView(heading, headingParams)
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val buttons = ids.mapNotNull { id -> profile.buttons.firstOrNull { it.id == id } }
        buttons.forEach { button ->
            val params = LinearLayout.LayoutParams(0, dp(52), 1f)
            params.setMargins(dp(4), dp(4), dp(4), dp(4))
            row.addView(remoteKey(button), params)
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, -2))
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

    private fun customizeImportedButton(profileId: String, buttonId: String) {
        val profile = runCatching { profileRepository.loadImportedProfile(profileId) }.getOrElse {
            Toast.makeText(this, "تعذر تحميل الريموت: ${it.message}", Toast.LENGTH_LONG).show()
            return
        }
        val button = profile.buttons.firstOrNull { it.id == buttonId } ?: return
        var selectedColor = button.buttonColor
        var selectedShape = button.buttonShape
        var selectedSize = button.buttonSize
        val colors = listOf(
            "slate" to "رمادي داكن",
            "blue" to "أزرق",
            "green" to "أخضر",
            "red" to "أحمر",
            "purple" to "بنفسجي",
            "amber" to "ذهبي"
        )
        val shapes = listOf(
            "rounded" to "مستدير",
            "pill" to "بيضاوي",
            "square" to "مربع"
        )
        val sizes = listOf(
            "small" to "صغير",
            "medium" to "متوسط",
            "large" to "كبير"
        )
        lateinit var chooseColor: () -> Unit
        lateinit var chooseShape: () -> Unit
        lateinit var chooseSize: () -> Unit
        chooseSize = {
            AlertDialog.Builder(this)
                .setTitle("حجم زر «${button.label}»")
                .setSingleChoiceItems(sizes.map { it.second }.toTypedArray(),
                    sizes.indexOfFirst { it.first == selectedSize }.coerceAtLeast(0)) { dialog, which ->
                    selectedSize = sizes[which].first
                    dialog.dismiss()
                    try {
                        profileRepository.setImportedButtonAppearance(
                            profileId, buttonId, selectedColor, selectedShape, selectedSize
                        )
                        Toast.makeText(this, "تم حفظ مظهر الزر", Toast.LENGTH_SHORT).show()
                        showImportedProfile(profileId)
                    } catch (error: Exception) {
                        Toast.makeText(this, "تعذر حفظ المظهر: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
        chooseShape = {
            AlertDialog.Builder(this)
                .setTitle("شكل زر «${button.label}»")
                .setSingleChoiceItems(shapes.map { it.second }.toTypedArray(),
                    shapes.indexOfFirst { it.first == selectedShape }.coerceAtLeast(0)) { dialog, which ->
                    selectedShape = shapes[which].first
                    dialog.dismiss()
                    chooseSize()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
        chooseColor = {
            AlertDialog.Builder(this)
                .setTitle("لون زر «${button.label}»")
                .setSingleChoiceItems(colors.map { it.second }.toTypedArray(),
                    colors.indexOfFirst { it.first == selectedColor }.coerceAtLeast(0)) { dialog, which ->
                    selectedColor = colors[which].first
                    dialog.dismiss()
                    chooseShape()
                }
                .setNegativeButton("إلغاء", null)
                .show()
        }
        chooseColor()
    }

    private fun remoteKey(
        button: RemoteButtonProfile,
        color: Int = Color.rgb(67, 79, 99)
    ): Button = Button(this).apply {
        val buttonColor = when (button.buttonColor) {
            "blue" -> Color.rgb(37, 99, 180)
            "green" -> Color.rgb(35, 125, 91)
            "red" -> Color.rgb(160, 58, 62)
            "purple" -> Color.rgb(108, 76, 156)
            "amber" -> Color.rgb(161, 112, 37)
            else -> color
        }
        val radius = when (button.buttonShape) {
            "pill" -> 28f
            "square" -> 5f
            else -> 16f
        }
        text = button.label
        contentDescription = button.description
        textSize = if (button.label.length > 7) 10f else 14f
        isAllCaps = false
        setTextColor(Color.WHITE)
        background = background(buttonColor, radius)
        setOnClickListener { sendRemoteButton(button) }
    }

    private fun sendRemoteButton(button: RemoteButtonProfile) {
        if (showingJvc) {
            val prefs = getSharedPreferences("jvc_ir_tests", MODE_PRIVATE)
            if (prefs.getBoolean("jvc_verified_${button.id}", false)) {
                val address = prefs.getInt("jvc_address_${button.id}", -1)
                val command = prefs.getInt("jvc_command_${button.id}", -1)
                if (address in 0..255 && command in 0..255) {
                    irTransmitter.send(
                        JvcProtocolEncoder.CARRIER_FREQUENCY_HZ,
                        JvcProtocolEncoder.encode(address, command)
                    ).fold(
                        onSuccess = { Toast.makeText(this, "تم إرسال أمر ${button.description}", Toast.LENGTH_SHORT).show() },
                        onFailure = { error -> Toast.makeText(this, "تعذر إرسال الأمر: ${error.message}", Toast.LENGTH_LONG).show() }
                    )
                    return
                }
            }
            Toast.makeText(this, "${button.description}: لم يتم العثور على كود بعد. افتح البحث التلقائي لهذا الزر.", Toast.LENGTH_LONG).show()
            return
        }
        if (!button.verified || button.carrierFrequencyHz == null || button.patternMicros == null) {
            Toast.makeText(this, "${button.description}: كود IR موثوق غير متوفر.", Toast.LENGTH_LONG).show()
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

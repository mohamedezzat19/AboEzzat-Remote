package com.aboezzat.remote

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
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

        val footer = text("الإصدار 1.0.0  •  منصة ريموتات قابلة للتوسع", 11f, muted)
        root.addView(footer, LinearLayout.LayoutParams(-1, dp(52)))
        setContentView(scroll)
    }

    private fun showJvc() {
        showingJvc = true
        val profile = try {
            profileRepository.loadJvcRcEz38s()
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
        heading(profile.displayName, "ريموت المسجل • ملف جهاز مستقل")

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
            text = "JVC  •  RC-EZ38S\nAUDIO SYSTEM"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(165, 205, 190))
            background = background(Color.rgb(15, 23, 28), 10f)
        }
        val displayParams = LinearLayout.LayoutParams(-1, dp(68))
        displayParams.topMargin = dp(10)
        displayParams.bottomMargin = dp(16)
        remote.addView(display, displayParams)

        addButtonGroup(remote, profile, "source", 3)
        addButtonGroup(remote, profile, "navigation", 3)
        addButtonGroup(remote, profile, "effects", 3)
        addButtonGroup(remote, profile, "sound", 3)
        addNumericPad(remote, profile)
        addButtonGroup(remote, profile, "volume", 2)

        root.addView(remote, LinearLayout.LayoutParams(-1, -2))

        val stateText = if (profile.verification.equals("verified", true))
            "ملف الأوامر موثق"
        else "ملف الأوامر قيد التحقق — الأزرار غير الموثقة لن ترسل إشارات"
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
            "effects" -> "وظائف التشغيل"
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
                    val p = LinearLayout.LayoutParams(0, dp(if (group == "transport") 54 else 48), 1f)
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

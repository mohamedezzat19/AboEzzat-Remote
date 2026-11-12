package com.aboezzat.remote

import android.app.Activity
import android.content.Context
import android.hardware.ConsumerIrManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
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
    private var irManager: ConsumerIrManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        irManager = getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
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
        val status = if (irManager?.hasIrEmitter() == true)
            "هذا الهاتف يحتوي على مرسل أشعة تحت الحمراء"
        else "لم يتم اكتشاف مرسل IR في هذا الهاتف"
        val statusCard = TextView(this).apply {
            text = "●  $status"
            textSize = 12f
            setTextColor(if (irManager?.hasIrEmitter() == true) Color.rgb(112, 220, 165) else Color.rgb(255, 190, 100))
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
        val fp = LinearLayout.LayoutParams(-1, dp(52))
        root.addView(footer, fp)
        setContentView(scroll)
    }

    private fun showJvc() {
        showingJvc = true
        val scroll = baseScreen()
        val back = Button(this).apply {
            text = "‹   الأجهزة"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { showHome() }
        }
        root.addView(back, LinearLayout.LayoutParams(-2, dp(48)))
        heading("JVC RC-EZ38S", "ريموت المسجل • ملف الجهاز الأول")

        val remote = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(20), dp(18), dp(20))
            background = background(Color.rgb(37, 43, 55), 30f)
        }
        val brand = text("JVC", 24f, Color.WHITE, true).apply { gravity = Gravity.CENTER }
        remote.addView(brand, LinearLayout.LayoutParams(-1, dp(42)))
        val screen = TextView(this).apply {
            text = "RC-EZ38S\nREMOTE CONTROL"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(165, 205, 190))
            background = background(Color.rgb(15, 23, 28), 10f)
        }
        val screenParams = LinearLayout.LayoutParams(-1, dp(72))
        screenParams.topMargin = dp(8)
        screenParams.bottomMargin = dp(18)
        remote.addView(screen, screenParams)

        remoteButton(remote, "POWER", "power")
        remoteButton(remote, "CD", "cd")
        remoteButton(remote, "USB", "usb")
        remoteButton(remote, "TUNER / BAND", "tuner")
        remoteButton(remote, "TAPE", "tape")

        val transport = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf("⏮", "▶ / II", "■", "⏭").forEach { label ->
            val b = Button(this).apply {
                text = label
                textSize = 12f
                setTextColor(Color.WHITE)
                background = background(Color.rgb(67, 79, 99), 18f)
                isAllCaps = false
                setOnClickListener { notProgrammed(label) }
            }
            val p = LinearLayout.LayoutParams(0, dp(54), 1f)
            p.setMargins(dp(3), dp(8), dp(3), dp(8))
            transport.addView(b, p)
        }
        remote.addView(transport)
        remoteButton(remote, "VOLUME  −", "volume_down")
        remoteButton(remote, "VOLUME  +", "volume_up")
        remoteButton(remote, "REPEAT / PROGRAM", "repeat")

        root.addView(remote, LinearLayout.LayoutParams(-1, -2))
        val note = text(
            "واجهة الريموت جاهزة كبداية. أكواد الإرسال الخاصة بهذا الموديل لم تُعتمد بعد؛ لن نرسل أكواداً تخمينية.",
            12f, muted
        )
        val np = LinearLayout.LayoutParams(-1, -2)
        np.topMargin = dp(16)
        root.addView(note, np)
        setContentView(scroll)
    }

    private fun remoteButton(parent: LinearLayout, label: String, command: String) {
        val b = Button(this).apply {
            text = label
            textSize = 13f
            isAllCaps = false
            setTextColor(Color.WHITE)
            background = background(Color.rgb(67, 79, 99), 16f)
            setOnClickListener { notProgrammed(command) }
        }
        val p = LinearLayout.LayoutParams(-1, dp(48))
        p.topMargin = dp(5)
        parent.addView(b, p)
    }

    private fun notProgrammed(command: String) {
        Toast.makeText(this, "زر $command: كود IR الموثوق غير مضاف بعد", Toast.LENGTH_SHORT).show()
    }

    private fun comingSoon(category: String) {
        Toast.makeText(this, "$category: سيتم إضافة موديلاته وأكواد التحكم تباعاً", Toast.LENGTH_SHORT).show()
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (showingJvc) showHome() else super.onBackPressed()
    }
}

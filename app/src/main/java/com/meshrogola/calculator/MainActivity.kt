package com.meshrogola.calculator

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.DecimalFormat
import kotlin.math.*

class MainActivity : Activity() {
    private lateinit var expressionView: TextView
    private lateinit var resultView: TextView
    private lateinit var historyView: TextView
    private val history = mutableListOf<String>()
    private var expression = ""
    private var justEvaluated = false

    private val bg = Color.rgb(7, 13, 24)
    private val panel = Color.rgb(14, 26, 43)
    private val cyan = Color.rgb(44, 218, 255)
    private val muted = Color.rgb(126, 157, 180)
    private val white = Color.rgb(235, 247, 255)
    private val orange = Color.rgb(255, 174, 83)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = 0
        buildInterface()
        refresh()
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(12))
            setBackgroundColor(bg)
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val brand = TextView(this).apply {
            text = "MESH ROGOLA"
            textSize = 21f
            letterSpacing = 0.12f
            setTextColor(cyan)
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
        }
        val version = TextView(this).apply {
            text = "  // CALC 01"
            textSize = 10f
            setTextColor(muted)
            typeface = Typeface.MONOSPACE
        }
        header.addView(brand)
        header.addView(version)
        root.addView(header)

        val subtitle = TextView(this).apply {
            text = "SMART SCIENTIFIC CALCULATOR"
            textSize = 10f
            letterSpacing = 0.18f
            setTextColor(muted)
            setPadding(0, dp(4), 0, dp(16))
        }
        root.addView(subtitle)

        val display = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.END
            setPadding(dp(16), dp(16), dp(16), dp(18))
            background = rounded(panel, dp(18), Color.rgb(27, 67, 91), dp(1))
            minimumHeight = dp(148)
        }
        expressionView = TextView(this).apply {
            textSize = 19f
            setTextColor(muted)
            gravity = Gravity.END
            typeface = Typeface.MONOSPACE
            maxLines = 2
        }
        resultView = TextView(this).apply {
            text = "0"
            textSize = 38f
            setTextColor(white)
            gravity = Gravity.END
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
            setPadding(0, dp(12), 0, 0)
            maxLines = 2
        }
        display.addView(expressionView, LinearLayout.LayoutParams(-1, 0, 1f))
        display.addView(resultView)
        root.addView(display, LinearLayout.LayoutParams(-1, 0, 2.2f).apply {
            bottomMargin = dp(12)
        })

        val keys = listOf(
            "AC", "⌫", "(", ")", "sin", "cos", "tan", "÷",
            "7", "8", "9", "×", "4", "5", "6", "−",
            "1", "2", "3", "+", "π", "0", ".", "=",
            "√", "x²", "^", "log", "ln", "abs", "e", "%"
        )
        val grid = GridLayout(this).apply {
            columnCount = 4
            rowCount = 8
            useDefaultMargins = false
        }
        keys.forEach { key ->
            val button = Button(this).apply {
                text = key
                textSize = when (key) { "sin", "cos", "tan", "log", "abs" -> 13f; else -> 18f }
                isAllCaps = false
                typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                setTextColor(when (key) {
                    "AC" -> Color.rgb(255, 127, 127)
                    "=", "÷", "×", "−", "+", "^" -> cyan
                    "sin", "cos", "tan", "√", "log", "ln", "abs", "π", "e", "x²", "%" -> orange
                    else -> white
                })
                background = rounded(
                    if (key == "=") Color.rgb(8, 91, 119) else if (key in listOf("÷","×","−","+","^")) Color.rgb(18, 43, 62) else panel,
                    dp(13),
                    if (key == "=") cyan else Color.rgb(27, 52, 72),
                    dp(1)
                )
                setPadding(0, 0, 0, 0)
                minHeight = dp(52)
                setOnClickListener { onKey(key) }
            }
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = dp(52)
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(dp(3), dp(3), dp(3), dp(3))
            }
            grid.addView(button, lp)
        }
        root.addView(grid, LinearLayout.LayoutParams(-1, 0, 4.2f))

        historyView = TextView(this).apply {
            text = "HISTORY  •  لا توجد عمليات بعد"
            textSize = 10f
            setTextColor(muted)
            typeface = Typeface.MONOSPACE
            setPadding(dp(4), dp(12), dp(4), 0)
            maxLines = 2
        }
        root.addView(historyView)
        setContentView(root)
    }

    private fun onKey(key: String) {
        when (key) {
            "AC" -> { expression = ""; justEvaluated = false; resultView.setTextColor(white); refresh() }
            "⌫" -> { expression = expression.dropLast(1); justEvaluated = false; refresh() }
            "=" -> calculate()
            else -> {
                if (justEvaluated && (key.firstOrNull()?.isDigit() == true || key == "." || key == "π" || key == "e")) expression = ""
                justEvaluated = false
                expression += when (key) {
                    "×" -> "*"; "÷" -> "/"; "−" -> "-"; "π" -> "pi"
                    "√" -> "sqrt("; "x²" -> "^2"; "sin", "cos", "tan", "log", "ln", "abs" -> "$key("
                    else -> key
                }
                refresh()
            }
        }
    }

    private fun calculate() {
        if (expression.isBlank()) return
        try {
            val value = MathParser(expression).parse()
            if (!value.isFinite()) throw IllegalArgumentException("not finite")
            val answer = format(value)
            history.add(0, "$expression = $answer")
            if (history.size > 4) history.removeAt(history.lastIndex)
            expressionView.text = expression
            resultView.text = answer
            resultView.setTextColor(cyan)
            historyView.text = "HISTORY  •  " + history.joinToString("     |     ")
            justEvaluated = true
        } catch (_: Exception) {
            resultView.text = "مش رجولة 😂"
            resultView.textSize = 27f
            resultView.setTextColor(Color.rgb(255, 111, 125))
            expressionView.text = expression
            justEvaluated = true
        }
    }

    private fun refresh() {
        expressionView.text = expression.ifBlank { "اكتب المسألة..." }
        resultView.text = "0"
        resultView.textSize = 38f
        resultView.setTextColor(white)
    }

    private fun format(value: Double): String {
        if (abs(value) < 1e-10) return "0"
        val df = DecimalFormat("#,##0.##########")
        return df.format(value)
    }

    private fun rounded(color: Int, radius: Int, stroke: Int, strokeWidth: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            setStroke(strokeWidth, stroke)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private class MathParser(private val input: String) {
        private var pos = 0
        fun parse(): Double {
            val result = expression()
            skip()
            if (pos != input.length) error("Unexpected input")
            return result
        }
        private fun skip() { while (pos < input.length && input[pos].isWhitespace()) pos++ }
        private fun eat(c: Char): Boolean {
            skip()
            if (pos < input.length && input[pos] == c) { pos++; return true }
            return false
        }
        private fun expression(): Double {
            var x = term()
            while (true) x = when {
                eat('+') -> x + term()
                eat('-') -> x - term()
                else -> return x
            }
        }
        private fun term(): Double {
            var x = power()
            while (true) x = when {
                eat('*') -> x * power()
                eat('/') -> {
                    val d = power()
                    if (d == 0.0) error("Division by zero")
                    x / d
                }
                eat('%') -> x / 100.0
                else -> return x
            }
        }
        private fun power(): Double {
            val x = unary()
            return if (eat('^')) x.pow(power()) else x
        }
        private fun unary(): Double {
            if (eat('+')) return unary()
            if (eat('-')) return -unary()
            return atom()
        }
        private fun atom(): Double {
            skip()
            if (eat('(')) {
                val x = expression()
                if (!eat(')')) error("Missing )")
                return x
            }
            if (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) return number()
            val start = pos
            while (pos < input.length && input[pos].isLetter()) pos++
            if (start == pos) error("Expected value")
            val name = input.substring(start, pos).lowercase()
            if (name == "pi") return Math.PI
            if (name == "e") return Math.E
            if (!eat('(')) error("Function needs parentheses")
            val v = expression()
            if (!eat(')')) error("Missing )")
            return when (name) {
                "sqrt" -> sqrt(v)
                "sin" -> sin(Math.toRadians(v))
                "cos" -> cos(Math.toRadians(v))
                "tan" -> tan(Math.toRadians(v))
                "log" -> log10(v)
                "ln" -> ln(v)
                "abs" -> abs(v)
                else -> error("Unknown function")
            }
        }
        private fun number(): Double {
            skip()
            val start = pos
            var dots = 0
            while (pos < input.length && (input[pos].isDigit() || input[pos] == '.')) {
                if (input[pos] == '.') dots++
                if (dots > 1) error("Bad number")
                pos++
            }
            return input.substring(start, pos).toDouble()
        }
    }
}
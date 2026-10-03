@file:Suppress("NAME_SHADOWING")

package com.kushal.mealapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// --------------------------------------------------------------------
// Global App-Wide Floating Calculator State
// --------------------------------------------------------------------
object FloatingCalculatorState {
    var isVisible by mutableStateOf(true)   // Default to visible globally
    var isMinimized by mutableStateOf(true)  // Default to minimized floating bubble

    // Position coordinates
    var offsetX by mutableFloatStateOf(-1f)
    var offsetY by mutableFloatStateOf(-1f)

    // Persistent Calculation State across minimize, maximize & recomposition
    var input by mutableStateOf("")
    var history by mutableStateOf("")
    var isResultDisplayed by mutableStateOf(false)
    var lastOperator by mutableStateOf<String?>(null)
    var openParenthesesCount by mutableIntStateOf(0)

    fun clear() {
        input = ""
        history = ""
        isResultDisplayed = false
        lastOperator = null
        openParenthesesCount = 0
    }

    fun show() {
        isVisible = true
        isMinimized = false
        // Reset coordinates so it re-anchors in screen
        offsetX = -1f
        offsetY = -1f
    }

    fun minimize() {
        isMinimized = true
    }

    fun expand() {
        isMinimized = false
    }

    fun dismiss() {
        isVisible = false
    }
}

@Composable
fun GlobalCalculatorOverlay() {
    val imeBottomPadding = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val isImeVisible = imeBottomPadding > 0.dp

    if (FloatingCalculatorState.isVisible) {
        // Automatically hide the minimized floating bubble when the soft keyboard (IME) is visible
        // so it NEVER blocks or intercepts key presses on the bottom-right of the soft keyboard!
        if (!FloatingCalculatorState.isMinimized || !isImeVisible) {
            CalculatorPopup(onDismiss = { FloatingCalculatorState.dismiss() })
        }
    }
}

@Composable
fun CalculatorPopup(onDismiss: () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val minX = with(density) { 16.dp.toPx() }
    val maxX = (screenWidthPx - with(density) { 170.dp.toPx() }).coerceAtLeast(minX)
    val minY = with(density) { 60.dp.toPx() }
    val maxY = (screenHeightPx - with(density) { 120.dp.toPx() }).coerceAtLeast(minY)

    // Clamp or initialize default position
    if (FloatingCalculatorState.offsetX < 0f || FloatingCalculatorState.offsetX > maxX) {
        FloatingCalculatorState.offsetX = maxX
    }
    if (FloatingCalculatorState.offsetY < 0f || FloatingCalculatorState.offsetY > maxY) {
        FloatingCalculatorState.offsetY = maxY
    }

    fun clear() {
        FloatingCalculatorState.clear()
    }

    fun handleNumber(num: String) {
        if (FloatingCalculatorState.isResultDisplayed) {
            FloatingCalculatorState.input = num
            FloatingCalculatorState.history = ""
            FloatingCalculatorState.isResultDisplayed = false
        } else {
            FloatingCalculatorState.input += num
        }
    }

    fun handleOperator(op: String) {
        if (FloatingCalculatorState.input.isNotEmpty()) {
            FloatingCalculatorState.history += "${FloatingCalculatorState.input} $op "
            FloatingCalculatorState.input = ""
            FloatingCalculatorState.lastOperator = op
            FloatingCalculatorState.isResultDisplayed = false
        } else if (FloatingCalculatorState.lastOperator != null) {
            FloatingCalculatorState.history = FloatingCalculatorState.history.dropLast(2) + "$op "
            FloatingCalculatorState.lastOperator = op
        }
    }

    fun backspace() {
        if (FloatingCalculatorState.input.isNotEmpty()) {
            FloatingCalculatorState.input = FloatingCalculatorState.input.dropLast(1)
        } else if (FloatingCalculatorState.history.isNotEmpty()) {
            val tokens = FloatingCalculatorState.history.trim().split(" ")
            if (tokens.size >= 2) {
                FloatingCalculatorState.history = tokens.dropLast(2).joinToString(" ") + " "
                FloatingCalculatorState.lastOperator = tokens.getOrNull(tokens.size - 3)
            }
        }
    }

    fun handleParenthesis(open: Boolean) {
        if (open) {
            if (FloatingCalculatorState.isResultDisplayed) clear()
            FloatingCalculatorState.input += "("
            FloatingCalculatorState.openParenthesesCount++
        } else if (FloatingCalculatorState.openParenthesesCount > 0) {
            FloatingCalculatorState.input += ")"
            FloatingCalculatorState.openParenthesesCount--
        }
    }

    fun handleDecimal() {
        if (FloatingCalculatorState.isResultDisplayed) {
            FloatingCalculatorState.input = "0."
            FloatingCalculatorState.history = ""
            FloatingCalculatorState.isResultDisplayed = false
        } else if (!FloatingCalculatorState.input.contains(".")) {
            FloatingCalculatorState.input += "."
        }
    }

    fun handleSquare() {
        if (FloatingCalculatorState.input.isNotEmpty()) {
            try {
                val number = FloatingCalculatorState.input.toDouble()
                FloatingCalculatorState.input = (number.pow(2)).toString()
            } catch (_: Exception) {
                FloatingCalculatorState.input = "Error"
                FloatingCalculatorState.isResultDisplayed = true
            }
        }
    }

    fun handleRoot() {
        if (FloatingCalculatorState.input.isNotEmpty()) {
            try {
                val number = FloatingCalculatorState.input.toDouble()
                FloatingCalculatorState.input = sqrt(number).toString()
            } catch (_: Exception) {
                FloatingCalculatorState.input = "Error"
                FloatingCalculatorState.isResultDisplayed = true
            }
        }
    }

    fun handlePercent() {
        if (FloatingCalculatorState.input.isNotEmpty()) {
            try {
                val number = FloatingCalculatorState.input.toDouble()
                FloatingCalculatorState.input = (number / 100).toString()
            } catch (_: Exception) {
                FloatingCalculatorState.input = "Error"
                FloatingCalculatorState.isResultDisplayed = true
            }
        }
    }

    fun handlePlusMinus() {
        if (FloatingCalculatorState.input.isNotEmpty()) {
            FloatingCalculatorState.input = if (FloatingCalculatorState.input.startsWith("-")) {
                FloatingCalculatorState.input.drop(1)
            } else {
                "-${FloatingCalculatorState.input}"
            }
        }
    }

    fun applyOperator(operator: String, a: Double, b: Double): Double = when (operator) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> if (b != 0.0) a / b else throw ArithmeticException("Division by zero")
        else -> throw IllegalArgumentException("Unknown operator: $operator")
    }

    fun evaluateExpression(expression: String): Double {
        val tokens = expression.replace("(", " ( ").replace(")", " ) ")
            .split(" ").filter { it.isNotEmpty() }

        val values = mutableListOf<Double>()
        val operators = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.toDoubleOrNull() != null -> values.add(token.toDouble())
                token == "(" -> operators.add(token)
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.last() != "(") {
                        val b = values.removeLastOrNull() ?: 0.0
                        val a = values.removeLastOrNull() ?: 0.0
                        val op = operators.removeAt(operators.lastIndex)
                        values.add(applyOperator(op, a, b))
                    }
                    if (operators.isNotEmpty() && operators.last() == "(")
                        operators.removeAt(operators.lastIndex)
                }
                token in listOf("+", "-", "*", "/") -> {
                    while (operators.isNotEmpty() && operators.last() in listOf("+", "-", "*", "/")) {
                        val b = values.removeLastOrNull() ?: 0.0
                        val a = values.removeLastOrNull() ?: 0.0
                        val op = operators.removeAt(operators.lastIndex)
                        values.add(applyOperator(op, a, b))
                    }
                    operators.add(token)
                }
            }
            i++
        }

        while (operators.isNotEmpty()) {
            val b = values.removeLastOrNull() ?: 0.0
            val a = values.removeLastOrNull() ?: 0.0
            val op = operators.removeAt(operators.lastIndex)
            values.add(applyOperator(op, a, b))
        }

        return values.lastOrNull() ?: 0.0
    }

    fun calculate() {
        if (FloatingCalculatorState.input.isNotEmpty() || FloatingCalculatorState.history.isNotEmpty()) {
            FloatingCalculatorState.history += FloatingCalculatorState.input
            try {
                val result = evaluateExpression(FloatingCalculatorState.history)
                FloatingCalculatorState.input = result.toString()
                FloatingCalculatorState.history = ""
                FloatingCalculatorState.isResultDisplayed = true
                FloatingCalculatorState.lastOperator = null
                FloatingCalculatorState.openParenthesesCount = 0
            } catch (_: Exception) {
                FloatingCalculatorState.input = "Error"
                FloatingCalculatorState.isResultDisplayed = true
            }
        }
    }

    if (FloatingCalculatorState.isMinimized) {
        // Non-blocking Box overlay for minimized floating bubble
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(end = 16.dp, bottom = 90.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1565C0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .offset {
                        IntOffset(
                            FloatingCalculatorState.offsetX.roundToInt(),
                            FloatingCalculatorState.offsetY.roundToInt()
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            FloatingCalculatorState.offsetX += dragAmount.x
                            FloatingCalculatorState.offsetY += dragAmount.y
                        }
                    }
                    .clickable { FloatingCalculatorState.expand() }
                    .border(2.dp, Color(0xFF80D8FF), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🧮 ", fontSize = 18.sp)
                    Text(
                        "Calculator",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.25f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "✕",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    } else {
        // Expanded Floating Calculator Window
        Popup(
            alignment = Alignment.Center,
            onDismissRequest = onDismiss,
            properties = PopupProperties(
                focusable = true,
                dismissOnClickOutside = false,
                dismissOnBackPress = true
            )
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .widthIn(max = 340.dp)
                    .padding(12.dp)
                    .border(1.5.dp, Color(0xFF1565C0), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Header Bar (Title + Minimize + Close)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🧮 ", fontSize = 16.sp)
                            Text(
                                "Calculator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFF002B49)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Minimize Button
                            IconButton(
                                onClick = { FloatingCalculatorState.minimize() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("➖", fontSize = 12.sp)
                            }

                            // Close Button
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(
                                    "✕",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828)
                                )
                            }
                        }
                    }

                    // Display Screen
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF002B49)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (FloatingCalculatorState.history.isNotEmpty()) {
                                Text(
                                    text = FloatingCalculatorState.history,
                                    fontSize = 12.sp,
                                    color = Color(0xFF80D8FF),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = FloatingCalculatorState.input.ifEmpty { if (FloatingCalculatorState.history.isEmpty()) "0" else "" },
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Keypad Grid
                    val buttons = listOf(
                        listOf("C", "⌫", "%", "÷"),
                        listOf("7", "8", "9", "×"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("+/-", "0", ".", "="),
                        listOf("(", ")", "x²", "√")
                    )

                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            row.forEach { label ->
                                val btnBg = when (label) {
                                    "=" -> Color(0xFF1565C0)
                                    "C" -> Color(0xFFD32F2F)
                                    "⌫" -> Color(0xFFE53935)
                                    "+", "-", "×", "÷" -> Color(0xFF0288D1)
                                    "%", "+/-", "(", ")", "x²", "√" -> Color(0xFFE0F7FA)
                                    else -> Color.White
                                }

                                val txtColor = when (label) {
                                    "=", "C", "⌫", "+", "-", "×", "÷" -> Color.White
                                    "%", "+/-", "(", ")", "x²", "√" -> Color(0xFF00695C)
                                    else -> Color(0xFF1F2937)
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(40.dp)
                                        .clickable {
                                            val opSymbol = when (label) {
                                                "÷" -> "/"
                                                "×" -> "*"
                                                else -> label
                                            }
                                            when (label) {
                                                in "0".."9" -> handleNumber(label)
                                                "+", "-", "×", "÷" -> handleOperator(opSymbol)
                                                "=" -> calculate()
                                                "." -> handleDecimal()
                                                "(" -> handleParenthesis(true)
                                                ")" -> handleParenthesis(false)
                                                "x²" -> handleSquare()
                                                "√" -> handleRoot()
                                                "C" -> clear()
                                                "%" -> handlePercent()
                                                "+/-" -> handlePlusMinus()
                                                "⌫" -> backspace()
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = btnBg),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = txtColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

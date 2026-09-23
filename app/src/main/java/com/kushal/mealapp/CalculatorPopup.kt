@file:Suppress("NAME_SHADOWING")

package com.kushal.mealapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.AlertDialogDefaults.containerColor
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun CalculatorPopup(onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    var history by remember { mutableStateOf("") }
    var isResultDisplayed by remember { mutableStateOf(false) }
    var lastOperator by remember { mutableStateOf<String?>(null) }
    var openParenthesesCount by remember { mutableStateOf(0) }

    fun updateDisplayText(): String = (history + input).ifEmpty { "0" }

    fun clear() {
        input = ""
        history = ""
        isResultDisplayed = false
        lastOperator = null
        openParenthesesCount = 0
    }

    fun handleNumber(num: String) {
        if (isResultDisplayed) {
            input = num
            history = ""
            isResultDisplayed = false
        } else input += num
    }

    fun handleOperator(op: String) {
        if (input.isNotEmpty()) {
            history += "$input $op "
            input = ""
            lastOperator = op
            isResultDisplayed = false
        } else if (lastOperator != null) {
            history = history.dropLast(2) + "$op "
            lastOperator = op
        }
    }

    fun backspace() {
        if (input.isNotEmpty()) input = input.dropLast(1)
        else if (history.isNotEmpty()) {
            val tokens = history.trim().split(" ")
            if (tokens.size >= 2) {
                history = tokens.dropLast(2).joinToString(" ") + " "
                lastOperator = tokens.getOrNull(tokens.size - 3)
            }
        }
    }

    fun handleParenthesis(open: Boolean) {
        if (open) {
            if (isResultDisplayed) clear()
            input += "("
            openParenthesesCount++
        } else if (openParenthesesCount > 0) {
            input += ")"
            openParenthesesCount--
        }
    }

    fun handleDecimal() {
        if (isResultDisplayed) {
            input = "0."
            history = ""
            isResultDisplayed = false
        } else if (!input.contains(".")) input += "."
    }

    fun handleSquare() {
        if (input.isNotEmpty()) {
            try {
                val number = input.toDouble()
                input = (number.pow(2)).toString()
            } catch (_: Exception) {
                input = "Error"
                isResultDisplayed = true
            }
        }
    }

    fun handleRoot() {
        if (input.isNotEmpty()) {
            try {
                val number = input.toDouble()
                input = sqrt(number).toString()
            } catch (_: Exception) {
                input = "Error"
                isResultDisplayed = true
            }
        }
    }

    fun handlePercent() {
        if (input.isNotEmpty()) {
            try {
                val number = input.toDouble()
                input = (number / 100).toString()
            } catch (_: Exception) {
                input = "Error"
                isResultDisplayed = true
            }
        }
    }

    fun handlePlusMinus() {
        if (input.isNotEmpty()) {
            input = if (input.startsWith("-")) input.drop(1) else "-$input"
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
        if (input.isNotEmpty() || history.isNotEmpty()) {
            history += input
            try {
                val result = evaluateExpression(history)
                input = result.toString()
                history = ""
                isResultDisplayed = true
                lastOperator = null
                openParenthesesCount = 0
            } catch (_: Exception) {
                input = "Error"
                isResultDisplayed = true
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            tonalElevation = 10.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f) // expand width
                .fillMaxHeight(0.9f) // expand height
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Text(
                        text = updateDisplayText(),
                        textAlign = TextAlign.End,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(Modifier.height(14.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    val buttons = listOf(
                        listOf("C", "⌫", "%", "/"),
                        listOf("7", "8", "9", "*"),
                        listOf("4", "5", "6", "-"),
                        listOf("1", "2", "3", "+"),
                        listOf("+/-", "0", ".", "="),
                        listOf("(", ")", "x²", "√")
                    )

                    buttons.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { label ->
                                val isWide = label in listOf("C", "⌫", "+/-", "x²")
                                Button(
                                    onClick = {
                                        when (label) {
                                            in "0".."9" -> handleNumber(label)
                                            "+", "-", "*", "/" -> handleOperator(label)
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
                                    modifier = Modifier
                                        .weight(if (isWide) 1.3f else 1f) // wider buttons
                                        .padding(vertical = 4.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = when (label) {
                                            "=", "C" -> MaterialTheme.colorScheme.primary
                                            "+", "-", "*", "/", "x²", "√", "%" -> MaterialTheme.colorScheme.secondaryContainer
                                            else -> MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = if (label in listOf("+/-", "x²")) 16.sp else 20.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp, brush = SolidColor(
                        Color(0xFFB71C1C)
                    )
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFB71C1C)
                    )
                ) {
                    Text("Close", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

            }
        }
    }
}

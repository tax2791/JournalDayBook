@file:OptIn(ExperimentalFoundationApi::class)

package database

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EntityTable(viewModel: MealViewModel) {
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()   // Keeps UI below camera & status bar
            .padding(8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp)
        ) {
            MemberTable(members = allMembers)
        }
    }
}

@Composable
fun MemberTable(members: List<Member>) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val horizontalScrollState = rememberScrollState()

    val headerModifier = Modifier
        .background(
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        )
        .border(BorderStroke(2.dp, Color.Black))

    // Enabled horizontal scrolling to comfortably handle multiple columns
    Box(
        modifier = Modifier
            .fillMaxSize()
            .horizontalScroll(horizontalScrollState)
    ) {
        LazyColumn(
            modifier = Modifier
                .border(1.dp, Color.Gray)
        ) {
            // Header row
            stickyHeader {
                Row(
                    modifier = headerModifier
                        .height(IntrinsicSize.Min)
                        .padding(vertical = 6.dp)
                ) {
                    TableCell("ID", 0.6f, Color.White, true)
                    TableCell("Name", 1.5f, Color.White, true)
                    TableCell("Type", 1.3f, Color.White, true)
                    TableCell("Account Name", 1.5f, Color.White, true)
                    TableCell("Sub-Type", 1.2f, Color.White, true)
                    TableCell("Balance (₹)", 1.2f, Color.White, true)
                    TableCell("Join Date", 1.3f, Color.White, true)
                    TableCell("Created Date", 1.3f, Color.White, true)
                    TableCell("Exit Date", 1.3f, Color.White, true)
                }
            }

            // Data rows
            itemsIndexed(members) { index, member ->
                val backgroundColor =
                    if (index % 2 == 0) Color(0xFFE3F2FD) else Color(0xFFF1F8E9) // Alternate row colors

                val createdDateStr = member.createdDate.let {
                    dateFormat.format(Date(it))
                } ?: "N/A"

                Row(
                    modifier = Modifier
                        .background(backgroundColor)
                        .height(IntrinsicSize.Min)
                        .border(BorderStroke(1.dp, Color.Black))
                        .padding(vertical = 4.dp)
                ) {
                    TableCell(member.id.toString(), 0.6f)
                    TableCell(member.name, 1.5f)
                    TableCell(member.type, 1.3f)
                    TableCell(member.accountName ?: "N/A", 1.5f)
                    TableCell(member.accountType ?: "N/A", 1.2f)
                    TableCell(member.openingBalance.toString(), 1.2f)
                    TableCell(member.joinDate?.let { dateFormat.format(it) } ?: "N/A", 1.3f)
                    TableCell(createdDateStr, 1.3f)
                    TableCell(member.exitDate?.let { dateFormat.format(it) } ?: "N/A", 1.3f)
                }
            }
        }
    }
}

@Composable
fun RowScope.TableCell(
    text: String,
    weight: Float = 1f,
    textColor: Color = Color.Black,
    isHeader: Boolean = false
) {
    Text(
        text = text,
        color = textColor,
        textAlign = TextAlign.Center,
        fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .weight(weight)
            .border(1.dp, Color.Black)
            .padding(8.dp)
    )
}
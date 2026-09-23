@file:OptIn(ExperimentalFoundationApi::class)

package database

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.kushal.mealapp.database.MealViewModel
import com.kushal.mealapp.database.Member
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun EntityTable(viewModel: MealViewModel) {
    val allMembers by viewModel.allMembers.collectAsState(initial = emptyList())

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()   // ✅ Keeps UI below camera & status bar
            .padding(8.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        // ✅ Added horizontal scroll for wide tables
      //  val horizontalScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxSize()
                //.horizontalScroll(horizontalScrollState)
                .padding(bottom = 16.dp)
        ) {
            MemberTable(members = allMembers)
        }
    }
}

@Composable
fun MemberTable(members: List<Member>) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val headerModifier = Modifier
        .fillMaxWidth()
        .background(
            Brush.horizontalGradient(
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary
                )
            )
        )
        .border(BorderStroke(2.dp, Color.Black))

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.Gray)
    ) {
        // Header row
        stickyHeader {
            Row(
                modifier = headerModifier
                    .height(IntrinsicSize.Min)
                    .padding(vertical = 6.dp)
            ) {
                TableCell("ID", 1f, Color.White, true)
                TableCell("Name", 2f, Color.White, true)
                TableCell("Join Date", 2f, Color.White, true)
                TableCell("Created Date", 2f, Color.White, true)
                TableCell("Exit Date", 2f, Color.White, true)
            }
        }

        // Data rows
        itemsIndexed(members) { index, member ->
            val backgroundColor =
                if (index % 2 == 0) Color(0xFFE3F2FD) else Color(0xFFF1F8E9) // Alternate row colors

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .height(IntrinsicSize.Min)
                    .border(BorderStroke(1.dp, Color.Black))
                    .padding(vertical = 4.dp)
            ) {
                TableCell(member.id.toString(), 1f)
                TableCell(member.name, 2f)
                TableCell(member.joinDate?.let { dateFormat.format(it) } ?: "N/A", 2f)
                TableCell(member.createdDate?.let { dateFormat.format(it) } ?: "N/A", 2f)
                TableCell(member.exitDate?.let { dateFormat.format(it) } ?: "N/A", 2f)
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

package database

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MemberReportExporter {

    private const val TAG = "MemberReportExporter"

    /** 📊 Export Member Report & Related Transactions to Excel (.csv) */
    fun exportToExcelCsv(
        context: Context,
        reportDataList: List<MemberReportData>,
        relatedTransactions: List<Meal1> = emptyList(),
        fromDateStr: String,
        toDateStr: String
    ): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Member_Report_$timeStamp.csv"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(downloadsDir, fileName)

            val csvContent = StringBuilder()
            // Header Info
            csvContent.append("Member Financial Account Report\n")
            csvContent.append("Filter Period,\"$fromDateStr to $toDateStr\"\n")
            csvContent.append("Generated On,\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\"\n\n")

            // Section 1: Member Summary Table
            csvContent.append("Member Summary\n")
            csvContent.append("Name,Opening Balance,Transfer In,Expenditure,Transfer Out,Closing Balance\n")

            var totalOp = 0.0
            var totalTrIn = 0.0
            var totalExp = 0.0
            var totalTrOut = 0.0
            var totalCl = 0.0

            reportDataList.forEach { data ->
                val name = "\"${data.member.name.replace("\"", "\"\"")}\""
                csvContent.append(
                    "$name,${formatVal(data.openingBalance)},${formatVal(data.transferIn)},${formatVal(data.expenditure)},${formatVal(data.transferOut)},${formatVal(data.closingBalance)}\n"
                )
                totalOp += data.openingBalance
                totalTrIn += data.transferIn
                totalExp += data.expenditure
                totalTrOut += data.transferOut
                totalCl += data.closingBalance
            }

            // Summary Total Row
            csvContent.append(
                "TOTAL,${formatVal(totalOp)},${formatVal(totalTrIn)},${formatVal(totalExp)},${formatVal(totalTrOut)},${formatVal(totalCl)}\n\n\n"
            )

            // Section 2: Detailed Transaction Log
            if (relatedTransactions.isNotEmpty()) {
                csvContent.append("Detailed Transaction Log\n")
                csvContent.append("S.No,Date,Name,Item,Remark,Amount (Rs.)\n")

                var totalTx = 0.0
                relatedTransactions.forEachIndexed { index, meal ->
                    val name = "\"${meal.name.replace("\"", "\"\"")}\""
                    val item = "\"${meal.item.replace("\"", "\"\"")}\""
                    val remark = "\"${meal.expenditure.replace("\"", "\"\"")}\""
                    csvContent.append("${index + 1},${formatDate(meal.date)},$name,$item,$remark,${formatVal(meal.price)}\n")
                    totalTx += meal.price
                }

                csvContent.append("TOTAL,,,,,${formatVal(totalTx)}\n")
            }

            FileOutputStream(file).use { out ->
                out.write(csvContent.toString().toByteArray(Charsets.UTF_8))
            }

            Log.d(TAG, "CSV exported successfully: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export CSV", e)
            null
        }
    }

    /** 📄 Export Member Report & Related Transactions to PDF document */
    fun exportToPdf(
        context: Context,
        reportDataList: List<MemberReportData>,
        relatedTransactions: List<Meal1> = emptyList(),
        fromDateStr: String,
        toDateStr: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            // Paints
            val titlePaint = Paint().apply {
                color = Color.rgb(26, 35, 126) // Dark Indigo
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subTitlePaint = Paint().apply {
                color = Color.rgb(66, 66, 66)
                textSize = 10f
                isAntiAlias = true
            }

            val sectionHeaderPaint = Paint().apply {
                color = Color.rgb(0, 77, 64) // Dark Teal
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val headerBgPaint1 = Paint().apply {
                color = Color.rgb(63, 81, 181) // Indigo
                style = Paint.Style.FILL
            }

            val headerBgPaint2 = Paint().apply {
                color = Color.rgb(0, 137, 123) // Teal
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val rowBgEven = Paint().apply {
                color = Color.rgb(245, 245, 245)
                style = Paint.Style.FILL
            }

            val rowBgOdd = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

            val totalBgPaint = Paint().apply {
                color = Color.rgb(232, 234, 246)
                style = Paint.Style.FILL
            }

            val textPaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 9f
                isAntiAlias = true
            }

            val textBoldPaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val borderPaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }

            val greenTextPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val redTextPaint = Paint().apply {
                color = Color.rgb(198, 40, 40)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            // Page dimensions and bounds
            val marginLeft = 36f
            val marginRight = 559f
            val rowHeight = 22f
            val bottomMargin = 800f

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // ================= SECTION 1: MEMBER FINANCIAL ACCOUNT REPORT =================
            val colWidths1 = floatArrayOf(128f, 75f, 80f, 80f, 80f, 80f)
            val colHeaders1 = arrayOf("Name", "Op. Bal", "Tr. In", "Exp.", "Tr. Out", "Cl. Bal")

            fun drawHeader1(c: Canvas) {
                c.drawText("MEMBER FINANCIAL ACCOUNT REPORT", marginLeft, 45f, titlePaint)
                c.drawText("Filter Period: $fromDateStr to $toDateStr", marginLeft, 62f, subTitlePaint)
                c.drawText("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())}", marginLeft, 75f, subTitlePaint)
                c.drawLine(marginLeft, 85f, marginRight, 85f, borderPaint)
            }

            fun drawTableHeader1(c: Canvas, startY: Float) {
                val headerRect = RectF(marginLeft, startY, marginRight, startY + 24f)
                c.drawRect(headerRect, headerBgPaint1)
                var currentX = marginLeft
                for (i in colHeaders1.indices) {
                    c.drawText(colHeaders1[i], currentX + 4f, startY + 16f, headerTextPaint)
                    currentX += colWidths1[i]
                }
            }

            drawHeader1(canvas)
            var currentY = 95f
            drawTableHeader1(canvas, currentY)
            currentY += 24f

            var totalOp = 0.0
            var totalTrIn = 0.0
            var totalExp = 0.0
            var totalTrOut = 0.0
            var totalCl = 0.0

            reportDataList.forEachIndexed { index, data ->
                totalOp += data.openingBalance
                totalTrIn += data.transferIn
                totalExp += data.expenditure
                totalTrOut += data.transferOut
                totalCl += data.closingBalance

                if (currentY + rowHeight > bottomMargin) {
                    canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawHeader1(canvas)
                    currentY = 95f
                    drawTableHeader1(canvas, currentY)
                    currentY += 24f
                }

                val rowBg = if (index % 2 == 0) rowBgEven else rowBgOdd
                val rowRect = RectF(marginLeft, currentY, marginRight, currentY + rowHeight)
                canvas.drawRect(rowRect, rowBg)
                canvas.drawRect(rowRect, borderPaint)

                val values = arrayOf(
                    truncateName(data.member.name, 18),
                    formatVal(data.openingBalance),
                    formatVal(data.transferIn),
                    formatVal(data.expenditure),
                    formatVal(data.transferOut),
                    formatVal(data.closingBalance)
                )

                var currentX = marginLeft
                for (i in values.indices) {
                    val p = when (i) {
                        0 -> textBoldPaint
                        5 -> if (data.closingBalance >= 0) greenTextPaint else redTextPaint
                        else -> textPaint
                    }
                    canvas.drawText(values[i], currentX + 4f, currentY + 15f, p)
                    currentX += colWidths1[i]
                }

                currentY += rowHeight
            }

            // Summary Totals Row for Section 1
            if (currentY + rowHeight > bottomMargin) {
                canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawHeader1(canvas)
                currentY = 95f
                drawTableHeader1(canvas, currentY)
                currentY += 24f
            }

            val totalRect1 = RectF(marginLeft, currentY, marginRight, currentY + rowHeight + 2f)
            canvas.drawRect(totalRect1, totalBgPaint)
            canvas.drawRect(totalRect1, borderPaint)

            val totalValues1 = arrayOf(
                "TOTAL",
                formatVal(totalOp),
                formatVal(totalTrIn),
                formatVal(totalExp),
                formatVal(totalTrOut),
                formatVal(totalCl)
            )

            var currentX1 = marginLeft
            for (i in totalValues1.indices) {
                val p = if (i == 5) (if (totalCl >= 0) greenTextPaint else redTextPaint) else textBoldPaint
                canvas.drawText(totalValues1[i], currentX1 + 4f, currentY + 16f, p)
                currentX1 += colWidths1[i]
            }

            canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)

            // ================= SECTION 2: DETAILED TRANSACTION LOG (ON NEXT PAGE) =================
            if (relatedTransactions.isNotEmpty()) {
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                val colWidths2 = floatArrayOf(35f, 70f, 120f, 90f, 120f, 88f)
                val colHeaders2 = arrayOf("S.No", "Date", "Name", "Item", "Remark", "Amount")

                fun drawHeader2(c: Canvas) {
                    c.drawText("DETAILED TRANSACTION LOG", marginLeft, 45f, sectionHeaderPaint)
                    c.drawText("Individual transactions for period $fromDateStr to $toDateStr", marginLeft, 62f, subTitlePaint)
                    c.drawText("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())}", marginLeft, 75f, subTitlePaint)
                    c.drawLine(marginLeft, 85f, marginRight, 85f, borderPaint)
                }

                fun drawTableHeader2(c: Canvas, startY: Float) {
                    val headerRect = RectF(marginLeft, startY, marginRight, startY + 24f)
                    c.drawRect(headerRect, headerBgPaint2)
                    var cx = marginLeft
                    for (i in colHeaders2.indices) {
                        c.drawText(colHeaders2[i], cx + 4f, startY + 16f, headerTextPaint)
                        cx += colWidths2[i]
                    }
                }

                drawHeader2(canvas)
                currentY = 95f
                drawTableHeader2(canvas, currentY)
                currentY += 24f

                var totalTxAmount = 0.0

                relatedTransactions.forEachIndexed { index, meal ->
                    totalTxAmount += meal.price

                    if (currentY + rowHeight > bottomMargin) {
                        canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                        pdfDocument.finishPage(page)

                        pageNum++
                        pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                        page = pdfDocument.startPage(pageInfo)
                        canvas = page.canvas

                        drawHeader2(canvas)
                        currentY = 95f
                        drawTableHeader2(canvas, currentY)
                        currentY += 24f
                    }

                    val rowBg = if (index % 2 == 0) rowBgEven else rowBgOdd
                    val rowRect = RectF(marginLeft, currentY, marginRight, currentY + rowHeight)
                    canvas.drawRect(rowRect, rowBg)
                    canvas.drawRect(rowRect, borderPaint)

                    val values2 = arrayOf(
                        (index + 1).toString(),
                        formatDate(meal.date),
                        truncateName(meal.name, 18),
                        truncateName(meal.item, 14),
                        truncateName(meal.expenditure, 18),
                        formatVal(meal.price)
                    )

                    var cx = marginLeft
                    for (i in values2.indices) {
                        val p = if (i == 5) textBoldPaint else textPaint
                        canvas.drawText(values2[i], cx + 4f, currentY + 15f, p)
                        cx += colWidths2[i]
                    }

                    currentY += rowHeight
                }

                // Total Summary Row for Section 2
                if (currentY + rowHeight > bottomMargin) {
                    canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawHeader2(canvas)
                    currentY = 95f
                    drawTableHeader2(canvas, currentY)
                    currentY += 24f
                }

                val totalRect2 = RectF(marginLeft, currentY, marginRight, currentY + rowHeight + 2f)
                canvas.drawRect(totalRect2, totalBgPaint)
                canvas.drawRect(totalRect2, borderPaint)

                val totalValues2 = arrayOf("TOTAL", "", "", "", "", formatVal(totalTxAmount))
                var cx2 = marginLeft
                for (i in totalValues2.indices) {
                    canvas.drawText(totalValues2[i], cx2 + 4f, currentY + 16f, textBoldPaint)
                    cx2 += colWidths2[i]
                }

                canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
            }

            pdfDocument.finishPage(page)

            // Save PDF to file
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Member_Report_$timeStamp.pdf"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Log.d(TAG, "PDF exported successfully: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF", e)
            null
        }
    }

    /** 📤 Share or open exported file via Intent */
    fun shareOrOpenFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, file)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, chooserTitle)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing file", e)
            Toast.makeText(context, "Error opening file chooser: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatVal(value: Double): String {
        return String.format(Locale.US, "%.2f", value)
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
    }

    private fun truncateName(name: String, maxLength: Int): String {
        return if (name.length > maxLength) name.substring(0, maxLength - 1) + "…" else name
    }

    /** 📊 Export Summary Table (Transactions) to Excel (.csv) */
    fun exportSummaryToExcelCsv(
        context: Context,
        filteredMeals: List<Meal1>,
        fromDate: String,
        toDate: String,
        selectedName: String,
        selectedItem: String
    ): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Transaction_Summary_$timeStamp.csv"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(downloadsDir, fileName)

            val csvContent = StringBuilder()
            csvContent.append("Transaction Summary Report\n")
            val periodStr = if (fromDate.isEmpty() && toDate.isEmpty()) "All Time" else "${fromDate.ifEmpty { "Start" }} to ${toDate.ifEmpty { "End" }}"
            csvContent.append("Period,\"$periodStr\"\n")
            csvContent.append("Name Filter,\"$selectedName\"\n")
            csvContent.append("Item Filter,\"$selectedItem\"\n")
            csvContent.append("Generated On,\"${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\"\n\n")

            csvContent.append("S.No,Date,Name,Item,Remark,Amount (Rs.)\n")

            var totalAmount = 0.0
            filteredMeals.forEachIndexed { index, meal ->
                val name = "\"${meal.name.replace("\"", "\"\"")}\""
                val item = "\"${meal.item.replace("\"", "\"\"")}\""
                val remark = "\"${meal.expenditure.replace("\"", "\"\"")}\""
                val dateStr = formatDate(meal.date)
                csvContent.append("${index + 1},$dateStr,$name,$item,$remark,${formatVal(meal.price)}\n")
                totalAmount += meal.price
            }

            csvContent.append("TOTAL,,,,,${formatVal(totalAmount)}\n")

            FileOutputStream(file).use { out ->
                out.write(csvContent.toString().toByteArray(Charsets.UTF_8))
            }

            Log.d(TAG, "Transaction Summary CSV exported successfully: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export Transaction Summary CSV", e)
            null
        }
    }

    /** 📄 Export Summary Table (Transactions) to PDF document */
    fun exportSummaryToPdf(
        context: Context,
        filteredMeals: List<Meal1>,
        fromDate: String,
        toDate: String,
        selectedName: String,
        selectedItem: String
    ): File? {
        return try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842

            val titlePaint = Paint().apply {
                color = Color.rgb(0, 77, 64)
                textSize = 18f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subTitlePaint = Paint().apply {
                color = Color.rgb(66, 66, 66)
                textSize = 10f
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(0, 137, 123)
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val rowBgEven = Paint().apply {
                color = Color.rgb(240, 247, 244)
                style = Paint.Style.FILL
            }

            val rowBgOdd = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }

            val totalBgPaint = Paint().apply {
                color = Color.rgb(232, 245, 233)
                style = Paint.Style.FILL
            }

            val textPaint = Paint().apply {
                color = Color.rgb(33, 33, 33)
                textSize = 9f
                isAntiAlias = true
            }

            val textBoldPaint = Paint().apply {
                color = Color.rgb(0, 77, 64)
                textSize = 9f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val borderPaint = Paint().apply {
                color = Color.rgb(200, 200, 200)
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }

            val marginLeft = 36f
            val marginRight = 559f
            val rowHeight = 22f

            val colWidths = floatArrayOf(35f, 70f, 120f, 90f, 120f, 88f)
            val colHeaders = arrayOf("S.No", "Date", "Name", "Item", "Remark", "Amount")

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            val periodStr = if (fromDate.isEmpty() && toDate.isEmpty()) "All Time" else "${fromDate.ifEmpty { "Start" }} to ${toDate.ifEmpty { "End" }}"

            fun drawPageHeader(c: Canvas) {
                c.drawText("TRANSACTION SUMMARY REPORT", marginLeft, 45f, titlePaint)
                c.drawText("Period: $periodStr | Name: $selectedName | Item: $selectedItem", marginLeft, 62f, subTitlePaint)
                c.drawText("Generated: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(Date())}", marginLeft, 75f, subTitlePaint)
                c.drawLine(marginLeft, 85f, marginRight, 85f, borderPaint)
            }

            fun drawTableHeader(c: Canvas, startY: Float) {
                val headerRect = RectF(marginLeft, startY, marginRight, startY + 24f)
                c.drawRect(headerRect, headerBgPaint)

                var currentX = marginLeft
                for (i in colHeaders.indices) {
                    val w = colWidths[i]
                    c.drawText(colHeaders[i], currentX + 4f, startY + 16f, headerTextPaint)
                    currentX += w
                }
            }

            drawPageHeader(canvas)
            var currentY = 95f
            drawTableHeader(canvas, currentY)
            currentY += 24f

            val bottomMargin = 800f
            var totalAmount = 0.0

            filteredMeals.forEachIndexed { index, meal ->
                totalAmount += meal.price

                if (currentY + rowHeight > bottomMargin) {
                    canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawPageHeader(canvas)
                    currentY = 95f
                    drawTableHeader(canvas, currentY)
                    currentY += 24f
                }

                val rowBg = if (index % 2 == 0) rowBgEven else rowBgOdd
                val rowRect = RectF(marginLeft, currentY, marginRight, currentY + rowHeight)
                canvas.drawRect(rowRect, rowBg)
                canvas.drawRect(rowRect, borderPaint)

                val values = arrayOf(
                    (index + 1).toString(),
                    formatDate(meal.date),
                    truncateName(meal.name, 18),
                    truncateName(meal.item, 14),
                    truncateName(meal.expenditure, 18),
                    formatVal(meal.price)
                )

                var currentX = marginLeft
                for (i in values.indices) {
                    val w = colWidths[i]
                    val p = if (i == 5) textBoldPaint else textPaint
                    canvas.drawText(values[i], currentX + 4f, currentY + 15f, p)
                    currentX += w
                }

                currentY += rowHeight
            }

            if (currentY + rowHeight > bottomMargin) {
                canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawPageHeader(canvas)
                currentY = 95f
                drawTableHeader(canvas, currentY)
                currentY += 24f
            }

            val totalRect = RectF(marginLeft, currentY, marginRight, currentY + rowHeight + 2f)
            canvas.drawRect(totalRect, totalBgPaint)
            canvas.drawRect(totalRect, borderPaint)

            val totalValues = arrayOf("TOTAL", "", "", "", "", formatVal(totalAmount))
            var currentX = marginLeft
            for (i in totalValues.indices) {
                val w = colWidths[i]
                canvas.drawText(totalValues[i], currentX + 4f, currentY + 16f, textBoldPaint)
                currentX += w
            }

            canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
            pdfDocument.finishPage(page)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Transaction_Summary_$timeStamp.pdf"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            Log.d(TAG, "Transaction Summary PDF exported successfully: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export Transaction Summary PDF", e)
            null
        }
    }
}

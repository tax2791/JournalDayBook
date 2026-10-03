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

object LoanPdfExporter {

    private const val TAG = "LoanPdfExporter"

    /** 📄 Export Loan Accounts & All Loan Transactions to PDF */
    fun exportLoanReportToPdf(
        context: Context,
        loans: List<LoanRecord>,
        repayments: List<LoanRepayment>
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width in points
            val pageHeight = 842 // A4 height in points

            // Paints
            val titlePaint = Paint().apply {
                color = Color.rgb(21, 101, 192) // Deep Blue
                textSize = 16f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subTitlePaint = Paint().apply {
                color = Color.rgb(97, 97, 97)
                textSize = 9f
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(25, 118, 210) // Primary Blue Header
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

            val marginLeft = 36f
            val marginRight = 559f
            val rowHeight = 22f
            val bottomMargin = 800f

            var pageNum = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            fun drawDocumentHeader(c: Canvas) {
                c.drawText("LOAN ACCOUNT & COLLECTIONS REPORT", marginLeft, 45f, titlePaint)
                c.drawText("Generated On: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).format(Date())}", marginLeft, 62f, subTitlePaint)
                c.drawText("Meal App Journal DayBook • Offline Loan Ledger", marginLeft, 75f, subTitlePaint)
                c.drawLine(marginLeft, 85f, marginRight, 85f, borderPaint)
            }

            drawDocumentHeader(canvas)
            var currentY = 95f

            // ================= SECTION 1: PARTY LOAN BALANCES SUMMARY =================
            val colWidths1 = floatArrayOf(120f, 90f, 100f, 100f, 113f)
            val colHeaders1 = arrayOf("Party Name", "Phone", "Loan Given (Rs)", "Received (Rs)", "Balance Due (Rs)")

            fun drawTableHeader1(c: Canvas, startY: Float) {
                val headerRect = RectF(marginLeft, startY, marginRight, startY + 24f)
                c.drawRect(headerRect, headerBgPaint)
                var currentX = marginLeft
                for (i in colHeaders1.indices) {
                    c.drawText(colHeaders1[i], currentX + 4f, startY + 16f, headerTextPaint)
                    currentX += colWidths1[i]
                }
            }

            drawTableHeader1(canvas, currentY)
            currentY += 24f

            // Group summary
            val partyNames = (loans.map { it.partyName } + repayments.map { it.partyName }).distinct()
            var totalGivenSum = 0.0
            var totalReceivedSum = 0.0

            partyNames.forEachIndexed { index, partyName ->
                val partyLoans = loans.filter { it.partyName.equals(partyName, ignoreCase = true) }
                val partyRepayments = repayments.filter { it.partyName.equals(partyName, ignoreCase = true) }
                val totalGiven = partyLoans.sumOf { it.loanAmount }
                val totalRec = partyRepayments.sumOf { it.amountReceived }
                val balance = totalGiven - totalRec
                val phone = partyLoans.firstOrNull { it.phone.isNotBlank() }?.phone ?: "-"

                totalGivenSum += totalGiven
                totalReceivedSum += totalRec

                if (currentY + rowHeight > bottomMargin) {
                    canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawDocumentHeader(canvas)
                    currentY = 95f
                    drawTableHeader1(canvas, currentY)
                    currentY += 24f
                }

                val rowBg = if (index % 2 == 0) rowBgEven else rowBgOdd
                val rowRect = RectF(marginLeft, currentY, marginRight, currentY + rowHeight)
                canvas.drawRect(rowRect, rowBg)
                canvas.drawRect(rowRect, borderPaint)

                val values = arrayOf(
                    truncateString(partyName, 18),
                    truncateString(phone, 12),
                    String.format(Locale.US, "%.2f", totalGiven),
                    String.format(Locale.US, "%.2f", totalRec),
                    String.format(Locale.US, "%.2f", balance)
                )

                var currentX = marginLeft
                for (i in values.indices) {
                    val p = when (i) {
                        0 -> textBoldPaint
                        4 -> if (balance <= 0) greenTextPaint else redTextPaint
                        else -> textPaint
                    }
                    canvas.drawText(values[i], currentX + 4f, currentY + 15f, p)
                    currentX += colWidths1[i]
                }

                currentY += rowHeight
            }

            // Summary Totals Row
            val totalBalanceSum = totalGivenSum - totalReceivedSum
            val totalRect1 = RectF(marginLeft, currentY, marginRight, currentY + 24f)
            canvas.drawRect(totalRect1, totalBgPaint)
            canvas.drawRect(totalRect1, borderPaint)

            canvas.drawText("TOTAL OUTSTANDING", marginLeft + 4f, currentY + 16f, textBoldPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", totalGivenSum), marginLeft + colWidths1[0] + colWidths1[1] + 4f, currentY + 16f, textBoldPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", totalReceivedSum), marginLeft + colWidths1[0] + colWidths1[1] + colWidths1[2] + 4f, currentY + 16f, greenTextPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", totalBalanceSum), marginLeft + colWidths1[0] + colWidths1[1] + colWidths1[2] + colWidths1[3] + 4f, currentY + 16f, redTextPaint)

            currentY += 40f

            // ================= SECTION 2: DETAILED LOAN & REPAYMENT TRANSACTIONS =================
            val colWidths2 = floatArrayOf(80f, 110f, 110f, 90f, 133f)
            val colHeaders2 = arrayOf("Date", "Party Name", "Type", "Amount (Rs)", "Mode / Remarks")

            fun drawTableHeader2(c: Canvas, startY: Float) {
                c.drawText("DETAILED TRANSACTIONS LEDGER", marginLeft, startY - 8f, titlePaint)
                val headerRect = RectF(marginLeft, startY, marginRight, startY + 24f)
                c.drawRect(headerRect, headerBgPaint)
                var currentX = marginLeft
                for (i in colHeaders2.indices) {
                    c.drawText(colHeaders2[i], currentX + 4f, startY + 16f, headerTextPaint)
                    currentX += colWidths2[i]
                }
            }

            if (currentY + 60f > bottomMargin) {
                canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                pdfDocument.finishPage(page)

                pageNum++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                drawDocumentHeader(canvas)
                currentY = 95f
            }

            drawTableHeader2(canvas, currentY)
            currentY += 24f

            data class CombinedTx(
                val date: String,
                val partyName: String,
                val type: String,
                val amount: Double,
                val remarks: String
            )

            val combinedList = mutableListOf<CombinedTx>()
            loans.forEach {
                combinedList.add(CombinedTx(it.loanDate, it.partyName, "LOAN GIVEN", it.loanAmount, it.remarks))
            }
            repayments.forEach {
                combinedList.add(CombinedTx(it.paymentDate, it.partyName, "REPAYMENT", it.amountReceived, "${it.paymentMode} ${it.remarks}"))
            }

            combinedList.sortByDescending { it.date }

            combinedList.forEachIndexed { index, tx ->
                if (currentY + rowHeight > bottomMargin) {
                    canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
                    pdfDocument.finishPage(page)

                    pageNum++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawDocumentHeader(canvas)
                    currentY = 95f
                    drawTableHeader2(canvas, currentY)
                    currentY += 24f
                }

                val rowBg = if (index % 2 == 0) rowBgEven else rowBgOdd
                val rowRect = RectF(marginLeft, currentY, marginRight, currentY + rowHeight)
                canvas.drawRect(rowRect, rowBg)
                canvas.drawRect(rowRect, borderPaint)

                val values = arrayOf(
                    tx.date,
                    truncateString(tx.partyName, 16),
                    tx.type,
                    String.format(Locale.US, "%.2f", tx.amount),
                    truncateString(tx.remarks, 20)
                )

                var currentX = marginLeft
                for (i in values.indices) {
                    val p = when (i) {
                        2 -> if (tx.type == "REPAYMENT") greenTextPaint else redTextPaint
                        3 -> textBoldPaint
                        else -> textPaint
                    }
                    canvas.drawText(values[i], currentX + 4f, currentY + 15f, p)
                    currentX += colWidths2[i]
                }

                currentY += rowHeight
            }

            canvas.drawText("Page $pageNum", marginRight - 40f, 825f, subTitlePaint)
            pdfDocument.finishPage(page)

            // Save PDF File
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "Loan_Account_Report_$timeStamp.pdf"
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
            val pdfFile = File(downloadsDir, fileName)

            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            Log.d(TAG, "PDF generated successfully: ${pdfFile.absolutePath}")
            Toast.makeText(context, "PDF Report Exported: ${pdfFile.name} 📄", Toast.LENGTH_LONG).show()

            // Open or Share PDF
            sharePdfFile(context, pdfFile)

        } catch (e: Exception) {
            Log.e(TAG, "Error exporting loan PDF: ${e.message}", e)
            Toast.makeText(context, "Failed to export PDF: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdfFile(context: Context, pdfFile: File) {
        try {
            val authority = "${context.packageName}.provider"
            val uri: Uri = FileProvider.getUriForFile(context, authority, pdfFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Loan Account Report")
                putExtra(Intent.EXTRA_TEXT, "Attached is the Loan Account & Collections PDF Report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Open / Share Loan PDF Report")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing PDF: ${e.message}", e)
            Toast.makeText(context, "PDF created at: ${pdfFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    }

    private fun truncateString(str: String, maxLength: Int): String {
        return if (str.length <= maxLength) str else str.substring(0, maxLength - 2) + ".."
    }
}

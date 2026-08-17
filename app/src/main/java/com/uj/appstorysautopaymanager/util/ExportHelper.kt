package com.uj.appstorysautopaymanager.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.uj.appstorysautopaymanager.data.local.entity.Bill
import com.uj.appstorysautopaymanager.data.local.entity.Transaction
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportHelper {

    fun exportToCsv(bills: List<Bill>, transactions: List<Transaction>, outputStream: OutputStream) {
        val writer = outputStream.bufferedWriter()
        writer.write("TYPE,TITLE/MERCHANT,AMOUNT,DATE,CATEGORY,STATUS/TYPE,BANK/REF\n")
        
        for (bill in bills) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(bill.dueDate))
            writer.write("BILL,\"${bill.title}\",${bill.amount},$dateStr,\"${bill.category}\",\"${bill.status}\",\"${bill.repeatType}\"\n")
        }
        
        for (txn in transactions) {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(txn.date))
            writer.write("TRANSACTION,\"${txn.merchant}\",${txn.amount},$dateStr,\"${txn.category}\",\"${txn.transactionType}\",\"${txn.bankName} - ${txn.accountNumber}\"\n")
        }
        writer.flush()
    }

    fun exportToJsonBackup(bills: List<Bill>, transactions: List<Transaction>): String {
        val root = JSONObject()
        
        val billsArray = JSONArray()
        for (bill in bills) {
            val obj = JSONObject().apply {
                put("title", bill.title)
                put("amount", bill.amount)
                put("dueDate", bill.dueDate)
                put("repeatType", bill.repeatType)
                put("reminderDays", bill.reminderDays)
                put("notes", bill.notes)
                put("category", bill.category)
                put("status", bill.status)
                put("isArchived", bill.isArchived)
            }
            billsArray.put(obj)
        }
        root.put("bills", billsArray)
        
        val txnArray = JSONArray()
        for (txn in transactions) {
            val obj = JSONObject().apply {
                put("smsId", txn.smsId)
                put("merchant", txn.merchant)
                put("amount", txn.amount)
                put("date", txn.date)
                put("bankName", txn.bankName)
                put("accountNumber", txn.accountNumber)
                put("referenceNumber", txn.referenceNumber)
                put("transactionType", txn.transactionType)
                put("category", txn.category)
                put("smsBody", txn.smsBody)
                put("isAutoPay", txn.isAutoPay)
            }
            txnArray.put(obj)
        }
        root.put("transactions", txnArray)
        
        return root.toString(4)
    }

    fun importFromJsonBackup(jsonString: String): BackupData {
        val root = JSONObject(jsonString)
        
        val bills = mutableListOf<Bill>()
        val billsArray = root.optJSONArray("bills")
        if (billsArray != null) {
            for (i in 0 until billsArray.length()) {
                val obj = billsArray.getJSONObject(i)
                bills.add(
                    Bill(
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        dueDate = obj.getLong("dueDate"),
                        repeatType = obj.getString("repeatType"),
                        reminderDays = obj.getInt("reminderDays"),
                        notes = obj.optString("notes", ""),
                        category = obj.getString("category"),
                        status = obj.getString("status"),
                        isArchived = obj.optBoolean("isArchived", false)
                    )
                )
            }
        }
        
        val transactions = mutableListOf<Transaction>()
        val txnArray = root.optJSONArray("transactions")
        if (txnArray != null) {
            for (i in 0 until txnArray.length()) {
                val obj = txnArray.getJSONObject(i)
                transactions.add(
                    Transaction(
                        smsId = obj.getString("smsId"),
                        merchant = obj.getString("merchant"),
                        amount = obj.getDouble("amount"),
                        date = obj.getLong("date"),
                        bankName = obj.getString("bankName"),
                        accountNumber = obj.getString("accountNumber"),
                        referenceNumber = obj.optString("referenceNumber", ""),
                        transactionType = obj.getString("transactionType"),
                        category = obj.getString("category"),
                        smsBody = obj.getString("smsBody"),
                        isAutoPay = obj.optBoolean("isAutoPay", false)
                    )
                )
            }
        }
        
        return BackupData(bills, transactions)
    }

    fun exportToPdf(bills: List<Bill>, transactions: List<Transaction>, outputStream: OutputStream) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        
        val canvas: Canvas = page.canvas
        val paint = Paint()
        
        paint.color = Color.BLACK
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("AutoPay Manager - Financial Report", 40f, 50f, paint)
        
        paint.textSize = 12f
        paint.isFakeBoldText = false
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generated on: $dateStr", 40f, 75f, paint)
        
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Upcoming & Active Bills", 40f, 120f, paint)
        
        paint.textSize = 10f
        paint.isFakeBoldText = false
        var yPos = 145f
        
        paint.color = Color.DKGRAY
        canvas.drawRect(40f, yPos - 12, 555f, yPos + 4, paint)
        paint.color = Color.WHITE
        canvas.drawText("Title", 45f, yPos, paint)
        canvas.drawText("Category", 180f, yPos, paint)
        canvas.drawText("Amount", 320f, yPos, paint)
        canvas.drawText("Due Date", 420f, yPos, paint)
        canvas.drawText("Status", 500f, yPos, paint)
        
        yPos += 20f
        paint.color = Color.BLACK
        for (bill in bills.take(15)) {
            val due = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(bill.dueDate))
            canvas.drawText(bill.title, 45f, yPos, paint)
            canvas.drawText(bill.category, 180f, yPos, paint)
            canvas.drawText("Rs.${bill.amount}", 320f, yPos, paint)
            canvas.drawText(due, 420f, yPos, paint)
            canvas.drawText(bill.status, 500f, yPos, paint)
            yPos += 18f
        }
        
        yPos += 20f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Recent Transactions (SMS)", 40f, yPos, paint)
        
        paint.textSize = 10f
        paint.isFakeBoldText = false
        yPos += 25f
        
        paint.color = Color.DKGRAY
        canvas.drawRect(40f, yPos - 12, 555f, yPos + 4, paint)
        paint.color = Color.WHITE
        canvas.drawText("Merchant", 45f, yPos, paint)
        canvas.drawText("Bank/Acc", 180f, yPos, paint)
        canvas.drawText("Amount", 320f, yPos, paint)
        canvas.drawText("Date", 420f, yPos, paint)
        canvas.drawText("Type", 500f, yPos, paint)
        
        yPos += 20f
        paint.color = Color.BLACK
        for (txn in transactions.take(15)) {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(txn.date))
            canvas.drawText(txn.merchant, 45f, yPos, paint)
            canvas.drawText("${txn.bankName} (${txn.accountNumber})", 180f, yPos, paint)
            canvas.drawText("Rs.${txn.amount}", 320f, yPos, paint)
            canvas.drawText(date, 420f, yPos, paint)
            canvas.drawText(txn.transactionType, 500f, yPos, paint)
            yPos += 18f
        }
        
        document.finishPage(page)
        document.writeTo(outputStream)
        document.close()
    }
}

data class BackupData(
    val bills: List<Bill>,
    val transactions: List<Transaction>
)

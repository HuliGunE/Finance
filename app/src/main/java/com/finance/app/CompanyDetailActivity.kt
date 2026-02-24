package com.finance.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.ContentValues
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.lifecycleScope
import com.finance.app.databinding.ActivityCompanyDetailBinding
import com.google.android.material.datepicker.MaterialDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Locale

class CompanyDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COMPANY_ID = "company_id"
        const val EXTRA_COMPANY_NAME = "company_name"
    }

    private lateinit var binding: ActivityCompanyDetailBinding
    private var companyId: Long = 0
    private lateinit var companyName: String
    private lateinit var db: AppDatabase
    private lateinit var transactionAdapter: TransactionAdapter
    private var allTransactions: List<Transaction> = emptyList()
    private var typeFilter: String? = null // "income", "expense" або null
    private var searchQuery: String = ""
    private var dateFilterStart: Long? = null
    private var dateFilterEnd: Long? = null
    private var currentBalance: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompanyDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        companyId = intent.getLongExtra(EXTRA_COMPANY_ID, 0)
        companyName = intent.getStringExtra(EXTRA_COMPANY_NAME) ?: ""
        db = AppDatabase.getInstance(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = companyName
        binding.toolbar.setNavigationOnClickListener { finish() }

        transactionAdapter = TransactionAdapter { transaction ->
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete))
                .setMessage("Видалити цей запис?")
                .setPositiveButton(getString(R.string.delete)) { _, _ ->
                    lifecycleScope.launch {
                        db.transactionDao().delete(transaction)
                    }
                }
                .setNegativeButton(getString(R.string.cancel), null)
                .show()
        }

        binding.recyclerTransactions.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.recyclerTransactions.adapter = transactionAdapter

        binding.btnAddIncome.setOnClickListener {
            startAddTransaction("income")
        }
        binding.btnAddExpense.setOnClickListener {
            startAddTransaction("expense")
        }

        setupFiltersUi()

        lifecycleScope.launch {
            combine(
                db.transactionDao().getByCompany(companyId),
                db.transactionDao().getTotalIncome(companyId),
                db.transactionDao().getTotalExpense(companyId)
            ) { transactions, income, expense ->
                Triple(transactions, income, expense)
            }.collect { (transactions, income, expense) ->
                val balance = income - expense
                currentBalance = balance
                val df = DecimalFormat("0.00", DecimalFormatSymbols(Locale("uk")))
                binding.tvBalance.text = "${df.format(balance)} грн"
                binding.tvBalance.setTextColor(
                    if (balance >= 0) android.graphics.Color.parseColor("#4CAF50")
                    else android.graphics.Color.parseColor("#F44336")
                )
                binding.tvTotalIncome.text = "+ ${df.format(income)} грн"
                binding.tvTotalExpense.text = "− ${df.format(expense)} грн"
                allTransactions = transactions
                applyFilters()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_company_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_rename_company -> {
                showRenameDialog()
                true
            }
            R.id.action_delete_company -> {
                confirmDeleteCompany()
                true
            }
            R.id.action_export_pdf -> {
                showDateRangePicker()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showRenameDialog() {
        val input = EditText(this)
        input.setText(companyName)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.rename_company))
            .setView(input)
            .setPositiveButton(getString(R.string.save)) { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.companyDao().update(Company(id = companyId, name = newName))
                        companyName = newName
                        supportActionBar?.title = companyName
                    }
                } else {
                    Toast.makeText(this, getString(R.string.fill_name), Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun confirmDeleteCompany() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_company))
            .setMessage(getString(R.string.delete_company_confirm))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                lifecycleScope.launch {
                    db.companyDao().delete(Company(id = companyId, name = companyName))
                    withContext(Dispatchers.Main) {
                        finish()
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText(getString(R.string.select_period))
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            val start = range.first
            val end = range.second
            if (start != null && end != null) {
                dateFilterStart = start
                dateFilterEnd = end
                updateDateFilterLabel()
                applyFilters()
                generatePdf(start, end)
            }
        }

        picker.show(supportFragmentManager, "date_range")
    }

    private fun generatePdf(startMillis: Long, endMillis: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dao = db.transactionDao()
                val transactions = dao.getByCompanyAndPeriod(companyId, startMillis, endMillis)
                if (transactions.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CompanyDetailActivity, getString(R.string.pdf_no_transactions), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val dfAmount = DecimalFormat("0.00", DecimalFormatSymbols(Locale("uk")))
                val dfDate = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk"))

                val pdf = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
                val page = pdf.startPage(pageInfo)
                val canvas = page.canvas
                val paint = Paint()
                paint.isAntiAlias = true

                var y = 40f
                paint.textSize = 18f
                paint.isFakeBoldText = true
                canvas.drawText("Звіт по фірмі: $companyName", 40f, y, paint)

                y += 24f
                paint.textSize = 14f
                paint.isFakeBoldText = false
                val periodText = "Період: ${dfDate.format(startMillis)} - ${dfDate.format(endMillis)}"
                canvas.drawText(periodText, 40f, y, paint)

                y += 24f
                val balanceText = "Поточний баланс: ${dfAmount.format(currentBalance)} грн"
                canvas.drawText(balanceText, 40f, y, paint)

                y += 32f
                paint.isFakeBoldText = true
                canvas.drawText("Дата        Тип      Сума, грн        Опис", 40f, y, paint)
                paint.isFakeBoldText = false
                y += 16f

                for (t in transactions) {
                    val dateStr = dfDate.format(t.dateMillis)
                    val typeStr = if (t.type == "income") "Дохід" else "Витрата"
                    val amountStr = dfAmount.format(t.amount)
                    val line = "$dateStr  $typeStr  $amountStr  ${t.description}"
                    canvas.drawText(line, 40f, y, paint)
                    y += 16f
                }

                pdf.finishPage(page)

                val resolver = contentResolver
                val fileName = "report_${companyId}_${System.currentTimeMillis()}.pdf"
                val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI
                }

                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                }

                val uri: Uri? = resolver.insert(collection, values)
                if (uri == null) {
                    pdf.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CompanyDetailActivity, getString(R.string.pdf_error), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                resolver.openOutputStream(uri)?.use { out ->
                    pdf.writeTo(out)
                } ?: run {
                    pdf.close()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CompanyDetailActivity, getString(R.string.pdf_error), Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }
                pdf.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@CompanyDetailActivity,
                        getString(R.string.pdf_saved),
                        Toast.LENGTH_LONG
                    ).show()
                    val openIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    try {
                        startActivity(openIntent)
                    } catch (e: ActivityNotFoundException) {
                        Toast.makeText(
                            this@CompanyDetailActivity,
                            "Не знайдено програму для відкриття PDF",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CompanyDetailActivity, getString(R.string.pdf_error), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun startAddTransaction(type: String) {
        val i = Intent(this, AddTransactionActivity::class.java)
        i.putExtra(AddTransactionActivity.EXTRA_COMPANY_ID, companyId)
        i.putExtra(AddTransactionActivity.EXTRA_TYPE, type)
        startActivity(i)
    }

    private fun setupFiltersUi() {
        binding.etSearch.addTextChangedListener { text, _, _, _ ->
            searchQuery = text?.toString()?.trim().orEmpty()
            applyFilters()
        }

        binding.chipAll.setOnClickListener {
            typeFilter = null
            applyFilters()
        }
        binding.chipIncome.setOnClickListener {
            typeFilter = "income"
            applyFilters()
        }
        binding.chipExpense.setOnClickListener {
            typeFilter = "expense"
            applyFilters()
        }

        binding.btnDateFilter.setOnClickListener {
            showDateRangePicker()
        }
        binding.btnClearDateFilter.setOnClickListener {
            dateFilterStart = null
            dateFilterEnd = null
            updateDateFilterLabel()
            applyFilters()
        }
        updateDateFilterLabel()
    }

    private fun applyFilters() {
        var list = allTransactions

        typeFilter?.let { type ->
            list = list.filter { it.type == type }
        }

        if (searchQuery.isNotEmpty()) {
            val q = searchQuery.lowercase(Locale.getDefault())
            list = list.filter { it.description.lowercase(Locale.getDefault()).contains(q) }
        }

        dateFilterStart?.let { start ->
            list = list.filter { it.dateMillis >= start }
        }
        dateFilterEnd?.let { end ->
            list = list.filter { it.dateMillis <= end }
        }

        transactionAdapter.submitList(list)
        val isEmpty = list.isEmpty()
        binding.emptyTransactions.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerTransactions.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun updateDateFilterLabel() {
        if (dateFilterStart == null || dateFilterEnd == null) {
            binding.tvDateFilter.text = getString(R.string.filter_date)
        } else {
            val df = SimpleDateFormat("dd.MM.yyyy", Locale("uk"))
            val text = "${df.format(dateFilterStart)} - ${df.format(dateFilterEnd)}"
            binding.tvDateFilter.text = text
        }
    }
}

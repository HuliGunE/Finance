package com.finance.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finance.app.databinding.ActivityCompanyDetailBinding
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

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

        lifecycleScope.launch {
            combine(
                db.transactionDao().getByCompany(companyId),
                db.transactionDao().getTotalIncome(companyId),
                db.transactionDao().getTotalExpense(companyId)
            ) { transactions, income, expense ->
                Triple(transactions, income, expense)
            }.collect { (transactions, income, expense) ->
                val balance = income - expense
                val df = java.text.DecimalFormat("0.00", java.text.DecimalFormatSymbols(java.util.Locale("uk")))
                binding.tvBalance.text = "${df.format(balance)} грн"
                binding.tvBalance.setTextColor(
                    if (balance >= 0) android.graphics.Color.parseColor("#4CAF50")
                    else android.graphics.Color.parseColor("#F44336")
                )
                binding.tvTotalIncome.text = "+ ${df.format(income)} грн"
                binding.tvTotalExpense.text = "− ${df.format(expense)} грн"
                transactionAdapter.submitList(transactions)
                binding.emptyTransactions.visibility = if (transactions.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
                binding.recyclerTransactions.visibility = if (transactions.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
        }
    }

    private fun startAddTransaction(type: String) {
        val i = Intent(this, AddTransactionActivity::class.java)
        i.putExtra(AddTransactionActivity.EXTRA_COMPANY_ID, companyId)
        i.putExtra(AddTransactionActivity.EXTRA_TYPE, type)
        startActivity(i)
    }
}

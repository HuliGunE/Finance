package com.finance.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finance.app.databinding.ActivityAddTransactionBinding
import kotlinx.coroutines.launch

class AddTransactionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_COMPANY_ID = "company_id"
        const val EXTRA_TYPE = "type" // "income" or "expense"
    }

    private lateinit var binding: ActivityAddTransactionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val type = intent.getStringExtra(EXTRA_TYPE) ?: "income"
        val companyId = intent.getLongExtra(EXTRA_COMPANY_ID, 0)
        supportActionBar?.title = if (type == "income") getString(R.string.add_income) else getString(R.string.add_expense)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text?.toString()?.trim()
            val amount = amountStr?.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, getString(R.string.fill_amount), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val description = binding.etDescription.text?.toString()?.trim() ?: ""
            lifecycleScope.launch {
                AppDatabase.getInstance(this@AddTransactionActivity).transactionDao().insert(
                    Transaction(
                        companyId = companyId,
                        type = type,
                        amount = amount,
                        description = description
                    )
                )
                Toast.makeText(this@AddTransactionActivity, "Збережено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

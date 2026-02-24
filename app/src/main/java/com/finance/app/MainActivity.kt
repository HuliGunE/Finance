package com.finance.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.finance.app.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var adapter: CompanyAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupDeveloperEasterEgg()
        db = AppDatabase.getInstance(this)

        adapter = CompanyAdapter { company ->
            val i = Intent(this, CompanyDetailActivity::class.java)
            i.putExtra(CompanyDetailActivity.EXTRA_COMPANY_ID, company.id)
            i.putExtra(CompanyDetailActivity.EXTRA_COMPANY_NAME, company.name)
            startActivity(i)
        }

        binding.recyclerCompanies.layoutManager = LinearLayoutManager(this)
        binding.recyclerCompanies.adapter = adapter

        binding.fabAddCompany.setOnClickListener {
            startActivity(Intent(this, AddCompanyActivity::class.java))
        }

        fun loadCompanies() {
            lifecycleScope.launch {
                db.companyDao().getAllCompanies().collect { companies ->
                    lifecycleScope.launch {
                        if (companies.isEmpty()) {
                            binding.emptyText.visibility = android.view.View.VISIBLE
                            binding.recyclerCompanies.visibility = android.view.View.GONE
                        } else {
                            binding.emptyText.visibility = android.view.View.GONE
                            binding.recyclerCompanies.visibility = android.view.View.VISIBLE
                            val list = companies.map { company ->
                                val income = db.transactionDao().getTotalIncome(company.id).first()
                                val expense = db.transactionDao().getTotalExpense(company.id).first()
                                CompanyWithBalance(company, income - expense)
                            }
                            adapter.submitList(list)
                        }
                    }
                }
            }
        }
        loadCompanies()
    }

    private fun setupDeveloperEasterEgg() {
        var tapCount = 0
        binding.toolbar.setOnClickListener {
            tapCount++
            if (tapCount >= 5) {
                tapCount = 0
                Toast.makeText(
                    this,
                    getString(R.string.developer_info_text),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized && ::db.isInitialized) {
            lifecycleScope.launch {
                db.companyDao().getAllCompanies().collect { companies ->
                    if (companies.isEmpty()) return@collect
                    lifecycleScope.launch {
                        val list = companies.map { company ->
                            val income = db.transactionDao().getTotalIncome(company.id).first()
                            val expense = db.transactionDao().getTotalExpense(company.id).first()
                            CompanyWithBalance(company, income - expense)
                        }
                        adapter.submitList(list)
                    }
                }
            }
        }
    }
}

package com.finance.app

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.finance.app.databinding.ActivityAddCompanyBinding
import kotlinx.coroutines.launch

class AddCompanyActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddCompanyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddCompanyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.btnSave.setOnClickListener {
            val name = binding.etCompanyName.text?.toString()?.trim()
            if (name.isNullOrEmpty()) {
                Toast.makeText(this, getString(R.string.fill_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                AppDatabase.getInstance(this@AddCompanyActivity).companyDao().insert(Company(name = name))
                Toast.makeText(this@AddCompanyActivity, "Фірму додано", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}

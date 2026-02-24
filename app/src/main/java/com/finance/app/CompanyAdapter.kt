package com.finance.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finance.app.databinding.ItemCompanyBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CompanyWithBalance(
    val company: Company,
    val balance: Double
)

class CompanyAdapter(
    private val onItemClick: (Company) -> Unit
) : ListAdapter<CompanyWithBalance, CompanyAdapter.ViewHolder>(DiffCallback()) {

    private val df = DecimalFormat("0.00", DecimalFormatSymbols(Locale("uk")))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCompanyBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemCompanyBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val item = getItem(adapterPosition)
                onItemClick(item.company)
            }
        }

        fun bind(item: CompanyWithBalance) {
            binding.tvCompanyName.text = item.company.name
            val balanceStr = "${df.format(item.balance)} грн"
            binding.tvCompanyBalance.text = "Залишок: $balanceStr"
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<CompanyWithBalance>() {
        override fun areItemsTheSame(a: CompanyWithBalance, b: CompanyWithBalance) = a.company.id == b.company.id
        override fun areContentsTheSame(a: CompanyWithBalance, b: CompanyWithBalance) =
            a.company == b.company && a.balance == b.balance
    }
}

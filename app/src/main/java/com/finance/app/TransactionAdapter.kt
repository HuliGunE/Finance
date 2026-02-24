package com.finance.app

import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.finance.app.databinding.ItemTransactionBinding
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class TransactionAdapter(
    private val onDelete: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(DiffCallback()) {

    private val df = DecimalFormat("0.00", DecimalFormatSymbols(Locale("uk")))
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale("uk"))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(t: Transaction) {
            binding.tvDescription.text = t.description.ifEmpty { if (t.type == "income") "Дохід" else "Витрата" }
            binding.tvDate.text = dateFormat.format(Date(t.dateMillis))
            val prefix = if (t.type == "income") "+" else "−"
            binding.tvAmount.text = "$prefix ${df.format(t.amount)} грн"
            binding.tvAmount.setTextColor(
                ContextCompat.getColor(binding.root.context, if (t.type == "income") R.color.income_green else R.color.expense_red)
            )
            binding.root.setOnLongClickListener {
                onDelete(t)
                true
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(a: Transaction, b: Transaction) = a.id == b.id
        override fun areContentsTheSame(a: Transaction, b: Transaction) = a == b
    }
}

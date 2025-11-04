package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class PagamentoAdapter(
    private var listaPagamentos: List<Pagamento>,
    private val listener: OnPagamentoRemoveListener
) : RecyclerView.Adapter<PagamentoAdapter.PagamentoViewHolder>() {

    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    interface OnPagamentoRemoveListener {
        fun onRemovePagamentoClicked(position: Int)
    }

    class PagamentoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val forma: TextView = itemView.findViewById(R.id.tvFormaPagamentoItem)
        val valor: TextView = itemView.findViewById(R.id.tvValorPagamentoItem)
        val btnRemover: ImageButton = itemView.findViewById(R.id.btnRemoverPagamento)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PagamentoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pagamento_adicionado, parent, false)
        return PagamentoViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaPagamentos.size
    }

    override fun onBindViewHolder(holder: PagamentoViewHolder, position: Int) {
        val pagamento = listaPagamentos[position]

        holder.forma.text = pagamento.forma
        holder.valor.text = formatoMoeda.format(pagamento.valor)

        holder.btnRemover.setOnClickListener {
            listener.onRemovePagamentoClicked(position)
        }
    }

    fun atualizarLista(novaLista: List<Pagamento>) {
        listaPagamentos = novaLista
        notifyDataSetChanged()
    }
}
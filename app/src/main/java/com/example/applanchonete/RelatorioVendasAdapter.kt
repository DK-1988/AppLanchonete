package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class RelatorioVendasAdapter(
    private var listaVendas: List<Venda>
) : RecyclerView.Adapter<RelatorioVendasAdapter.VendaViewHolder>() {

    class VendaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dataVenda: TextView = itemView.findViewById(R.id.tvDataVenda)
        val formaPagamento: TextView = itemView.findViewById(R.id.tvFormaPagamento)
        val totalVenda: TextView = itemView.findViewById(R.id.tvTotalVendaItem)
        val lucroVenda: TextView = itemView.findViewById(R.id.tvLucroVendaItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VendaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_relatorio_venda, parent, false)
        return VendaViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaVendas.size
    }

    override fun onBindViewHolder(holder: VendaViewHolder, position: Int) {
        val venda = listaVendas[position]

        val dataFormatada = if (venda.dataHora != null) {
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(venda.dataHora.toDate())
        } else { "Data indisponível" }

        val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val totalFormatado = formatoMoeda.format(venda.totalVenda)
        val lucroFormatado = formatoMoeda.format(venda.lucroTotal)

        holder.dataVenda.text = dataFormatada
        holder.totalVenda.text = totalFormatado
        holder.lucroVenda.text = "Lucro: $lucroFormatado"

        if (venda.pagamentos.isEmpty()) {
            holder.formaPagamento.text = "N/A"
        } else if (venda.pagamentos.size == 1) {
            holder.formaPagamento.text = venda.pagamentos[0].forma
        } else {
            holder.formaPagamento.text = "Múltiplos Pagamentos"
        }
    }

    fun atualizarLista(novaLista: List<Venda>) {
        listaVendas = novaLista
        notifyDataSetChanged()
    }
}
package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat // NOVO: Import para formatar moeda
import java.util.Locale     // NOVO: Import para Locale

class MaisVendidosAdapter(
    private var listaProdutos: List<ProdutoVendidoInfo>
) : RecyclerView.Adapter<MaisVendidosAdapter.ProdutoViewHolder>() {

    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    class ProdutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val posicao: TextView = itemView.findViewById(R.id.tvPosicao)
        val nome: TextView = itemView.findViewById(R.id.tvNomeProdutoMaisVendido)
        val quantidade: TextView = itemView.findViewById(R.id.tvQuantidadeMaisVendida)
        val lucro: TextView = itemView.findViewById(R.id.tvLucroProdutoMaisVendido) // NOVO
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto_mais_vendido, parent, false)
        return ProdutoViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaProdutos.size
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        val produto = listaProdutos[position]

        holder.posicao.text = "${position + 1}."

        holder.nome.text = produto.nome

        holder.quantidade.text = "${produto.quantidade} unid."

        holder.lucro.text = "Lucro: ${formatoMoeda.format(produto.lucroTotal)}"
    }

    fun atualizarLista(novaLista: List<ProdutoVendidoInfo>) {
        listaProdutos = novaLista
        notifyDataSetChanged()
    }
}
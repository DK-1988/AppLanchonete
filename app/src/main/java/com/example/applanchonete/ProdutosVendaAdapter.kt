package com.example.applanchonete

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class ProdutosVendaAdapter(
    private val listener: OnProdutoVendaClickListener
) : ListAdapter<Produto, ProdutosVendaAdapter.ProdutosVendaViewHolder>(DiffCallback()) {

    interface OnProdutoVendaClickListener {
        fun onProdutoClicked(produto: Produto)
    }

    class ProdutosVendaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.tvNomeProdutoVenda)
        val preco: TextView = itemView.findViewById(R.id.tvPrecoProdutoVenda)
        val estoque: TextView = itemView.findViewById(R.id.tvEstoqueVenda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutosVendaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto_venda, parent, false)
        return ProdutosVendaViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProdutosVendaViewHolder, position: Int) {
        val produto = getItem(position)
        val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val precoFormatado = formatoMoeda.format(produto.precoVenda)

        holder.nome.text = produto.nome
        holder.preco.text = precoFormatado
        holder.estoque.text = "Estoque: ${produto.quantidadeEstoque}"

        if (produto.quantidadeEstoque <= 0) {
            holder.nome.setTextColor(Color.GRAY)
            holder.preco.setTextColor(Color.GRAY)
            holder.estoque.text = "ESGOTADO"
            holder.estoque.setTextColor(Color.RED)
            holder.itemView.isClickable = false
            holder.itemView.alpha = 0.6f
        } else {
            holder.nome.setTextColor(Color.BLACK)
            holder.preco.setTextColor(Color.parseColor("#FF3700B3"))
            holder.estoque.setTextColor(Color.DKGRAY)
            holder.itemView.isClickable = true
            holder.itemView.alpha = 1.0f
        }

        holder.itemView.setOnClickListener {
            listener.onProdutoClicked(produto)
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Produto>() {
        override fun areItemsTheSame(oldItem: Produto, newItem: Produto): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Produto, newItem: Produto): Boolean =
            oldItem == newItem
    }

    fun atualizarLista(novaLista: List<Produto>) {
        submitList(novaLista)
    }
}
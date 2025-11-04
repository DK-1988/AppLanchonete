package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class CarrinhoVendaAdapter(
    private val listener: OnCarrinhoInteractionListener
) : ListAdapter<ItemVenda, CarrinhoVendaAdapter.CarrinhoViewHolder>(DiffCallback()) {

    interface OnCarrinhoInteractionListener {
        fun onAumentarQuantidade(position: Int)
        fun onDiminuirQuantidade(position: Int)
        fun onRemoverItem(position: Int)
    }

    class CarrinhoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.tvNomeItemCarrinho)
        val precoTotal: TextView = itemView.findViewById(R.id.tvPrecoTotalItemCarrinho)
        val precoUnitario: TextView = itemView.findViewById(R.id.tvPrecoUnitarioItem)
        val quantidade: TextView = itemView.findViewById(R.id.tvQuantidadeItem)
        val btnAumentar: Button = itemView.findViewById(R.id.btnAumentarQtd)
        val btnDiminuir: Button = itemView.findViewById(R.id.btnDiminuirQtd)
        val opcoes: TextView = itemView.findViewById(R.id.tvOpcoesItemCarrinho)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarrinhoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carrinho_venda, parent, false)
        return CarrinhoViewHolder(view)
    }

    override fun onBindViewHolder(holder: CarrinhoViewHolder, position: Int) {
        val item = getItem(position)
        val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

        val precoUnitarioComAdicionais = item.precoUnitario + item.precoAdicionais
        val precoUnitarioFormatado = formatoMoeda.format(precoUnitarioComAdicionais)
        val precoTotalFormatado = formatoMoeda.format(item.totalItem)

        holder.nome.text = item.nomeProduto
        holder.quantidade.text = item.quantidade.toString()
        holder.precoUnitario.text = "($precoUnitarioFormatado / un)"
        holder.precoTotal.text = precoTotalFormatado

        if (item.opcoesSelecionadas.isNotEmpty()) {
            val opcoesStr = item.opcoesSelecionadas.joinToString(", ") { it.nome }
            holder.opcoes.text = "($opcoesStr)"
            holder.opcoes.visibility = View.VISIBLE
        } else {
            holder.opcoes.visibility = View.GONE
        }

        holder.btnAumentar.setOnClickListener {
            listener.onAumentarQuantidade(position)
        }

        if (item.quantidade > 1) {
            holder.btnDiminuir.text = "-"
            holder.btnDiminuir.setOnClickListener {
                listener.onDiminuirQuantidade(position)
            }
        } else {
            holder.btnDiminuir.text = "X"
            holder.btnDiminuir.setOnClickListener {
                listener.onRemoverItem(position)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<ItemVenda>() {
        override fun areItemsTheSame(oldItem: ItemVenda, newItem: ItemVenda): Boolean {
            // CORREÇÃO: Compara o ID e as opções
            return oldItem.produtoId == newItem.produtoId &&
                    oldItem.opcoesSelecionadas == newItem.opcoesSelecionadas
        }

        override fun areContentsTheSame(oldItem: ItemVenda, newItem: ItemVenda): Boolean {
            return oldItem == newItem
        }
    }

    fun atualizarLista(novaLista: List<ItemVenda>) {
        submitList(novaLista.toList())
    }
}
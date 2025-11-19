package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class ModificadorOpcaoAdapter(
    private var listaOpcoes: List<ModificadorOpcao>,
    private val listener: OnOpcaoRemoveListener
) : RecyclerView.Adapter<ModificadorOpcaoAdapter.OpcaoViewHolder>() {

    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    interface OnOpcaoRemoveListener {
        fun onRemoveOpcaoClicked(position: Int)
    }

    class OpcaoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.tvNomeOpcaoItem)
        val preco: TextView = itemView.findViewById(R.id.tvPrecoOpcaoItem)
        val btnRemover: ImageButton = itemView.findViewById(R.id.btnRemoverOpcao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OpcaoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_modificador_opcao, parent, false)
        return OpcaoViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaOpcoes.size
    }

    override fun onBindViewHolder(holder: OpcaoViewHolder, position: Int) {
        val opcao = listaOpcoes[position]

        holder.nome.text = opcao.nome

        if (opcao.precoAdicional > 0) {
            holder.preco.text = "Preço: ${formatoMoeda.format(opcao.precoAdicional)}"
        } else {
            holder.preco.text = "Preço: R$ 0,00"
        }

        holder.btnRemover.setOnClickListener {
            listener.onRemoveOpcaoClicked(position)
        }
    }

    fun atualizarLista(novaLista: List<ModificadorOpcao>) {
        listaOpcoes = novaLista
        notifyDataSetChanged()
    }
}
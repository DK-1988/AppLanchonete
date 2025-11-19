package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
// ... (outros imports)
import androidx.recyclerview.widget.RecyclerView

class SetorFiltroAdapter(
    private var listaSetores: List<Setor>,
    private val listener: OnSetorFiltroClickListener
) : RecyclerView.Adapter<SetorFiltroAdapter.SetorFiltroViewHolder>() {

    interface OnSetorFiltroClickListener {
        fun onSetorFiltroClicked(nomeSetor: String)
    }

    class SetorFiltroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeSetor: TextView = itemView.findViewById(R.id.tvNomeSetorFiltro)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetorFiltroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_setor_filtro, parent, false)
        return SetorFiltroViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaSetores.size
    }

    override fun onBindViewHolder(holder: SetorFiltroViewHolder, position: Int) {
        val setor = listaSetores[position]

        holder.nomeSetor.text = setor.nome

        holder.itemView.setOnClickListener {
            listener.onSetorFiltroClicked(setor.nome)
        }
    }

    fun atualizarLista(novaLista: List<Setor>) {
        listaSetores = novaLista
        notifyDataSetChanged()
    }
}
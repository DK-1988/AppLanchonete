package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.util.Locale

class SelecionarClienteAdapter(
    private var listaClientesCompleta: List<Cliente>,
    private val listener: OnClienteSelecionadoListener
) : RecyclerView.Adapter<SelecionarClienteAdapter.ClienteViewHolder>(), Filterable {

    private var listaClientesFiltrada: List<Cliente> = listaClientesCompleta

    interface OnClienteSelecionadoListener {
        fun onClienteSelecionado(cliente: Cliente)
    }

    class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.tvNomeClienteSelecao)
        val cpfCnpj: TextView = itemView.findViewById(R.id.tvCpfCnpjClienteSelecao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selecionar_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaClientesFiltrada.size
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = listaClientesFiltrada[position]
        holder.nome.text = cliente.nomeRazaoSocial
        holder.cpfCnpj.text = cliente.cpfCnpj
        holder.itemView.setOnClickListener {
            listener.onClienteSelecionado(cliente)
        }
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val textoFiltro = constraint.toString().lowercase(Locale.getDefault()).trim()

                listaClientesFiltrada = if (textoFiltro.isEmpty()) {
                    listaClientesCompleta
                } else {
                    listaClientesCompleta.filter {
                        it.nomeRazaoSocial.lowercase(Locale.getDefault()).contains(textoFiltro) ||
                                it.cpfCnpj.contains(textoFiltro)
                    }
                }
                val filterResults = FilterResults()
                filterResults.values = listaClientesFiltrada
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                listaClientesFiltrada = results?.values as? List<Cliente> ?: emptyList()
                notifyDataSetChanged()
            }
        }
    }

    fun atualizarListaCompleta(novaLista: List<Cliente>) {
        listaClientesCompleta = novaLista
        listaClientesFiltrada = novaLista
        notifyDataSetChanged()
    }
}
package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

/**
 * --- Documentação: ModificadorOpcaoAdapter ---
 *
 * Exibe a lista de opções (ex: "+Bacon") DENTRO do formulário
 * AdicionarModificadorActivity.
 *
 * @param listaOpcoes A lista de opções que estão sendo adicionadas.
 * @param listener A Activity, que "ouve" o clique no botão de remover.
 */
class ModificadorOpcaoAdapter(
    private var listaOpcoes: List<ModificadorOpcao>,
    private val listener: OnOpcaoRemoveListener
) : RecyclerView.Adapter<ModificadorOpcaoAdapter.OpcaoViewHolder>() {

    // Formato para moeda
    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    /**
     * --- Interface de Callback ---
     * Define o contrato para a Activity saber qual item remover
     * da lista temporária.
     */
    interface OnOpcaoRemoveListener {
        fun onRemoveOpcaoClicked(position: Int)
    }

    /**
     * --- ViewHolder ---
     * "Segura" as referências do layout 'item_modificador_opcao.xml'.
     */
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

        // Formata o preço
        if (opcao.precoAdicional > 0) {
            holder.preco.text = "Preço: ${formatoMoeda.format(opcao.precoAdicional)}"
        } else {
            holder.preco.text = "Preço: R$ 0,00"
        }

        // Configura o clique no botão "X" (Remover)
        holder.btnRemover.setOnClickListener {
            // Avisa a Activity (via interface) que este item deve ser removido
            listener.onRemoveOpcaoClicked(position)
        }
    }

    /**
     * Atualiza a lista de opções exibida no adapter.
     */
    fun atualizarLista(novaLista: List<ModificadorOpcao>) {
        listaOpcoes = novaLista
        notifyDataSetChanged()
    }
}
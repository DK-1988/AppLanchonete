package com.example.applanchonete

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
class RelatorioCaixaAdapter(
    private var listaSessoes: List<SessaoCaixa>
) : RecyclerView.Adapter<RelatorioCaixaAdapter.CaixaViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    class CaixaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dataAbertura: TextView = itemView.findViewById(R.id.tvDataAberturaCaixa)
        val status: TextView = itemView.findViewById(R.id.tvStatusCaixa)
        val usuario: TextView = itemView.findViewById(R.id.tvUsuarioCaixa)
        val valorInicial: TextView = itemView.findViewById(R.id.tvValorInicialCaixa)
        val valorVendido: TextView = itemView.findViewById(R.id.tvValorVendidoCaixa)
        val valorFinal: TextView = itemView.findViewById(R.id.tvValorFinalCaixa)
        val dataFechamento: TextView = itemView.findViewById(R.id.tvDataFechamentoCaixa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaixaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_relatorio_caixa, parent, false)
        return CaixaViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaSessoes.size
    }

    override fun onBindViewHolder(holder: CaixaViewHolder, position: Int) {
        val sessao = listaSessoes[position]

        holder.dataAbertura.text = formatarTimestamp(sessao.dataAbertura, "Aberto em: ")
        holder.status.text = sessao.status
        if (sessao.status == "Fechado") {
            holder.status.setTextColor(Color.parseColor("#FF6200EE"))
            holder.status.setBackgroundResource(R.drawable.item_borda_tipo)
        } else {
            holder.status.setTextColor(Color.parseColor("#FF018786"))
            holder.status.setBackgroundResource(R.drawable.item_borda_tipo_verde)
        }

        holder.usuario.text = "Usuário: ${sessao.usuarioEmail}"
        holder.valorInicial.text = "Inicial: ${formatoMoeda.format(sessao.valorInicial)}"
        holder.valorVendido.text = "Vendido: ${formatoMoeda.format(sessao.valorTotalVendas)}"
        holder.valorFinal.text = "Final: ${formatoMoeda.format(sessao.valorFinalCalculado)}"

        if (sessao.status == "Fechado" && sessao.dataFechamento != null) {
            holder.dataFechamento.text = formatarTimestamp(sessao.dataFechamento, "Fechado em: ")
            holder.dataFechamento.visibility = View.VISIBLE
        } else {
            holder.dataFechamento.visibility = View.GONE
        }
    }

    private fun formatarTimestamp(timestamp: Timestamp?, prefixo: String): String {
        return if (timestamp != null) {
            prefixo + sdf.format(timestamp.toDate())
        } else {
            prefixo + "Indisponível"
        }
    }

    fun atualizarLista(novaLista: List<SessaoCaixa>) {
        listaSessoes = novaLista
        notifyDataSetChanged()
    }
}
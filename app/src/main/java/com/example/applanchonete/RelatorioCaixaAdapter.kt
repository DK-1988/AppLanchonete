package com.example.applanchonete

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class RelatorioCaixaAdapter(
    private var listaSessoes: List<SessaoCaixa>
) : RecyclerView.Adapter<RelatorioCaixaAdapter.CaixaViewHolder>() {

    class CaixaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val data: TextView = itemView.findViewById(R.id.tvDataAberturaCaixa)
        val status: TextView = itemView.findViewById(R.id.tvStatusCaixa)
        val usuario: TextView = itemView.findViewById(R.id.tvUsuarioCaixa)
        val total: TextView = itemView.findViewById(R.id.tvTotalVendasCaixa)
        val valorInicial: TextView = itemView.findViewById(R.id.tvValorInicialCaixa)
        val dinheiro: TextView = itemView.findViewById(R.id.tvDinheiroCaixa)
        val pix: TextView = itemView.findViewById(R.id.tvPixCaixa)
        val credito: TextView = itemView.findViewById(R.id.tvCreditoCaixa)
        val debito: TextView = itemView.findViewById(R.id.tvDebitoCaixa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CaixaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_relatorio_caixa, parent, false)
        return CaixaViewHolder(view)
    }

    override fun getItemCount(): Int = listaSessoes.size

    override fun onBindViewHolder(holder: CaixaViewHolder, position: Int) {
        val sessao = listaSessoes[position]
        val fMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val dataStr = if(sessao.dataAbertura != null) sdf.format(sessao.dataAbertura.toDate()) else "N/A"

        holder.data.text = dataStr
        holder.status.text = sessao.status
        holder.usuario.text = "Usuário: ${sessao.usuarioEmail}"
        holder.valorInicial.text = "Fundo Inicial: ${fMoeda.format(sessao.valorInicial)}"
        holder.dinheiro.text = "Dinheiro: ${fMoeda.format(sessao.totalDinheiro)}"
        holder.pix.text = "PIX: ${fMoeda.format(sessao.totalPix)}"
        holder.credito.text = "Crédito: ${fMoeda.format(sessao.totalCartaoCredito)}"
        holder.debito.text = "Débito: ${fMoeda.format(sessao.totalCartaoDebito)}"

        holder.total.text = fMoeda.format(sessao.valorTotalVendas)
    }

    fun atualizarLista(novaLista: List<SessaoCaixa>) {
        listaSessoes = novaLista
        notifyDataSetChanged()
    }
}
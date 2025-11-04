package com.example.applanchonete

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.util.Locale

class ProdutoAdapter(
    private val context: Context,
    private val listaProdutos: List<Produto>
) :
    RecyclerView.Adapter<ProdutoAdapter.ProdutoViewHolder>() {

    class ProdutoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.tvNomeProduto)
        val setor: TextView = itemView.findViewById(R.id.tvSetorProduto)
        val precoVenda: TextView = itemView.findViewById(R.id.tvPrecoVenda)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProdutoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_produto, parent, false)
        return ProdutoViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaProdutos.size
    }

    override fun onBindViewHolder(holder: ProdutoViewHolder, position: Int) {
        val produto = listaProdutos[position]

        val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val precoFormatado = formatoMoeda.format(produto.precoVenda)

        holder.nome.text = produto.nome
        holder.setor.text = produto.setor
        holder.precoVenda.text = precoFormatado

        holder.itemView.setOnClickListener {
            val intent = Intent(context, AdicionarProdutoActivity::class.java)
            intent.putExtra("PRODUTO_ID", produto.id)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            mostrarDialogoDeExclusao(produto)

            return@setOnLongClickListener true
        }
    }

    private fun mostrarDialogoDeExclusao(produto: Produto) {
        AlertDialog.Builder(context)
            .setTitle("Confirmar Exclusão")
            .setMessage("Tem a certeza de que deseja excluir o produto '${produto.nome}'?")
            .setPositiveButton("Sim, Excluir") { dialog, _ ->
                excluirProdutoDoFirebase(produto.id)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun excluirProdutoDoFirebase(produtoId: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("produtos").document(produtoId)
            .delete()
            .addOnSuccessListener {
                // Sucesso! Mostra uma mensagem
                Toast.makeText(context, "Produto excluído com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao excluir o produto: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
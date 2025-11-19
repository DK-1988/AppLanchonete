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

class SetorAdapter(
    private val context: Context,
    private var listaSetores: List<Setor>
) : RecyclerView.Adapter<SetorAdapter.SetorViewHolder>() {

    class SetorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.tvNomeSetorItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetorViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_setor, parent, false)
        return SetorViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaSetores.size
    }

    override fun onBindViewHolder(holder: SetorViewHolder, position: Int) {
        val setor = listaSetores[position]
        holder.nome.text = setor.nome
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AdicionarSetorActivity::class.java)
            intent.putExtra("SETOR_ID", setor.id)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(context)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir o setor '${setor.nome}'?")
                .setPositiveButton("Sim, Excluir") { _, _ ->
                    excluirSetorDoFirebase(setor.id)
                }
                .setNegativeButton("Cancelar", null)
                .create()
                .show()

            return@setOnLongClickListener true
        }
    }
    private fun excluirSetorDoFirebase(setorId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("setores").document(setorId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Setor excluído com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun atualizarLista(novaLista: List<Setor>) {
        listaSetores = novaLista
        notifyDataSetChanged()
    }
}
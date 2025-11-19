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

class ModificadorGrupoAdapter(
    private val context: Context,
    private var listaGrupos: List<ModificadorGrupo>
) : RecyclerView.Adapter<ModificadorGrupoAdapter.GrupoViewHolder>() {

    class GrupoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nomeGrupo: TextView = itemView.findViewById(R.id.tvNomeGrupo)
        val tipoSelecao: TextView = itemView.findViewById(R.id.tvTipoSelecaoGrupo)
        val contagemOpcoes: TextView = itemView.findViewById(R.id.tvContagemOpcoes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GrupoViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_modificador_grupo, parent, false)
        return GrupoViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaGrupos.size
    }

    override fun onBindViewHolder(holder: GrupoViewHolder, position: Int) {
        val grupo = listaGrupos[position]

        holder.nomeGrupo.text = grupo.nome

        val tipo = if (grupo.tipoSelecao == "UNICA") "Seleção Única" else "Seleção Múltipla"
        holder.tipoSelecao.text = tipo

        val contagem = grupo.opcoes.size
        holder.contagemOpcoes.text = "$contagem Opçõe${if (contagem == 1) "s" else "s"}" // Simples plural

        holder.itemView.setOnClickListener {
            val intent = Intent(context, AdicionarModificadorActivity::class.java)
            intent.putExtra("MODIFICADOR_GRUPO_ID", grupo.id)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(context)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir o grupo '${grupo.nome}'? (Produtos que usam este grupo não serão afetados, mas não poderão mais ser editados com ele)")
                .setPositiveButton("Sim, Excluir") { _, _ ->
                    excluirGrupoDoFirebase(grupo.id)
                }
                .setNegativeButton("Cancelar", null)
                .create()
                .show()

            return@setOnLongClickListener true
        }
    }

    private fun excluirGrupoDoFirebase(grupoId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("modificadores").document(grupoId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Grupo excluído com sucesso!", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun atualizarLista(novaLista: List<ModificadorGrupo>) {
        listaGrupos = novaLista
        notifyDataSetChanged()
    }
}
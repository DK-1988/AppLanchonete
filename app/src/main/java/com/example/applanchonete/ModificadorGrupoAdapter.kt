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

/**
 * --- Documentação: ModificadorGrupoAdapter ---
 *
 * Exibe a lista principal de Grupos de Modificadores (ex: "Tamanhos", "Adicionais")
 * na tela ModificadoresActivity.
 *
 * @param context O contexto da Activity que está usando o adapter.
 * @param listaGrupos A lista de grupos de modificadores.
 */
class ModificadorGrupoAdapter(
    private val context: Context,
    private var listaGrupos: List<ModificadorGrupo>
) : RecyclerView.Adapter<ModificadorGrupoAdapter.GrupoViewHolder>() {

    /**
     * --- ViewHolder ---
     * "Segura" as referências para os componentes do layout 'item_modificador_grupo.xml'.
     */
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
        // 1. Pega o grupo da lista
        val grupo = listaGrupos[position]

        // 2. Preenche os dados
        holder.nomeGrupo.text = grupo.nome

        // Traduz o tipo de seleção
        val tipo = if (grupo.tipoSelecao == "UNICA") "Seleção Única" else "Seleção Múltipla"
        holder.tipoSelecao.text = tipo

        // Exibe a contagem de opções
        val contagem = grupo.opcoes.size
        holder.contagemOpcoes.text = "$contagem Opçõe${if (contagem == 1) "s" else "s"}" // Simples plural

        // 3. Configura o Clique Simples (para Editar)
        holder.itemView.setOnClickListener {
            val intent = Intent(context, AdicionarModificadorActivity::class.java)
            // Passa o ID do grupo para a tela de formulário
            intent.putExtra("MODIFICADOR_GRUPO_ID", grupo.id)
            context.startActivity(intent)
        }

        // 4. Configura o Clique Longo (para Excluir)
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

            return@setOnLongClickListener true // Evento consumido
        }
    }

    /**
     * Exclui o grupo de modificadores do Firebase.
     */
    private fun excluirGrupoDoFirebase(grupoId: String) {
        val db = FirebaseFirestore.getInstance()
        db.collection("modificadores").document(grupoId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Grupo excluído com sucesso!", Toast.LENGTH_SHORT).show()
                // A ModificadoresActivity (que usará SnapshotListener)
                // atualizará a lista automaticamente.
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao excluir: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    /**
     * Função pública para atualizar a lista de grupos no adapter.
     */
    fun atualizarLista(novaLista: List<ModificadorGrupo>) {
        listaGrupos = novaLista
        notifyDataSetChanged()
    }
}
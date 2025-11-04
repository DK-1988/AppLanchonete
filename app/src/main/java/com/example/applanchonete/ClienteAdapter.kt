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
class ClienteAdapter(
    private val context: Context,
    private var listaClientes: List<Cliente>
) : RecyclerView.Adapter<ClienteAdapter.ClienteViewHolder>() {

    class ClienteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nome: TextView = itemView.findViewById(R.id.tvNomeCliente)
        val tipo: TextView = itemView.findViewById(R.id.tvTipoCliente)
        val cpfCnpj: TextView = itemView.findViewById(R.id.tvCpfCnpjCliente)
        val telefone: TextView = itemView.findViewById(R.id.tvTelefoneCliente)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClienteViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_cliente, parent, false)
        return ClienteViewHolder(view)
    }

    override fun getItemCount(): Int {
        return listaClientes.size
    }

    override fun onBindViewHolder(holder: ClienteViewHolder, position: Int) {
        val cliente = listaClientes[position]

        holder.nome.text = cliente.nomeRazaoSocial
        holder.tipo.text = cliente.tipoCliente
        holder.cpfCnpj.text = cliente.cpfCnpj
        holder.telefone.text = cliente.telefone

        holder.itemView.setOnClickListener {
            val intent = Intent(context, AdicionarClienteActivity::class.java)
            intent.putExtra("CLIENTE_ID", cliente.id)
            context.startActivity(intent)
        }

        holder.itemView.setOnLongClickListener {
            AlertDialog.Builder(context)
                .setTitle("Confirmar Exclusão")
                .setMessage("Tem certeza que deseja excluir o cliente '${cliente.nomeRazaoSocial}'?")
                .setPositiveButton("Sim, Excluir") { _, _ ->
                    // Se o usuário confirmar, chama a função de exclusão
                    excluirClienteDoFirebase(cliente.id, position)
                }
                .setNegativeButton("Cancelar", null)
                .create()
                .show()

            return@setOnLongClickListener true
        }
    }

    private fun excluirClienteDoFirebase(clienteId: String, position: Int) {
        val db = FirebaseFirestore.getInstance()
        db.collection("clientes").document(clienteId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Cliente excluído com sucesso!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao excluir o cliente: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun atualizarLista(novaLista: List<Cliente>) {
        listaClientes = novaLista
        notifyDataSetChanged()
    }
}
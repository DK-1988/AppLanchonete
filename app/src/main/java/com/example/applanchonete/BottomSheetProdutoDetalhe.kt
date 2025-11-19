package com.example.applanchonete

import android.content.Context
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.NumberFormat
import java.util.Locale

class BottomSheetProdutoDetalhe(
    private val produto: Produto,
    private val grupos: List<ModificadorGrupo>
) : BottomSheetDialogFragment() {

    interface OnItemAddedListener {
        fun onItemAdded(item: ItemVenda)
    }

    private var listener: OnItemAddedListener? = null
    private lateinit var cgIngredientes: ChipGroup
    private lateinit var cgMolhos: ChipGroup
    private lateinit var cgExtras: ChipGroup
    private lateinit var tvQuantidade: TextView
    private lateinit var tvTotal: TextView
    private lateinit var btnAdicionar: com.google.android.material.button.MaterialButton
    private var quantidade = 0
    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    companion object {
        fun newInstance(produto: Produto, grupos: List<ModificadorGrupo>): BottomSheetProdutoDetalhe {
            return BottomSheetProdutoDetalhe(produto, grupos)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnItemAddedListener) {
            listener = context
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.bottomsheet_produto_detalhe, container, false)

        val tvNome = root.findViewById<TextView>(R.id.bs_tvNomeProduto)
        val tvPrecoBase = root.findViewById<TextView>(R.id.bs_tvPrecoProduto)
        cgIngredientes = root.findViewById(R.id.cgIngredientes)
        cgMolhos = root.findViewById(R.id.cgMolhos)
        cgExtras = root.findViewById(R.id.cgExtras)
        val btnMenos = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_btnMenos)
        val btnMais = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_btnMais)
        tvQuantidade = root.findViewById(R.id.bs_tvQuantidade)
        tvTotal = root.findViewById(R.id.bs_tvTotal)
        val btnCancelar = root.findViewById<com.google.android.material.button.MaterialButton>(R.id.bs_btnCancelar)
        btnAdicionar = root.findViewById(R.id.bs_btnAdicionar)
        val lblIngredientes = root.findViewById<TextView>(R.id.bs_lbl_ingredientes)
        val lblMolhos = root.findViewById<TextView>(R.id.bs_lbl_molhos)
        val lblExtras = root.findViewById<TextView>(R.id.bs_lbl_extras)

        tvNome.text = produto.nome
        tvPrecoBase.text = formatoMoeda.format(produto.precoVenda)

        val grupoIngredientes = grupos.getOrNull(0)
        val grupoMolhos = grupos.getOrNull(1)
        val grupoExtras = grupos.getOrNull(2)

        populateChipsForGroup(grupoIngredientes, cgIngredientes, lblIngredientes)
        populateChipsForGroup(grupoMolhos, cgMolhos, lblMolhos)
        populateChipsForGroup(grupoExtras, cgExtras, lblExtras)

        quantidade = 0
        tvQuantidade.text = quantidade.toString()
        updateTotalAndButton()

        btnMais.setOnClickListener {
            quantidade++
            tvQuantidade.text = quantidade.toString()
            updateTotalAndButton()
        }

        btnMenos.setOnClickListener {
            if (quantidade > 0) {
                quantidade--
                tvQuantidade.text = quantidade.toString()
                updateTotalAndButton()
            }
        }

        btnCancelar.setOnClickListener { dismiss() }

        btnAdicionar.setOnClickListener {
            if (quantidade <= 0) return@setOnClickListener

            val selecionadas = mutableListOf<ModificadorOpcao>()
            selecionadas.addAll(getSelectedOptionsFromChipGroup(cgIngredientes))
            selecionadas.addAll(getSelectedOptionsFromChipGroup(cgMolhos))
            selecionadas.addAll(getSelectedOptionsFromChipGroup(cgExtras))

            var precoAdicionais = 0.0
            var valido = true
            var mensagemErro = ""

            grupos.forEach { grupo ->
                val view = when(grupo.id) {
                    grupos.getOrNull(0)?.id -> cgIngredientes
                    grupos.getOrNull(1)?.id -> cgMolhos
                    grupos.getOrNull(2)?.id -> cgExtras
                    else -> null
                }

                var selecaoFeita = false
                if(view != null) {
                    selecaoFeita = getSelectedOptionsFromChipGroup(view).isNotEmpty()
                }

                if (grupo.obrigatorio && !selecaoFeita) {
                    valido = false
                    mensagemErro = "O grupo '${grupo.nome}' é obrigatório."
                }
            }

            if (!valido) {
                Toast.makeText(requireContext(), mensagemErro, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            precoAdicionais = selecionadas.sumOf { it.precoAdicional }
            val totalItem = (produto.precoVenda + precoAdicionais) * quantidade

            val nomeComposto = if (selecionadas.isNotEmpty()) {
                "${produto.nome} (${selecionadas.joinToString { it.nome }})"
            } else produto.nome

            val item = ItemVenda(
                produtoId = produto.id,
                nomeProduto = nomeComposto,
                quantidade = quantidade,
                precoUnitario = produto.precoVenda,
                precoCustoUnitario = produto.precoCusto,
                precoAdicionais = precoAdicionais,
                totalItem = totalItem,
                opcoesSelecionadas = selecionadas
            )

            val parent = (parentFragment ?: activity)
            if (parent is OnItemAddedListener) {
                parent.onItemAdded(item)
            } else {
                listener?.onItemAdded(item)
            }
            dismiss()
        }

        return root
    }

    private fun populateChipsForGroup(grupo: ModificadorGrupo?, chipGroup: ChipGroup, label: TextView) {
        chipGroup.removeAllViews()
        if (grupo == null) {
            label.visibility = View.GONE
            chipGroup.visibility = View.GONE
            return
        }

        label.text = grupo.nome
        chipGroup.isSingleSelection = grupo.tipoSelecao == "UNICA"

        for (op in grupo.opcoes) {
            val chip = Chip(requireContext())
            val precoStr = if(op.precoAdicional > 0) " (${formatoMoeda.format(op.precoAdicional)})" else ""
            chip.text = "${op.nome}$precoStr"
            chip.isCheckable = true
            chip.isClickable = true

            chip.chipBackgroundColor = null
            chip.setBackgroundDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.chips_pdv))
            chip.setTextColor(getChipTextColorStateList())

            chip.setOnCheckedChangeListener { _, _ ->
                updateTotalAndButton()
            }
            chip.tag = op
            chipGroup.addView(chip)
        }
    }

    private fun getSelectedOptionsFromChipGroup(chipGroup: ChipGroup): List<ModificadorOpcao> {
        val result = mutableListOf<ModificadorOpcao>()
        for (i in 0 until chipGroup.childCount) {
            val v = chipGroup.getChildAt(i)
            if (v is Chip) {
                if (v.isChecked) {
                    val tag = v.tag
                    if (tag is ModificadorOpcao) result.add(tag)
                }
            }
        }
        return result
    }

    private fun updateTotalAndButton() {
        val selecionadas = mutableListOf<ModificadorOpcao>()
        selecionadas.addAll(getSelectedOptionsFromChipGroup(cgIngredientes))
        selecionadas.addAll(getSelectedOptionsFromChipGroup(cgMolhos))
        selecionadas.addAll(getSelectedOptionsFromChipGroup(cgExtras))

        val precoAdicionais = selecionadas.sumOf { it.precoAdicional }
        val total = (produto.precoVenda + precoAdicionais) * quantidade
        tvTotal.text = "Total: ${formatoMoeda.format(total)}"

        btnAdicionar.isEnabled = quantidade > 0
    }

    private fun getChipTextColorStateList(): ColorStateList {
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )
        val colors = intArrayOf(
            ContextCompat.getColor(requireContext(), android.R.color.white),
            ContextCompat.getColor(requireContext(), android.R.color.black)
        )
        return ColorStateList(states, colors)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
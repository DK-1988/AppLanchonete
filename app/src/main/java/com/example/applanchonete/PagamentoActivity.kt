package com.example.applanchonete

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale
import java.util.ArrayList

class PagamentoActivity : AppCompatActivity(), PagamentoAdapter.OnPagamentoRemoveListener {

    private lateinit var tvTotal: TextView
    private lateinit var tvRestante: TextView
    private lateinit var spinnerFormaPagamento: Spinner
    private lateinit var etValorPagamento: EditText
    private lateinit var btnAdicionarPagamento: Button
    private lateinit var rvPagamentosAdicionados: RecyclerView
    private lateinit var btnFinalizarVenda: Button

    private lateinit var adapter: PagamentoAdapter
    private val listaPagamentos = mutableListOf<Pagamento>()

    private var totalVenda: Double = 0.0
    private var totalPago: Double = 0.0

    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pagamento)

        totalVenda = intent.getDoubleExtra("TOTAL_VENDA", 0.0)
        if (totalVenda == 0.0) {
            Toast.makeText(this, "Erro: Valor total da venda não encontrado.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        ligarComponentesUI()
        setupRecyclerView()
        configurarListeners()
        atualizarTotais()
    }

    private fun ligarComponentesUI() {
        tvTotal = findViewById(R.id.tvPagamentoTotal)
        tvRestante = findViewById(R.id.tvPagamentoRestante)
        spinnerFormaPagamento = findViewById(R.id.spinnerFormaPagamento)
        etValorPagamento = findViewById(R.id.etValorPagamento)
        btnAdicionarPagamento = findViewById(R.id.btnAdicionarPagamento)
        rvPagamentosAdicionados = findViewById(R.id.rvPagamentosAdicionados)
        btnFinalizarVenda = findViewById(R.id.btnFinalizarVendaPagamento)
    }

    private fun setupRecyclerView() {
        adapter = PagamentoAdapter(listaPagamentos, this)
        rvPagamentosAdicionados.adapter = adapter
        rvPagamentosAdicionados.layoutManager = LinearLayoutManager(this)
    }

    private fun configurarListeners() {
        btnAdicionarPagamento.setOnClickListener {
            adicionarPagamento()
        }

        btnFinalizarVenda.setOnClickListener {
            finalizarPagamento()
        }
    }

    private fun adicionarPagamento() {
        val forma = spinnerFormaPagamento.selectedItem.toString()
        val valorStr = etValorPagamento.text.toString()
        val valor = valorStr.toDoubleOrNull()

        val restante = totalVenda - totalPago

        if (valor == null || valor <= 0) {
            etValorPagamento.error = "Valor inválido"
            return
        }

        if (forma != "Dinheiro" && valor > restante && restante > 0.01) {
            etValorPagamento.error = "Valor não pode ser maior que o restante para esta forma."
            return
        }

        listaPagamentos.add(Pagamento(forma, valor))
        adapter.atualizarLista(listaPagamentos)

        atualizarTotais()

        etValorPagamento.text.clear()
        etValorPagamento.error = null
    }

    private fun atualizarTotais() {
        totalPago = listaPagamentos.sumOf { it.valor }
        val restante = totalVenda - totalPago

        tvTotal.text = formatoMoeda.format(totalVenda)

        if (restante > 0.009) {
            tvRestante.text = formatoMoeda.format(restante)
            tvRestante.setTextColor(Color.parseColor("#AA0000"))
            btnFinalizarVenda.isEnabled = false
            etValorPagamento.hint = "Faltam ${formatoMoeda.format(restante)}"
        } else {
            tvRestante.text = "Troco: ${formatoMoeda.format(restante * -1)}"
            tvRestante.setTextColor(Color.parseColor("#008800"))
            btnFinalizarVenda.isEnabled = true
            etValorPagamento.hint = "Valor a pagar"
        }
    }

    override fun onRemovePagamentoClicked(position: Int) {
        if (position >= 0 && position < listaPagamentos.size) {
            listaPagamentos.removeAt(position)
            adapter.atualizarLista(listaPagamentos)
            atualizarTotais()
        }
    }

    private fun finalizarPagamento() {
        val restante = totalVenda - totalPago

        if (restante > 0.009) {
            Toast.makeText(this, "Ainda falta ${formatoMoeda.format(restante)} para pagar.", Toast.LENGTH_SHORT).show()
            return
        }

        if (restante < -0.009) {
            val temPagamentoNaoDinheiro = listaPagamentos.any { it.forma != "Dinheiro" }
            if (temPagamentoNaoDinheiro) {
                Toast.makeText(this, "Não é possível dar troco em pagamentos com Cartão ou PIX.", Toast.LENGTH_LONG).show()
                return
            }
        }

        val resultIntent = Intent()
        resultIntent.putExtra("LISTA_PAGAMENTOS", ArrayList(listaPagamentos))
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}
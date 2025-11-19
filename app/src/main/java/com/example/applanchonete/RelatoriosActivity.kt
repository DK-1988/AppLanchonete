package com.example.applanchonete

import android.app.DatePickerDialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.graphics.Color
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.IOException

class RelatoriosActivity : AppCompatActivity() {

    private lateinit var etDataInicio: EditText
    private lateinit var etDataFim: EditText
    private lateinit var btnGerarRelatorioVendas: Button
    private lateinit var btnGerarMaisVendidos: Button
    private lateinit var btnGerarRelatorioCaixa: Button
    private lateinit var tvTotalRelatorio: TextView
    private lateinit var rvRelatorios: RecyclerView
    private lateinit var db: FirebaseFirestore
    private lateinit var fabExportarCSV: FloatingActionButton
    private lateinit var chartVendas: BarChart

    private lateinit var adapterRelatorioVendas: RelatorioVendasAdapter
    private lateinit var adapterMaisVendidos: MaisVendidosAdapter
    private lateinit var adapterRelatorioCaixa: RelatorioCaixaAdapter

    private val listaVendasRelatorio = mutableListOf<Venda>()
    private val listaMaisVendidosRelatorio = mutableListOf<ProdutoVendidoInfo>()
    private val listaCaixaRelatorio = mutableListOf<SessaoCaixa>()

    private var dataInicioSelecionada: Date? = null
    private var dataFimSelecionada: Date? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private val formatoMoeda = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))

    private lateinit var exportarCsvLauncher: ActivityResultLauncher<String>
    private var csvContent: String = ""
    private var relatorioAtual: String = "VENDAS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_relatorios)

        db = FirebaseFirestore.getInstance()

        ligarComponentesUI()
        setupRecyclerViewEAdapters()
        setupCsvLauncher()
        setupGraficoVendas()
        configurarBotoesData()
        configurarBotoesRelatorio()
    }

    private fun ligarComponentesUI() {
        etDataInicio = findViewById(R.id.etDataInicio)
        etDataFim = findViewById(R.id.etDataFim)
        btnGerarRelatorioVendas = findViewById(R.id.btnGerarRelatorioVendas)
        btnGerarMaisVendidos = findViewById(R.id.btnGerarMaisVendidos)
        btnGerarRelatorioCaixa = findViewById(R.id.btnGerarRelatorioCaixa)
        tvTotalRelatorio = findViewById(R.id.tvTotalRelatorio)
        rvRelatorios = findViewById(R.id.rvRelatorios)
        fabExportarCSV = findViewById(R.id.fabExportarCSV)
        chartVendas = findViewById(R.id.chartVendas)
    }

    private fun configurarBotoesData() {
        etDataInicio.setOnClickListener { mostrarSeletorDeData(true) }
        etDataFim.setOnClickListener { mostrarSeletorDeData(false) }
    }

    private fun configurarBotoesRelatorio() {
        btnGerarRelatorioVendas.setOnClickListener {
            if (!validarDatas()) return@setOnClickListener
            relatorioAtual = "VENDAS"
            rvRelatorios.adapter = adapterRelatorioVendas
            tvTotalRelatorio.visibility = View.VISIBLE
            chartVendas.visibility = View.VISIBLE
            buscarVendasPorPeriodo(dataInicioSelecionada!!, dataFimSelecionada!!)
        }

        btnGerarMaisVendidos.setOnClickListener {
            if (!validarDatas()) return@setOnClickListener
            relatorioAtual = "MAIS_VENDIDOS"
            rvRelatorios.adapter = adapterMaisVendidos
            tvTotalRelatorio.visibility = View.GONE
            chartVendas.visibility = View.GONE
            buscarMaisVendidosPorPeriodo(dataInicioSelecionada!!, dataFimSelecionada!!)
        }

        btnGerarRelatorioCaixa.setOnClickListener {
            if (!validarDatas()) return@setOnClickListener
            relatorioAtual = "CAIXA"
            rvRelatorios.adapter = adapterRelatorioCaixa
            tvTotalRelatorio.visibility = View.GONE
            chartVendas.visibility = View.GONE
            buscarSessoesCaixaPorPeriodo(dataInicioSelecionada!!, dataFimSelecionada!!)
        }

        fabExportarCSV.setOnClickListener {
            iniciarExportacaoCSV()
        }
    }

    private fun validarDatas(): Boolean {
        if (dataInicioSelecionada == null || dataFimSelecionada == null) {
            Toast.makeText(this, "Por favor, selecione a data de início e fim.", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun setupRecyclerViewEAdapters() {
        adapterRelatorioVendas = RelatorioVendasAdapter(listaVendasRelatorio)
        adapterMaisVendidos = MaisVendidosAdapter(listaMaisVendidosRelatorio)
        adapterRelatorioCaixa = RelatorioCaixaAdapter(listaCaixaRelatorio)
        rvRelatorios.adapter = adapterRelatorioVendas
        rvRelatorios.layoutManager = LinearLayoutManager(this)
    }

    private fun mostrarSeletorDeData(isDataInicio: Boolean) {
        val calendario = Calendar.getInstance()
        val ano = calendario.get(Calendar.YEAR); val mes = calendario.get(Calendar.MONTH); val dia = calendario.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(this, { _, anoSel, mesSel, diaSel ->
            val calSel = Calendar.getInstance(); calSel.set(anoSel, mesSel, diaSel); val dataSel = calSel.time
            val dataFmt = sdf.format(dataSel)
            if (isDataInicio) { etDataInicio.setText(dataFmt); dataInicioSelecionada = ajustarHoraParaInicioDoDia(dataSel) }
            else { etDataFim.setText(dataFmt); dataFimSelecionada = ajustarHoraParaFimDoDia(dataSel) }
        }, ano, mes, dia
        )
        datePickerDialog.show()
    }

    private fun ajustarHoraParaInicioDoDia(date: Date): Date {
        val calendar = Calendar.getInstance(); calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        return calendar.time
    }
    private fun ajustarHoraParaFimDoDia(date: Date): Date {
        val calendar = Calendar.getInstance(); calendar.time = date
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        return calendar.time
    }

    private fun buscarVendasPorPeriodo(dataInicio: Date, dataFim: Date) {
        listaVendasRelatorio.clear()
        adapterRelatorioVendas.atualizarLista(listaVendasRelatorio)
        tvTotalRelatorio.text = "Total Faturado: R$ 0,00"

        db.collection("vendas").whereGreaterThanOrEqualTo("dataHora", dataInicio).whereLessThanOrEqualTo("dataHora", dataFim).orderBy("dataHora", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { querySnapshot ->
                var totalFaturadoPeriodo = 0.0
                if (querySnapshot.isEmpty) { Toast.makeText(this, "Nenhuma venda encontrada.", Toast.LENGTH_SHORT).show() }
                for (document in querySnapshot.documents) {
                    val venda = document.toObject(Venda::class.java)
                    if (venda != null) {
                        venda.id = document.id
                        listaVendasRelatorio.add(venda)
                        totalFaturadoPeriodo += venda.totalVenda
                    }
                }
                tvTotalRelatorio.text = "Total Faturado: ${formatoMoeda.format(totalFaturadoPeriodo)}"
                adapterRelatorioVendas.atualizarLista(listaVendasRelatorio)
                atualizarGraficoVendas(listaVendasRelatorio)
            }
            .addOnFailureListener { e -> Log.e("RelatoriosActivity", "Erro buscar vendas", e) }
    }

    private fun buscarMaisVendidosPorPeriodo(dataInicio: Date, dataFim: Date) {
        listaMaisVendidosRelatorio.clear()
        adapterMaisVendidos.atualizarLista(listaMaisVendidosRelatorio)

        db.collection("vendas").whereGreaterThanOrEqualTo("dataHora", dataInicio).whereLessThanOrEqualTo("dataHora", dataFim).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) { Toast.makeText(this, "Nenhuma venda encontrada.", Toast.LENGTH_SHORT).show(); return@addOnSuccessListener }
                data class AgregadoProduto(var quantidade: Int = 0, var lucro: Double = 0.0)
                val mapaProdutos = mutableMapOf<String, AgregadoProduto>()
                for (document in querySnapshot.documents) {
                    val venda = document.toObject(Venda::class.java)
                    venda?.itens?.forEach { item ->
                        val lucroDoItem = (item.precoUnitario - item.precoCustoUnitario) * item.quantidade
                        val agregado = mapaProdutos.getOrPut(item.nomeProduto) { AgregadoProduto() }
                        agregado.quantidade += item.quantidade
                        agregado.lucro += lucroDoItem
                    }
                }
                val listaOrdenada = mapaProdutos.map { (nome, agregado) -> ProdutoVendidoInfo(nome, agregado.quantidade, agregado.lucro) }
                    .sortedByDescending { it.quantidade }

                listaMaisVendidosRelatorio.addAll(listaOrdenada)
                adapterMaisVendidos.atualizarLista(listaMaisVendidosRelatorio)
            }
            .addOnFailureListener { e -> Log.e("RelatoriosActivity", "Erro buscar mais vendidos", e) }
    }

    private fun buscarSessoesCaixaPorPeriodo(dataInicio: Date, dataFim: Date) {
        listaCaixaRelatorio.clear()
        adapterRelatorioCaixa.atualizarLista(listaCaixaRelatorio)

        db.collection("sessoes_caixa").whereGreaterThanOrEqualTo("dataAbertura", dataInicio).whereLessThanOrEqualTo("dataAbertura", dataFim).orderBy("dataAbertura", Query.Direction.DESCENDING).get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) { Toast.makeText(this, "Nenhuma sessão encontrada.", Toast.LENGTH_SHORT).show() }
                for (document in querySnapshot.documents) {
                    val sessao = document.toObject(SessaoCaixa::class.java)
                    if (sessao != null) {
                        sessao.id = document.id
                        listaCaixaRelatorio.add(sessao)
                    }
                }
                adapterRelatorioCaixa.atualizarLista(listaCaixaRelatorio)
            }
            .addOnFailureListener { e -> Log.e("RelatoriosActivity", "Erro buscar sessões", e) }
    }

    private fun setupGraficoVendas() {
        chartVendas.description.isEnabled = false; chartVendas.legend.isEnabled = false
        chartVendas.setDrawGridBackground(false); chartVendas.setDrawBarShadow(false); chartVendas.setPinchZoom(false)
        val xAxis = chartVendas.xAxis; xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false); xAxis.textColor = Color.BLACK; xAxis.granularity = 1f
        val leftAxis = chartVendas.axisLeft; leftAxis.textColor = Color.BLACK; leftAxis.axisMinimum = 0f
        chartVendas.axisRight.isEnabled = false
    }

    private fun atualizarGraficoVendas(lista: List<Venda>) {
        if (lista.isEmpty()) { chartVendas.clear(); chartVendas.invalidate(); return }
        val sdfDiaMes = SimpleDateFormat("dd/MM", Locale.getDefault())
        val vendasAgrupadas = lista.filter { it.dataHora != null }.groupBy { sdfDiaMes.format(it.dataHora!!.toDate()) }
        val vendasPorDia = mutableMapOf<String, Double>()
        vendasAgrupadas.forEach { (dia, listaDeVendas) -> vendasPorDia[dia] = listaDeVendas.sumOf { it.totalVenda } }
        val vendasOrdenadas = vendasPorDia.entries.sortedBy { val p = it.key.split("/"); "${p[1]}${p[0]}" }
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        vendasOrdenadas.forEachIndexed { index, (dia, total) ->
            entries.add(BarEntry(index.toFloat(), total.toFloat())); labels.add(dia)
        }
        val dataSet = BarDataSet(entries, "Vendas por Dia"); dataSet.color = Color.parseColor("#FF6200EE")
        dataSet.valueTextColor = Color.BLACK; dataSet.valueTextSize = 10f
        chartVendas.xAxis.valueFormatter = IndexAxisValueFormatter(labels); chartVendas.xAxis.labelCount = labels.size
        val barData = BarData(dataSet); barData.barWidth = 0.5f
        chartVendas.data = barData; chartVendas.setFitBars(true); chartVendas.invalidate(); chartVendas.animateY(1000)
    }

    private fun setupCsvLauncher() {
        exportarCsvLauncher = registerForActivityResult(
            ActivityResultContracts.CreateDocument("text/csv")
        ) { uri ->
            if (uri != null) {
                writeCsvToFile(uri, csvContent)
            } else {
                Toast.makeText(this, "Exportação cancelada.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun iniciarExportacaoCSV() {
        if (dataInicioSelecionada == null || dataFimSelecionada == null) {
            Toast.makeText(this, "Selecione um período antes de exportar.", Toast.LENGTH_SHORT).show()
            return
        }

        var fileName = "relatorio.csv"

        when (relatorioAtual) {
            "VENDAS" -> {
                if (listaVendasRelatorio.isEmpty()) { Toast.makeText(this, "Não há dados para exportar.", Toast.LENGTH_SHORT).show(); return }
                csvContent = gerarCsvVendas(listaVendasRelatorio)
                fileName = "relatorio_vendas_${sdf.format(Date())}.csv"
            }
            "MAIS_VENDIDOS" -> {
                if (listaMaisVendidosRelatorio.isEmpty()) { Toast.makeText(this, "Não há dados para exportar.", Toast.LENGTH_SHORT).show(); return }
                csvContent = gerarCsvMaisVendidos(listaMaisVendidosRelatorio)
                fileName = "relatorio_mais_vendidos_${sdf.format(Date())}.csv"
            }
            "CAIXA" -> {
                if (listaCaixaRelatorio.isEmpty()) { Toast.makeText(this, "Não há dados para exportar.", Toast.LENGTH_SHORT).show(); return }
                csvContent = gerarCsvCaixa(listaCaixaRelatorio)
                fileName = "relatorio_caixa_${sdf.format(Date())}.csv"
            }
        }

        exportarCsvLauncher.launch(fileName)
    }

    private fun writeCsvToFile(uri: Uri, content: String) {
        try {
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(content.toByteArray())
            }
            Toast.makeText(this, "Relatório exportado com sucesso!", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e("RelatoriosActivity", "Erro ao escrever arquivo CSV", e)
            Toast.makeText(this, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun gerarCsvVendas(lista: List<Venda>): String {
        val builder = StringBuilder()
        val separador = ";"

        builder.appendLine("Data${separador}ID Venda${separador}Cliente${separador}Total Vendido${separador}Total Custo${separador}Lucro${separador}Pagamentos${separador}ID Caixa")

        val sdfCsv = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        lista.forEach { venda ->
            val data = sdfCsv.format(venda.dataHora?.toDate() ?: Date())

            val pagamentosStr = venda.pagamentos.joinToString(", ") {
                "${it.forma} (${formatoMoeda.format(it.valor)})"
            }.replace(";", ",")

            builder.appendLine(
                "${data}${separador}" +
                        "${venda.id}${separador}" +
                        "${venda.clienteNome.replace(";", "")}${separador}" +
                        "${venda.totalVenda}${separador}" +
                        "${venda.totalCusto}${separador}" +
                        "${venda.lucroTotal}${separador}" +
                        "\"${pagamentosStr}\"${separador}" + // Coloca entre aspas
                        venda.sessaoCaixaId
            )
        }
        return builder.toString()
    }

    private fun gerarCsvMaisVendidos(lista: List<ProdutoVendidoInfo>): String {
        val builder = StringBuilder()
        val separador = ";"
        builder.appendLine("Produto${separador}Quantidade Vendida${separador}Lucro Total")

        lista.forEach { produto ->
            builder.appendLine(
                "${produto.nome.replace(";", "")}${separador}" +
                        "${produto.quantidade}${separador}" +
                        produto.lucroTotal
            )
        }
        return builder.toString()
    }

    private fun gerarCsvCaixa(lista: List<SessaoCaixa>): String {
        val builder = StringBuilder()
        val separador = ";"
        val sdfCsv = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        builder.appendLine("Status${separador}Usuario${separador}Abertura${separador}Fechamento${separador}Vlr Inicial${separador}Dinheiro${separador}PIX${separador}Credito${separador}Debito${separador}Total Vendas")

        lista.forEach { sessao ->
            val dataAbertura = sdfCsv.format(sessao.dataAbertura?.toDate() ?: Date())
            val dataFechamento = if (sessao.dataFechamento != null) sdfCsv.format(sessao.dataFechamento!!.toDate()) else ""

            builder.appendLine(
                "${sessao.status}${separador}" +
                        "${sessao.usuarioEmail}${separador}" +
                        "${dataAbertura}${separador}" +
                        "${dataFechamento}${separador}" +
                        "${sessao.valorInicial}${separador}" +
                        "${sessao.totalDinheiro}${separador}" +
                        "${sessao.totalPix}${separador}" +
                        "${sessao.totalCartaoCredito}${separador}" +
                        "${sessao.totalCartaoDebito}${separador}" +
                        sessao.valorTotalVendas
            )
        }
        return builder.toString()
    }
}
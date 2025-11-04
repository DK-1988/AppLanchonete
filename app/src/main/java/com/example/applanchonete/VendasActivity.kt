package com.example.applanchonete

import androidx.appcompat.app.AlertDialog
import java.text.NumberFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ListenerRegistration
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.graphics.Typeface
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.ArrayList

class VendasActivity : AppCompatActivity(),
    ProdutosVendaAdapter.OnProdutoVendaClickListener,
    CarrinhoVendaAdapter.OnCarrinhoInteractionListener,
    SetorFiltroAdapter.OnSetorFiltroClickListener,
    BottomSheetProdutoDetalhe.OnItemAddedListener {

    private val TAG = "VendasActivity"
    private val db = FirebaseFirestore.getInstance()
    private lateinit var auth: FirebaseAuth
    private val vendasViewModel: VendasViewModelRefatorado by viewModels()

    private var produtosListener: ListenerRegistration? = null
    private var setoresListener: ListenerRegistration? = null
    private var modificadoresListener: ListenerRegistration? = null

    private lateinit var rvSetorFiltro: RecyclerView
    private lateinit var adapterSetorFiltro: SetorFiltroAdapter
    private val listaSetoresFiltro = mutableListOf<Setor>()

    private lateinit var rvProdutosDisponiveis: RecyclerView
    private lateinit var produtosAdapter: ProdutosVendaAdapter
    private lateinit var rvCarrinho: RecyclerView
    private lateinit var carrinhoAdapter: CarrinhoVendaAdapter
    private lateinit var tvTotalVenda: TextView
    private lateinit var btnFinalizarVenda: Button

    private lateinit var btnFecharCaixa: Button
    private lateinit var tvClienteSelecionado: TextView
    private lateinit var btnSelecionarCliente: Button
    private var clienteIdSelecionado: String = ""
    private var clienteNomeSelecionado: String = "Cliente Padrão"

    private val listaMestraProdutos = mutableListOf<Produto>()
    private val listaMestraModificadores = mutableListOf<ModificadorGrupo>()
    private val listaProdutosFiltrados = mutableListOf<Produto>()

    private var sessaoCaixaId: String? = null
    private var progressDialog: AlertDialog? = null

    private lateinit var selecionarClienteLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestBluetoothConnectLauncher: ActivityResultLauncher<String>
    private lateinit var pagamentoLauncher: androidx.activity.result.ActivityResultLauncher<android.content.Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vendas)

        auth = Firebase.auth
        ligarComponentesUI()
        setupAdapters()
        setupRecyclerViews()
        setupActivityResultLaunchers()
        configurarObservadoresViewModel()
        configurarBotoes()

        startListeningSetores()
        startListeningProdutos()
        startListeningModificadores()

        lifecycleScope.launch(Dispatchers.IO) {
            verificarSessaoCaixa()
        }
    }

    private fun ligarComponentesUI() {
        rvProdutosDisponiveis = findViewById(R.id.rvProdutosDisponiveis)
        rvCarrinho = findViewById(R.id.rvCarrinho)
        tvTotalVenda = findViewById(R.id.tvTotalVenda)
        btnFinalizarVenda = findViewById(R.id.btnFinalizarVenda)
        rvSetorFiltro = findViewById(R.id.rvSetorFiltro)
        btnFecharCaixa = findViewById(R.id.btnFecharCaixa)
        tvClienteSelecionado = findViewById(R.id.tvClienteSelecionado)
        btnSelecionarCliente = findViewById(R.id.btnSelecionarCliente)
        tvClienteSelecionado.text = clienteNomeSelecionado
        btnFinalizarVenda.text = "Revisar Pedido"
    }

    private fun setupAdapters() {
        adapterSetorFiltro = SetorFiltroAdapter(listaSetoresFiltro, this)
        produtosAdapter = ProdutosVendaAdapter(this)
        carrinhoAdapter = CarrinhoVendaAdapter(this)
    }

    private fun setupRecyclerViews() {
        rvSetorFiltro.apply {
            adapter = adapterSetorFiltro
            layoutManager = LinearLayoutManager(this@VendasActivity, LinearLayoutManager.HORIZONTAL, false)
        }
        rvProdutosDisponiveis.apply {
            adapter = produtosAdapter
            layoutManager = LinearLayoutManager(this@VendasActivity)
        }
        rvCarrinho.apply {
            adapter = carrinhoAdapter
            layoutManager = LinearLayoutManager(this@VendasActivity)
        }
    }

    private fun setupActivityResultLaunchers() {
        // --- ESTA É A CORREÇÃO ---
        // A lógica foi re-adicionada
        pagamentoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                @Suppress("UNCHECKED_CAST", "DEPRECATION")
                val listaPagamentos = data?.getSerializableExtra("LISTA_PAGAMENTOS") as? ArrayList<Pagamento>

                if (listaPagamentos.isNullOrEmpty()) {
                    Toast.makeText(this, "Erro ao processar pagamento.", Toast.LENGTH_SHORT).show()
                } else {
                    // Chama a função que processa a venda
                    verificarPermissaoEProcessar(listaPagamentos)
                }
            }
        }
        // --- FIM DA CORREÇÃO ---

        selecionarClienteLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                clienteIdSelecionado = data?.getStringExtra("CLIENTE_ID_RESULTADO") ?: ""
                clienteNomeSelecionado = data?.getStringExtra("CLIENTE_NOME_RESULTADO") ?: "Cliente Padrão"
                tvClienteSelecionado.text = clienteNomeSelecionado
            }
        }

        requestBluetoothConnectLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) { Toast.makeText(this, "Permissão Bluetooth concedida.", Toast.LENGTH_SHORT).show() }
            else { Toast.makeText(this, "Permissão Bluetooth negada.", Toast.LENGTH_LONG).show() }
        }
    }

    private fun configurarObservadoresViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    vendasViewModel.progressoTexto.collectLatest { texto ->
                        if (texto.isNullOrBlank()) esconderProgresso() else mostrarProgresso(texto)
                    }
                }

                launch {
                    vendasViewModel.erro.collectLatest { msg ->
                        esconderProgresso()
                        Toast.makeText(this@VendasActivity, "Erro: $msg", Toast.LENGTH_LONG).show()
                    }
                }
                launch {
                    vendasViewModel.sucesso.collectLatest {
                        esconderProgresso()
                        Toast.makeText(this@VendasActivity, "Venda finalizada com sucesso!", Toast.LENGTH_LONG).show()

                        clienteIdSelecionado = ""
                        clienteNomeSelecionado = "Cliente Padrão"
                        tvClienteSelecionado.text = clienteNomeSelecionado
                        // O carrinho será limpo pelo ViewModel (chamando 'limparCarrinho()')
                    }
                }
                launch {
                    vendasViewModel.impressaoConcluida.collectLatest {
                        Log.d(TAG, "Impressão concluída")
                    }
                }
                launch {
                    vendasViewModel.carrinhoUiState.collectLatest { state ->
                        val f = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
                        tvTotalVenda.text = "Total: ${f.format(state.total)}"
                        carrinhoAdapter.atualizarLista(state.itens)
                    }
                }
            }
        }
    }

    private fun configurarBotoes() {
        btnFinalizarVenda.setOnClickListener {
            val carrinhoAtual = vendasViewModel.carrinho.value
            if (carrinhoAtual.isEmpty()) {
                Toast.makeText(this, "O carrinho está vazio.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (sessaoCaixaId.isNullOrBlank()) {
                Toast.makeText(this, "Caixa não está aberto. Aguarde...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                val total = withContext(Dispatchers.Default) { carrinhoAtual.sumOf { it.totalItem } }
                val intent = Intent(this@VendasActivity, PagamentoActivity::class.java)

                intent.putExtra("TOTAL_VENDA", total)
                intent.putExtra("CLIENTE_ID", clienteIdSelecionado)
                intent.putExtra("CLIENTE_NOME", clienteNomeSelecionado)
                intent.putExtra("SESSAO_CAIXA_ID", sessaoCaixaId)

                pagamentoLauncher.launch(intent)
            }
        }

        btnFecharCaixa.setOnClickListener {
            if (sessaoCaixaId == null) { Toast.makeText(this, "Nenhum caixa aberto.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            AlertDialog.Builder(this)
                .setTitle("Fechar Caixa").setMessage("Tem certeza?")
                .setPositiveButton("Sim, Fechar") { _, _ -> calcularEFecharCaixa() }
                .setNegativeButton("Cancelar", null).show()
        }
        btnSelecionarCliente.setOnClickListener {
            val intent = Intent(this, SelecionarClienteActivity::class.java)
            selecionarClienteLauncher.launch(intent)
        }
    }

    private fun startListeningSetores() {
        setoresListener?.remove()
        setoresListener = db.collection("setores").orderBy("nome")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e(TAG, "Erro listener setores: ${error.message}", error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener

                lifecycleScope.launch {
                    val setores = withContext(Dispatchers.Default) {
                        snapshot.documents.mapNotNull { doc -> doc.toObject(Setor::class.java)?.apply { id = doc.id } }
                    }
                    listaSetoresFiltro.clear()
                    listaSetoresFiltro.add(Setor(id = "todos", nome = "Todos"))
                    listaSetoresFiltro.addAll(setores)
                    adapterSetorFiltro.atualizarLista(listaSetoresFiltro)
                }
            }
    }

    private fun startListeningProdutos() {
        produtosListener?.remove()
        produtosListener = db.collection("produtos").orderBy("nome")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e(TAG, "Erro listener produtos: ${error.message}", error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener

                lifecycleScope.launch {
                    val produtos = withContext(Dispatchers.Default) {
                        snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Produto::class.java)?.apply { id = doc.id }
                        }
                    }
                    listaMestraProdutos.clear()
                    listaMestraProdutos.addAll(produtos)

                    listaProdutosFiltrados.clear()
                    listaProdutosFiltrados.addAll(listaMestraProdutos)
                    produtosAdapter.atualizarLista(listaProdutosFiltrados)
                }
            }
    }

    private fun startListeningModificadores() {
        modificadoresListener?.remove()
        modificadoresListener = db.collection("modificadores").orderBy("nome")
            .addSnapshotListener { snapshot, error ->
                if (error != null) { Log.e(TAG, "Erro listener modificadores: ${error.message}", error); return@addSnapshotListener }
                if (snapshot == null) return@addSnapshotListener

                lifecycleScope.launch {
                    val mods = withContext(Dispatchers.Default) {
                        snapshot.documents.mapNotNull { doc ->
                            doc.toObject(ModificadorGrupo::class.java)?.apply { id = doc.id }
                        }
                    }
                    listaMestraModificadores.clear()
                    listaMestraModificadores.addAll(mods)
                }
            }
    }

    override fun onSetorFiltroClicked(nomeSetor: String) {
        listaProdutosFiltrados.clear()
        if (nomeSetor == "Todos") listaProdutosFiltrados.addAll(listaMestraProdutos)
        else listaProdutosFiltrados.addAll(listaMestraProdutos.filter { it.setor == nomeSetor })
        produtosAdapter.atualizarLista(listaProdutosFiltrados)
    }

    override fun onProdutoClicked(produto: Produto) {
        val grupos = listaMestraModificadores.filter { produto.modificadorGrupoIds.contains(it.id) }
        val sheet = BottomSheetProdutoDetalhe.newInstance(produto, grupos)
        sheet.show(supportFragmentManager, "BottomSheetProdutoDetalhe")
    }

    override fun onAumentarQuantidade(position: Int) {
        vendasViewModel.aumentarQuantidade(position)
    }
    override fun onDiminuirQuantidade(position: Int) {
        vendasViewModel.diminuirQuantidade(position)
    }
    override fun onRemoverItem(position: Int) {
        vendasViewModel.removerItem(position)
    }

    override fun onItemAdded(item: ItemVenda) {
        vendasViewModel.adicionarOuMesclarItem(item)
    }

    private fun verificarPermissaoEProcessar(listaPagamentos: List<Pagamento>) {
        val prefs = getSharedPreferences("AppLanchonetePrefs", Context.MODE_PRIVATE)
        val impressoraAddress = prefs.getString("impressora_bluetooth_address", null)
        val precisaImprimir = !impressoraAddress.isNullOrBlank()
        var imprimirPermitido = precisaImprimir

        if (precisaImprimir && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) {
                imprimirPermitido = false
                requestBluetoothConnectLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }
        executarProcessarVenda(listaPagamentos, imprimirPermitido)
    }

    private fun executarProcessarVenda(listaPagamentos: List<Pagamento>, imprimir: Boolean) {
        val sessaoId = sessaoCaixaId
        if (sessaoId.isNullOrBlank()) {
            Toast.makeText(this, "Erro: Caixa não aberto.", Toast.LENGTH_LONG).show()
            return
        }

        vendasViewModel.processarVenda(
            clienteId = clienteIdSelecionado,
            clienteNome = clienteNomeSelecionado,
            sessaoCaixaId = sessaoId,
            listaPagamentos = listaPagamentos,
            imprimir = imprimir
        )
    }

    private suspend fun verificarSessaoCaixa() {
        val usuario = auth.currentUser
        if (usuario == null) {
            withContext(Dispatchers.Main) {
                Toast.makeText(this@VendasActivity, "Erro: Usuário não logado.", Toast.LENGTH_LONG).show()
                finish()
            }
            return
        }

        try {
            val snapshot = withContext(Dispatchers.IO) {
                db.collection("sessoes_caixa")
                    .whereEqualTo("usuarioEmail", usuario.email)
                    .whereEqualTo("status", "Aberto")
                    .limit(1).get().await()
            }

            withContext(Dispatchers.Main) {
                if (snapshot.isEmpty) {
                    mostrarDialogoAberturaCaixa()
                } else {
                    val sessao = snapshot.documents[0].toObject(SessaoCaixa::class.java)
                    sessaoCaixaId = snapshot.documents[0].id
                    val valorFormatado = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(sessao?.valorInicial)
                    Toast.makeText(this@VendasActivity, "Sessão retomada (Fundo: $valorFormatado)", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Log.e(TAG, "Erro ao verificar caixa:", e)
                Toast.makeText(this@VendasActivity, "Erro ao verificar caixa: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun mostrarDialogoAberturaCaixa() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Abrir Caixa"); builder.setMessage("Insira o valor inicial (fundo de caixa/troco):")
        val input = EditText(this); input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        val container = LinearLayout(this); val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.setMargins(50, 20, 50, 20); input.layoutParams = params; container.addView(input); builder.setView(container)
        builder.setPositiveButton("Abrir") { _, _ ->
            val valorInicial = input.text.toString().toDoubleOrNull()
            if (valorInicial == null || valorInicial < 0) { Toast.makeText(this, "Valor inválido.", Toast.LENGTH_SHORT).show(); mostrarDialogoAberturaCaixa() }
            else { abrirNovoCaixa(valorInicial) }
        }
        builder.setNegativeButton("Sair do PDV") { dialog, _ -> dialog.dismiss(); finish() }
        builder.setCancelable(false); builder.show()
    }

    private fun abrirNovoCaixa(valorInicial: Double) {
        val email = auth.currentUser?.email ?: run { Toast.makeText(this, "Erro: Usuário não identificado.", Toast.LENGTH_SHORT).show(); finish(); return }
        val novaSessao = SessaoCaixa(usuarioEmail = email, valorInicial = valorInicial, status = "Aberto")

        mostrarProgresso("Abrindo caixa...")
        db.collection("sessoes_caixa").add(novaSessao)
            .addOnSuccessListener { documentRef ->
                esconderProgresso()
                sessaoCaixaId = documentRef.id
                val valorFormatado = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valorInicial)
                Toast.makeText(this, "Caixa aberto com $valorFormatado", Toast.LENGTH_LONG).show()
            }.addOnFailureListener { e ->
                esconderProgresso()
                Toast.makeText(this, "Erro fatal ao abrir caixa: ${e.message}", Toast.LENGTH_LONG).show(); finish()
            }
    }

    private fun calcularEFecharCaixa() {
        if (sessaoCaixaId == null) return
        mostrarProgresso("Calculando totais...")

        db.collection("vendas").whereEqualTo("sessaoCaixaId", sessaoCaixaId).get()
            .addOnSuccessListener { snapshotVendas ->
                var totalVendidoNaSessao = snapshotVendas.documents.sumOf { doc -> doc.toObject(Venda::class.java)?.totalVenda ?: 0.0 }

                db.collection("sessoes_caixa").document(sessaoCaixaId!!).get()
                    .addOnSuccessListener { documentSessao ->
                        esconderProgresso()
                        val sessao = documentSessao.toObject(SessaoCaixa::class.java) ?: run { Toast.makeText(this, "Erro: Sessão não encontrada.", Toast.LENGTH_SHORT).show(); return@addOnSuccessListener }
                        val valorInicial = sessao.valorInicial
                        val valorFinalCalculado = valorInicial + totalVendidoNaSessao
                        mostrarDialogoResumoCaixa(valorInicial, totalVendidoNaSessao, valorFinalCalculado)
                    }
                    .addOnFailureListener { e ->
                        esconderProgresso(); Toast.makeText(this, "Erro ao buscar sessão: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }.addOnFailureListener { e ->
                esconderProgresso(); Toast.makeText(this, "Erro ao calcular vendas: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarDialogoResumoCaixa(inicial: Double, vendido: Double, final: Double) {
        val f = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val msg = "Resumo:\n\nInicial: ${f.format(inicial)}\nVendido: ${f.format(vendido)}\n------------------\nEsperado: ${f.format(final)}\n\nConfirmar fechamento?"
        AlertDialog.Builder(this).setTitle("Confirmar Fechamento").setMessage(msg).setCancelable(false)
            .setPositiveButton("Confirmar e Fechar") { _, _ -> finalizarSessaoNoFirebase(vendido, final) }
            .setNegativeButton("Cancelar", null).create().show()
    }

    private fun finalizarSessaoNoFirebase(totalVendido: Double, totalFinal: Double) {
        if (sessaoCaixaId == null) return
        mostrarProgresso("Fechando caixa...")

        val atualizacoes = mapOf("status" to "Fechado", "dataFechamento" to Timestamp(Date()), "valorTotalVendas" to totalVendido, "valorFinalCalculado" to totalFinal)
        db.collection("sessoes_caixa").document(sessaoCaixaId!!).update(atualizacoes)
            .addOnSuccessListener {
                esconderProgresso()
                Toast.makeText(this, "Caixa fechado com sucesso!", Toast.LENGTH_LONG).show()
                sessaoCaixaId = null
                finish()
            }
            .addOnFailureListener { e ->
                esconderProgresso()
                Toast.makeText(this, "Erro ao atualizar sessão: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun mostrarProgresso(mensagem: String) {
        runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = AlertDialog.Builder(this)
                .setMessage(mensagem)
                .setCancelable(false)
                .show()
        }
    }
    private fun esconderProgresso() {
        runOnUiThread {
            progressDialog?.dismiss()
            progressDialog = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        produtosListener?.remove(); produtosListener = null
        setoresListener?.remove(); setoresListener = null
        modificadoresListener?.remove(); modificadoresListener = null
    }
}
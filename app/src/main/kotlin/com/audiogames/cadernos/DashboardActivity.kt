package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONObject

class DashboardActivity : Activity() {

    private lateinit var lista: RecyclerView
    private lateinit var adaptador: AdaptadorCadernos
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var containerVazio: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
        verificarAtualizacao(manual = false)
        pedirPermissaoNotificacao()
        CadernosMessagingService.registrarTokenAtual()
    }

    private fun pedirPermissaoNotificacao() {
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            val permissao = android.Manifest.permission.POST_NOTIFICATIONS
            if (checkSelfPermission(permissao) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(permissao), 1001)
            }
        }
    }

    private fun verificarAtualizacao(manual: Boolean) {
        if (manual) mostrarAviso(this, "Verificando atualizações...")
        UpdateManager(this).verificarSilenciosamente { atualizacao ->
            AlertDialog.Builder(contextoTema(this))
                .setTitle("Nova versão disponível")
                .setMessage("Versão ${atualizacao.versionName} já está pronta. Baixar e instalar agora?")
                .setPositiveButton("Atualizar") { _, _ ->
                    UpdateManager(this).baixarEInstalar(
                        atualizacao,
                        onProgresso = { mensagem -> mostrarAviso(this, mensagem) },
                        onErro = { mensagem -> mostrarErro(this, mensagem) }
                    )
                }
                .setNegativeButton("Agora não", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::adaptador.isInitialized) carregarCadernos()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Meus Cadernos", mostrarVoltar = false)

        val acoes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(this@DashboardActivity, 12), dp(this@DashboardActivity, 10), dp(this@DashboardActivity, 12), dp(this@DashboardActivity, 4))
        }
        acoes.addView(botaoAcao("Resumo do dia") { startActivity(Intent(this, ResumoDiaActivity::class.java)) })
        acoes.addView(botaoAcao("Buscar") { startActivity(Intent(this, BuscaActivity::class.java)) })
        acoes.addView(botaoAcao("Config.") { startActivity(Intent(this, ConfiguracoesActivity::class.java)) })

        adaptador = AdaptadorCadernos(this, emptyList()) { caderno -> abrirCaderno(caderno) }
        lista = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity)
            adapter = adaptador
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        containerVazio = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = android.view.View.GONE
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(criarMensagemVazio(this@DashboardActivity, "Você ainda não tem nenhum caderno.\nToque em \"+ Novo caderno\" pra começar."))
        }

        val pilha = android.widget.FrameLayout(this).apply {
            addView(lista)
            addView(containerVazio)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        refresh = SwipeRefreshLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            setOnRefreshListener { carregarCadernos() }
            addView(pilha)
        }

        val botaoNovo = criarBotaoPrimario(this, "+ Novo caderno") { abrirDialogoNovoCaderno() }

        val rodape = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@DashboardActivity, 16), dp(this@DashboardActivity, 8), dp(this@DashboardActivity, 16), dp(this@DashboardActivity, 16))
            addView(botaoNovo)
        }

        raiz.addView(acoes)
        raiz.addView(refresh)
        raiz.addView(rodape)
        setContentView(raiz)
    }

    private fun botaoAcao(texto: String, onClick: () -> Unit) = criarBotaoSecundario(this, texto, onClick).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(this@DashboardActivity, 6)
        }
    }

    private fun carregarCadernos() {
        refresh.isRefreshing = true
        ApiClient.get("cadernos/listar.php", onSucesso = { json ->
            refresh.isRefreshing = false
            val array = json.optJSONArray("cadernos") ?: org.json.JSONArray()
            val cadernos = (0 until array.length()).map { Caderno.deJson(array.getJSONObject(it)) }
            adaptador.atualizar(cadernos)
            containerVazio.visibility = if (cadernos.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            lista.visibility = if (cadernos.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        }, onErro = { mensagem ->
            refresh.isRefreshing = false
            mostrarErro(this, mensagem)
        })
    }

    private fun abrirCaderno(caderno: Caderno) {
        val intent = Intent(this, CadernoActivity::class.java)
        intent.putExtra("cadernoId", caderno.id)
        intent.putExtra("cadernoNome", caderno.nome)
        intent.putExtra("professor", caderno.professor)
        startActivity(intent)
    }

    private fun abrirDialogoNovoCaderno() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@DashboardActivity, 24), dp(this@DashboardActivity, 16), dp(this@DashboardActivity, 24), 0)
        }

        val rotuloMateria = TextView(this).apply { text = "Matéria" }
        val seletorMateria = Spinner(contextoTema(this)).apply {
            adapter = ArrayAdapter(contextoTema(this@DashboardActivity), android.R.layout.simple_spinner_dropdown_item, Materias.nomesParaSelecao())
        }

        val campoNome = EditText(this).apply { hint = "Nome do caderno" }
        val campoMateriaCustom = EditText(this).apply {
            hint = "Digite o nome da matéria"
            visibility = View.GONE
        }
        val campoProfessor = EditText(this).apply { hint = "Nome do professor (opcional)" }

        seletorMateria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, posicao: Int, id: Long) {
                val selecionado = seletorMateria.selectedItem as String
                campoMateriaCustom.visibility = if (selecionado == Materias.OUTRA) View.VISIBLE else View.GONE
                // Quando escolhe uma matéria da lista, o nome do caderno vira
                // opcional (usamos o nome da matéria como título, se não
                // digitar nada) - por isso atualiza a "dica" do campo.
                val materiaEscolhida = posicao > 0 && selecionado != Materias.OUTRA
                campoNome.hint = if (materiaEscolhida) "Nome do caderno (opcional)" else "Nome do caderno (obrigatório)"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        layout.addView(rotuloMateria)
        layout.addView(seletorMateria)
        layout.addView(campoMateriaCustom)
        layout.addView(campoNome)
        layout.addView(campoProfessor)

        AlertDialog.Builder(contextoTema(this))
            .setTitle("Novo caderno")
            .setView(layout)
            .setPositiveButton("Criar") { _, _ ->
                val posicaoSelecionada = seletorMateria.selectedItemPosition
                val itemSelecionado = seletorMateria.selectedItem as String

                val materia = when {
                    itemSelecionado == Materias.OUTRA -> campoMateriaCustom.text.toString().trim()
                    posicaoSelecionada > 0 -> itemSelecionado
                    else -> ""
                }

                var nome = campoNome.text.toString().trim()

                if (nome.isEmpty() && materia.isEmpty()) {
                    mostrarErro(this, "Digite um nome pro caderno, ou escolha uma matéria da lista.")
                    return@setPositiveButton
                }
                if (nome.isEmpty()) {
                    nome = materia  // usa o nome da matéria como título, já que ela foi escolhida
                }

                val cor = if (materia.isNotEmpty()) Materias.corPara(materia) else ""
                criarCaderno(nome, materia, campoProfessor.text.toString().trim(), cor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun criarCaderno(nome: String, materia: String, professor: String, cor: String) {
        val corpo = JSONObject().apply {
            put("nome", nome)
            put("materia", materia)
            put("professor", professor)
            put("cor", cor)
            TokenFcmCache.obter()?.let { put("meuTokenFcm", it) }
        }
        ApiClient.post("cadernos/criar.php", corpo, onSucesso = {
            mostrarAviso(this, "Caderno criado!")
            carregarCadernos()
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }
}

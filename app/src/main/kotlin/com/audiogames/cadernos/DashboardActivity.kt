package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Spinner
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONArray
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

        if (intent.getBooleanExtra("abrirNovoCaderno", false)) {
            abrirDialogoNovoCaderno()
        }
        if (intent.getBooleanExtra("abrirEscolherCadernoParaPagina", false)) {
            abrirEscolherCadernoParaPagina()
        }
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
        val raiz = criarTelaBase(this, "Meus Cadernos", mostrarVoltar = false, aoClicarMaisOpcoes = { ancora -> mostrarMenuMaisOpcoes(ancora) })

        val acoes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(this@DashboardActivity, 12), dp(this@DashboardActivity, 10), dp(this@DashboardActivity, 12), dp(this@DashboardActivity, 4))
        }
        acoes.addView(botaoAcao("Resumo do dia") { startActivity(Intent(this, ResumoDiaActivity::class.java)) })
        acoes.addView(botaoAcao("Buscar") { startActivity(Intent(this, BuscaActivity::class.java)) })

        adaptador = AdaptadorCadernos(this, emptyList(), { caderno -> abrirCaderno(caderno) }, { caderno -> abrirDialogoEditarCaderno(caderno) })
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

    private fun mostrarMenuMaisOpcoes(ancora: View) {
        val popup = PopupMenu(contextoTema(this), ancora)
        popup.menu.add(0, 1, 0, "Configurações")
        popup.menu.add(0, 2, 1, "Encontrou algum bug?")
        popup.menu.add(0, 3, 2, "Sobre")
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> startActivity(Intent(this, ConfiguracoesActivity::class.java))
                2 -> abrirEmailSuporte()
                3 -> startActivity(Intent(this, SobreActivity::class.java))
            }
            true
        }
        popup.show()
    }

    private fun abrirEmailSuporte() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:martim@audiogames.com.br")
            putExtra(Intent.EXTRA_SUBJECT, "Cadernos Digitais - Relato de bug")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            mostrarErro(this, "Nenhum app de e-mail encontrado. Escreva pra martim@audiogames.com.br")
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

    private fun abrirEscolherCadernoParaPagina() {
        ApiClient.get("cadernos/listar.php", onSucesso = { json ->
            val array = json.optJSONArray("cadernos") ?: JSONArray()
            if (array.length() == 0) {
                mostrarAviso(this, "Você ainda não tem nenhum caderno. Crie um primeiro.")
                return@get
            }

            val cadernos = (0 until array.length()).map { Caderno.deJson(array.getJSONObject(it)) }
            val nomes = cadernos.map { it.nome }.toTypedArray()

            AlertDialog.Builder(contextoTema(this))
                .setTitle("Em qual caderno?")
                .setItems(nomes) { _, posicao ->
                    val caderno = cadernos[posicao]
                    val intent = Intent(this, NovaPaginaActivity::class.java)
                    intent.putExtra("cadernoId", caderno.id)
                    startActivity(intent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun abrirDialogoNovoCaderno() {
        // Busca os professores já cadastrados ANTES de abrir o diálogo, pra
        // já montar a interface certa desde o início (lista pra escolher,
        // se já tiver algum; ou um campo de texto simples, se nunca usou
        // essa parte - importante pra quem não usa o app pra escola não
        // ver um menu de professor estranho, sem sentido nenhum).
        ApiClient.get("professores/listar.php", onSucesso = { json ->
            abrirDialogoComProfessores(json.optJSONArray("professores") ?: JSONArray())
        }, onErro = {
            abrirDialogoComProfessores(JSONArray()) // se der erro, trata como se não tivesse nenhum cadastrado
        })
    }

    private fun abrirDialogoComProfessores(professoresExistentes: JSONArray) {
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

        seletorMateria.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, posicao: Int, id: Long) {
                val selecionado = seletorMateria.selectedItem as String
                campoMateriaCustom.visibility = if (selecionado == Materias.OUTRA) View.VISIBLE else View.GONE
                val materiaEscolhida = posicao > 0 && selecionado != Materias.OUTRA
                campoNome.hint = if (materiaEscolhida) "Nome do caderno (opcional)" else "Nome do caderno (obrigatório)"
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        layout.addView(rotuloMateria)
        layout.addView(seletorMateria)
        layout.addView(campoMateriaCustom)
        layout.addView(campoNome)

        // -------------------- PROFESSOR --------------------
        val nomesProfessoresExistentes = (0 until professoresExistentes.length())
            .map { professoresExistentes.getJSONObject(it).optString("nome") }

        val campoNovoProfessorNome = EditText(this).apply { hint = "Nome do professor" }
        val campoNovoProfessorEmail = EditText(this).apply {
            hint = "E-mail do professor (opcional, fica salvo pras próximas vezes)"
            visibility = View.GONE
        }
        campoNovoProfessorNome.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                campoNovoProfessorEmail.visibility = if (s?.isNotBlank() == true) View.VISIBLE else View.GONE
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        var seletorProfessor: Spinner? = null

        if (nomesProfessoresExistentes.isEmpty()) {
            // Ninguém cadastrado ainda - só um campo de texto simples,
            // igual a antes. Sem menu nenhum, sem parecer estranho pra
            // quem não usa isso pra escola.
            layout.addView(criarTextoSecao(this, "PROFESSOR"))
            layout.addView(campoNovoProfessorNome)
            layout.addView(campoNovoProfessorEmail)
        } else {
            // Já tem professor cadastrado - mostra a lista pra escolher,
            // com uma opção de adicionar um novo no final.
            val opcoes = mutableListOf("Nenhum")
            opcoes.addAll(nomesProfessoresExistentes)
            opcoes.add("+ Novo professor...")
            seletorProfessor = Spinner(contextoTema(this)).apply {
                adapter = ArrayAdapter(contextoTema(this@DashboardActivity), android.R.layout.simple_spinner_dropdown_item, opcoes)
            }
            seletorProfessor.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, posicao: Int, id: Long) {
                    val ehNovo = posicao == opcoes.size - 1
                    campoNovoProfessorNome.visibility = if (ehNovo) View.VISIBLE else View.GONE
                    campoNovoProfessorEmail.visibility = View.GONE
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
            campoNovoProfessorNome.visibility = View.GONE

            layout.addView(criarTextoSecao(this, "PROFESSOR"))
            layout.addView(seletorProfessor)
            layout.addView(campoNovoProfessorNome)
            layout.addView(campoNovoProfessorEmail)
        }

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
                    nome = materia
                }

                // Decide o nome do professor: ou escolheu um já cadastrado,
                // ou digitou um novo (com ou sem lista de opções na tela).
                val professorEscolhidoDaLista = seletorProfessor?.let {
                    val posicao = it.selectedItemPosition
                    if (posicao in 1 until (nomesProfessoresExistentes.size + 1)) it.selectedItem as String else null
                }
                val professor = professorEscolhidoDaLista ?: campoNovoProfessorNome.text.toString().trim()
                val emailNovoProfessor = campoNovoProfessorEmail.text.toString().trim()

                val cor = if (materia.isNotEmpty()) Materias.corPara(materia) else ""

                // Se digitou um professor novo com e-mail, cadastra ele
                // também (fica salvo pras próximas vezes).
                if (professorEscolhidoDaLista == null && professor.isNotEmpty() && emailNovoProfessor.isNotEmpty()) {
                    val corpoProfessor = JSONObject().apply {
                        put("nome", professor)
                        put("email", emailNovoProfessor)
                    }
                    ApiClient.post("professores/salvar.php", corpoProfessor, onSucesso = {}, onErro = {})
                }

                criarCaderno(nome, materia, professor, cor)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun abrirDialogoEditarCaderno(caderno: Caderno) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@DashboardActivity, 24), dp(this@DashboardActivity, 16), dp(this@DashboardActivity, 24), 0)
        }
        val campoNome = EditText(this).apply { hint = "Nome do caderno"; setText(caderno.nome) }
        val campoMateria = EditText(this).apply { hint = "Matéria (opcional)"; setText(caderno.materia) }
        val campoProfessor = EditText(this).apply { hint = "Nome do professor (opcional)"; setText(caderno.professor) }
        layout.addView(campoNome)
        layout.addView(campoMateria)
        layout.addView(campoProfessor)

        AlertDialog.Builder(contextoTema(this))
            .setTitle("Editar caderno")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val nome = campoNome.text.toString().trim()
                if (nome.isEmpty()) {
                    mostrarErro(this, "Digite um nome pro caderno.")
                    return@setPositiveButton
                }
                val materia = campoMateria.text.toString().trim()
                val cor = if (materia.isNotEmpty() && materia != caderno.materia) {
                    Materias.corPara(materia)  // matéria mudou - tenta achar a cor certa (ou usa o azul padrão)
                } else {
                    caderno.cor  // não mudou (ou ficou vazia) - mantém a cor que já tinha
                }

                val corpo = JSONObject().apply {
                    put("cadernoId", caderno.id)
                    put("nome", nome)
                    put("materia", materia)
                    put("professor", campoProfessor.text.toString().trim())
                    put("cor", cor)
                }
                ApiClient.post("cadernos/editar.php", corpo, onSucesso = {
                    mostrarAviso(this, "Caderno atualizado!")
                    carregarCadernos()
                }, onErro = { mensagem -> mostrarErro(this, mensagem) })
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
            TarefasWidgetProvider.atualizarTodosOsWidgets(this)
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }
}

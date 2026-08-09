package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

/**
 * Modelo de email pronto: rótulo (mostrado no seletor), e o assunto/corpo
 * já montados a partir dos dados do caderno.
 */
private data class ModeloEmail(val chave: String, val rotulo: String, val assunto: String, val corpo: String)

class EnviarEmailActivity : Activity() {

    private lateinit var cadernoId: String
    private lateinit var cadernoNome: String
    private lateinit var professor: String
    private lateinit var alunoNome: String

    private lateinit var spinnerModelo: Spinner
    private lateinit var campoDestinatario: EditText
    private lateinit var campoAssunto: EditText
    private lateinit var campoMensagem: EditText

    private var modelos: List<ModeloEmail> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        cadernoId = intent.getStringExtra("cadernoId") ?: ""
        cadernoNome = intent.getStringExtra("cadernoNome") ?: ""
        professor = intent.getStringExtra("professor") ?: ""
        alunoNome = FirebaseAuth.getInstance().currentUser?.displayName ?: "Aluno"

        montarTela()
        carregarDadosEMontarModelos()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Enviar email")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        spinnerModelo = Spinner(this)

        campoDestinatario = criarCampoTexto(this, "Email do professor")
        campoAssunto = criarCampoTexto(this, "Assunto")
        campoMensagem = criarCampoTexto(this, "Mensagem", multilinha = true)

        val botaoEnviar = criarBotaoPrimario(this, "Enviar") { enviar() }

        conteudo.addView(criarTextoSecao(this, "MODELO DE MENSAGEM"))
        conteudo.addView(spinnerModelo)
        conteudo.addView(campoDestinatario)
        conteudo.addView(campoAssunto)
        conteudo.addView(campoMensagem)
        conteudo.addView(botaoEnviar)

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun carregarDadosEMontarModelos() {
        // Preenche o destinatário com o professor padrão salvo, se tiver.
        ApiClient.get("conta/configuracoes.php", onSucesso = { json ->
            val config = json.optJSONObject("configuracoes") ?: JSONObject()
            val emailPadrao = config.optString("emailProfessorPadrao")
            if (emailPadrao.isNotBlank()) campoDestinatario.setText(emailPadrao)
            val nomeSalvo = config.optString("nomeAluno")
            if (nomeSalvo.isNotBlank()) alunoNome = nomeSalvo
        }, onErro = { /* silencioso */ })

        if (cadernoId.isEmpty()) {
            montarModelos(emptyList())
            return
        }

        ApiClient.get("paginas/atrasadas.php", mapOf("cadernoId" to cadernoId), onSucesso = { json ->
            val array = json.optJSONArray("tarefas") ?: org.json.JSONArray()
            val tarefas = (0 until array.length()).map { Pagina.deJson(array.getJSONObject(it)) }
            montarModelos(tarefas)
        }, onErro = { montarModelos(emptyList()) })
    }

    private var primeiraAplicacaoFeita = false

    private fun montarModelos(tarefasAtrasadas: List<Pagina>) {
        val professorTexto = professor.ifBlank { "(nome do professor)" }
        val cadernoTexto = cadernoNome.ifBlank { "(nome da disciplina)" }

        var listaTarefasTexto = tarefasAtrasadas.joinToString("\n") { tarefa ->
            "- ${tarefa.titulo} (venceu em ${tarefa.dataEntrega})"
        }
        if (listaTarefasTexto.isBlank()) {
            listaTarefasTexto = "- (nenhuma tarefa em atraso identificada automaticamente; edite esta lista se necessário)"
        }

        modelos = listOf(
            ModeloEmail(
                "atraso", "Cobrar tarefas em atraso",
                "Tarefas em atraso - $cadernoTexto",
                "Prezado(a) $professorTexto,\n\n" +
                    "Escrevo para verificar sobre as seguintes tarefas de $cadernoTexto que ainda constam pendentes:\n\n" +
                    "$listaTarefasTexto\n\n" +
                    "Poderia me orientar sobre como regularizar essas pendências, ou se ainda é possível entregá-las?\n\n" +
                    "Desde já, agradeço a atenção.\n\nAtenciosamente,\n$alunoNome"
            ),
            ModeloEmail(
                "duvida", "Tirar dúvida sobre conteúdo",
                "Dúvida sobre $cadernoTexto",
                "Prezado(a) $professorTexto,\n\n" +
                    "Estou com uma dúvida sobre o conteúdo de $cadernoTexto. " +
                    "Poderia me ajudar a esclarecer? Escrevo abaixo o que não entendi:\n\n" +
                    "(descreva sua dúvida aqui)\n\n" +
                    "Agradeço desde já pela atenção.\n\nAtenciosamente,\n$alunoNome"
            ),
            ModeloEmail(
                "prazo", "Pedir mais prazo",
                "Pedido de prazo - $cadernoTexto",
                "Prezado(a) $professorTexto,\n\n" +
                    "Gostaria de solicitar, respeitosamente, um prazo adicional para a entrega das atividades de $cadernoTexto.\n\n" +
                    "Motivo: (explique o motivo aqui)\n\n" +
                    "Fico à disposição para conversar sobre isso.\n\nAtenciosamente,\n$alunoNome"
            ),
            ModeloEmail(
                "justificativa", "Justificar falta ou atraso",
                "Justificativa - $cadernoTexto",
                "Prezado(a) $professorTexto,\n\n" +
                    "Escrevo para justificar minha ausência/atraso em $cadernoTexto.\n\n" +
                    "Motivo: (explique o motivo aqui)\n\n" +
                    "Estou à disposição para repor o que for necessário.\n\nAtenciosamente,\n$alunoNome"
            ),
            ModeloEmail(
                "agradecimento", "Agradecer o professor",
                "Agradecimento - $cadernoTexto",
                "Prezado(a) $professorTexto,\n\n" +
                    "Gostaria de agradecer pelas aulas de $cadernoTexto. " +
                    "Tenho aprendido bastante e queria registrar minha gratidão.\n\nAtenciosamente,\n$alunoNome"
            ),
            ModeloEmail(
                "livre", "Mensagem livre (em branco)",
                if (cadernoNome.isNotBlank()) cadernoTexto else "",
                "Prezado(a) $professorTexto,\n\n\n\nAtenciosamente,\n$alunoNome"
            )
        )

        val adaptador = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelos.map { it.rotulo })
        adaptador.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerModelo.adapter = adaptador

        spinnerModelo.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (!primeiraAplicacaoFeita) {
                    // O Android chama isso sozinho assim que o adapter é
                    // configurado (comportamento padrão de Spinner) - ignora
                    // essa primeira chamada automática, já que aplicamos o
                    // primeiro modelo manualmente logo abaixo.
                    primeiraAplicacaoFeita = true
                    return
                }
                aplicarModelo(modelos[position])
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        // Aplica o primeiro modelo (Cobrar tarefas) manualmente ao abrir.
        aplicarModelo(modelos.first())
    }

    private fun aplicarModelo(modelo: ModeloEmail) {
        val vazio = campoMensagem.text.toString().isBlank()
        if (vazio) {
            campoAssunto.setText(modelo.assunto)
            campoMensagem.setText(modelo.corpo)
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Trocar modelo")
            .setMessage("Substituir o texto atual pelo modelo escolhido?")
            .setPositiveButton("Substituir") { _, _ ->
                campoAssunto.setText(modelo.assunto)
                campoMensagem.setText(modelo.corpo)
            }
            .setNegativeButton("Manter texto atual", null)
            .show()
    }

    private fun enviar() {
        val destinatario = campoDestinatario.text.toString().trim()
        val assunto = campoAssunto.text.toString().trim()
        val mensagem = campoMensagem.text.toString().trim()

        if (destinatario.isEmpty() || assunto.isEmpty() || mensagem.isEmpty()) {
            mostrarErro(this, "Preencha destinatário, assunto e mensagem.")
            return
        }

        val corpo = JSONObject().apply {
            put("destinatario", destinatario)
            put("assunto", assunto)
            put("mensagem", mensagem)
        }

        ApiClient.post("email/enviar.php", corpo, onSucesso = {
            mostrarAviso(this, "E-mail enviado!")
            finish()
        }, onErro = { mensagemErro -> mostrarErro(this, mensagemErro) })
    }
}

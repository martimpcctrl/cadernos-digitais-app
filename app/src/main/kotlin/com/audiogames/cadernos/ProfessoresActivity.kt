package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

class ProfessoresActivity : Activity() {

    private lateinit var containerLista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
        carregarProfessores()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Professores")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val explicacao = TextView(this).apply {
            text = "Cadastre o e-mail de cada professor aqui. Quando você criar um caderno com o nome de um professor cadastrado, o e-mail dele já vem preenchido sozinho na hora de mandar mensagem."
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, 0, 0, dp(this@ProfessoresActivity, 16))
        }

        containerLista = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        val botaoNovo = criarBotaoPrimario(this, "+ Novo professor") { abrirDialogoProfessor(null) }

        conteudo.addView(explicacao)
        conteudo.addView(containerLista)
        conteudo.addView(botaoNovo)

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun carregarProfessores() {
        containerLista.removeAllViews()
        containerLista.addView(TextView(this).apply {
            text = "Carregando..."
            setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
        })

        ApiClient.get("professores/listar.php", onSucesso = { json ->
            renderizarLista(json.optJSONArray("professores") ?: JSONArray())
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun renderizarLista(professores: JSONArray) {
        containerLista.removeAllViews()

        if (professores.length() == 0) {
            val vazio = TextView(this).apply {
                text = "Nenhum professor cadastrado ainda."
                setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
                setPadding(0, dp(this@ProfessoresActivity, 8), 0, dp(this@ProfessoresActivity, 16))
            }
            containerLista.addView(vazio)
            return
        }

        for (i in 0 until professores.length()) {
            val professor = professores.getJSONObject(i)
            val id = professor.optString("id")
            val nome = professor.optString("nome")
            val email = professor.optString("email")

            val cartao = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(this@ProfessoresActivity, 12), dp(this@ProfessoresActivity, 10), dp(this@ProfessoresActivity, 12), dp(this@ProfessoresActivity, 10))
                setBackgroundColor(android.graphics.Color.parseColor(Cores.SUPERFICIE))
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(this@ProfessoresActivity, 8)
                }
            }
            val textoNome = TextView(this).apply {
                text = nome
                textSize = 16f
                setTextColor(android.graphics.Color.parseColor(Cores.TEXTO))
            }
            val textoEmail = TextView(this).apply {
                text = email
                textSize = 13f
                setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
                setPadding(0, 0, 0, dp(this@ProfessoresActivity, 8))
            }
            val linhaBotoes = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val botaoEditar = criarBotaoSecundario(this, "Editar") { abrirDialogoProfessor(professor) }
            val botaoExcluir = criarBotaoSecundario(this, "Excluir") { confirmarExcluir(id, nome) }
            linhaBotoes.addView(botaoEditar)
            linhaBotoes.addView(botaoExcluir)

            cartao.addView(textoNome)
            cartao.addView(textoEmail)
            cartao.addView(linhaBotoes)
            containerLista.addView(cartao)
        }
    }

    private fun abrirDialogoProfessor(professorExistente: JSONObject?) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@ProfessoresActivity, 24), dp(this@ProfessoresActivity, 16), dp(this@ProfessoresActivity, 24), 0)
        }
        val campoNome = EditText(this).apply {
            hint = "Nome do professor"
            setText(professorExistente?.optString("nome") ?: "")
        }
        val campoEmail = EditText(this).apply {
            hint = "E-mail do professor"
            setText(professorExistente?.optString("email") ?: "")
        }
        layout.addView(campoNome)
        layout.addView(campoEmail)

        AlertDialog.Builder(contextoTema(this))
            .setTitle(if (professorExistente == null) "Novo professor" else "Editar professor")
            .setView(layout)
            .setPositiveButton("Salvar") { _, _ ->
                val nome = campoNome.text.toString().trim()
                val email = campoEmail.text.toString().trim()
                if (nome.isEmpty() || email.isEmpty()) {
                    mostrarErro(this, "Preencha o nome e o e-mail.")
                    return@setPositiveButton
                }
                salvarProfessor(nome, email)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun salvarProfessor(nome: String, email: String) {
        val corpo = JSONObject().apply {
            put("nome", nome)
            put("email", email)
        }
        ApiClient.post("professores/salvar.php", corpo, onSucesso = {
            mostrarAviso(this, "Professor salvo!")
            carregarProfessores()
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun confirmarExcluir(id: String, nome: String) {
        AlertDialog.Builder(contextoTema(this))
            .setTitle("Excluir professor")
            .setMessage("Tem certeza que quer remover \"$nome\"?")
            .setPositiveButton("Excluir") { _, _ ->
                val corpo = JSONObject().apply { put("professorId", id) }
                ApiClient.post("professores/excluir.php", corpo, onSucesso = {
                    carregarProfessores()
                }, onErro = { mensagem -> mostrarErro(this, mensagem) })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

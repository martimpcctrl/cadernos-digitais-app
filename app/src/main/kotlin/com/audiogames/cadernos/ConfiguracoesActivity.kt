package com.audiogames.cadernos

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONObject

class ConfiguracoesActivity : Activity() {

    private lateinit var campoNome: EditText
    private lateinit var campoEmailProfessor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        montarTela()
        carregarConfiguracoes()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Configurações")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        campoNome = criarCampoTexto(this, "Seu nome (aparece nos e-mails que você envia)")
        campoEmailProfessor = criarCampoTexto(this, "E-mail padrão do professor (opcional)")

        val botaoSalvar = criarBotaoPrimario(this, "Salvar") { salvar() }

        conteudo.addView(criarTextoSecao(this, "SEU PERFIL"))
        conteudo.addView(campoNome)
        conteudo.addView(criarTextoSecao(this, "E-MAIL"))
        conteudo.addView(campoEmailProfessor)
        conteudo.addView(botaoSalvar)

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun carregarConfiguracoes() {
        ApiClient.get("conta/configuracoes.php", onSucesso = { json ->
            val config = json.optJSONObject("configuracoes") ?: JSONObject()
            campoNome.setText(config.optString("nomeAluno"))
            campoEmailProfessor.setText(config.optString("emailProfessorPadrao"))
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun salvar() {
        val corpo = JSONObject().apply {
            put("nomeAluno", campoNome.text.toString().trim())
            put("emailProfessorPadrao", campoEmailProfessor.text.toString().trim())
        }
        ApiClient.post("conta/configuracoes.php", corpo, onSucesso = {
            mostrarAviso(this, "Configurações salvas!")
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }
}

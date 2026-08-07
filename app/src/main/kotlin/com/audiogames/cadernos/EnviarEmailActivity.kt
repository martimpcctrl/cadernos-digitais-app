package com.audiogames.cadernos

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONObject

class EnviarEmailActivity : Activity() {

    private lateinit var campoDestinatario: EditText
    private lateinit var campoAssunto: EditText
    private lateinit var campoMensagem: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        montarTela()
        preencherComPadroes()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Enviar e-mail")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        campoDestinatario = criarCampoTexto(this, "E-mail do destinatário")
        campoAssunto = criarCampoTexto(this, "Assunto")
        campoMensagem = criarCampoTexto(this, "Mensagem", multilinha = true)

        val botaoEnviar = criarBotaoPrimario(this, "Enviar") { enviar() }

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

    private fun preencherComPadroes() {
        campoAssunto.setText(intent.getStringExtra("assuntoSugerido") ?: "")
        campoMensagem.setText(intent.getStringExtra("mensagemSugerida") ?: "")

        ApiClient.get("conta/configuracoes.php", onSucesso = { json ->
            val config = json.optJSONObject("configuracoes") ?: JSONObject()
            val emailPadrao = config.optString("emailProfessorPadrao")
            if (emailPadrao.isNotBlank()) campoDestinatario.setText(emailPadrao)
        }, onErro = { /* silencioso - não é crítico */ })
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

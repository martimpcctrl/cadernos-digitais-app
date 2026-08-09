package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject

class ConfigEmailActivity : Activity() {

    private lateinit var campoEmailRemetente: EditText
    private lateinit var campoChaveApp: EditText
    private lateinit var textoChaveStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
        carregarConfiguracoes()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Envio de e-mail")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val explicacaoEmail = TextView(this).apply {
            text = "Use um e-mail do Gmail e gere uma senha de app de 16 caracteres em " +
                "myaccount.google.com/apppasswords. Essa chave é diferente da senha normal da conta."
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, 0, 0, dp(this@ConfigEmailActivity, 12))
        }
        val botaoAbrirLink = criarBotaoSecundario(this, "Abrir myaccount.google.com/apppasswords") {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://myaccount.google.com/apppasswords")))
            } catch (e: Exception) { /* sem navegador disponível */ }
        }

        campoEmailRemetente = criarCampoTexto(this, "Seu e-mail (o que vai enviar as mensagens)")
        campoChaveApp = criarCampoTexto(this, "Chave de app (16 caracteres)").apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        textoChaveStatus = TextView(this).apply {
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, dp(this@ConfigEmailActivity, 4), 0, dp(this@ConfigEmailActivity, 12))
        }

        conteudo.addView(explicacaoEmail)
        conteudo.addView(botaoAbrirLink)
        conteudo.addView(campoEmailRemetente)
        conteudo.addView(campoChaveApp)
        conteudo.addView(textoChaveStatus)
        conteudo.addView(criarBotaoPrimario(this, "Salvar configurações de e-mail") { salvarEmail() })
        conteudo.addView(criarBotaoSecundario(this, "Enviar e-mail de teste") { testarEnvio() })

        raiz.addView(conteudo)
        setContentView(raiz)
    }

    private fun carregarConfiguracoes() {
        ApiClient.get("conta/configuracoes.php", onSucesso = { json ->
            val config = json.optJSONObject("configuracoes") ?: JSONObject()
            campoEmailRemetente.setText(config.optString("emailRemetente"))
            val temChave = config.optBoolean("temChaveApp", false)
            textoChaveStatus.text = if (temChave) {
                "Já existe uma chave salva. Deixe o campo em branco para manter a atual, ou digite uma nova para trocar."
            } else {
                "Nenhuma chave salva ainda."
            }
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun salvarEmail() {
        val email = campoEmailRemetente.text.toString().trim()
        val chave = campoChaveApp.text.toString().replace(" ", "")

        if (email.isEmpty()) {
            mostrarErro(this, "Digite o e-mail que vai usar pra enviar.")
            return
        }
        if (chave.isNotEmpty() && chave.length != 16) {
            mostrarErro(this, "A chave de app deve ter exatamente 16 caracteres (ou deixe em branco pra manter a atual).")
            return
        }

        val corpo = JSONObject().apply {
            put("emailRemetente", email)
            put("chaveApp", chave)
        }
        ApiClient.post("conta/configuracoes.php", corpo, onSucesso = {
            mostrarAviso(this, "Configurações de e-mail salvas!")
            campoChaveApp.setText("")
            carregarConfiguracoes()
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun testarEnvio() {
        val email = campoEmailRemetente.text.toString().trim()
        if (email.isEmpty()) {
            mostrarErro(this, "Salve seu e-mail de envio primeiro.")
            return
        }
        mostrarAviso(this, "Enviando e-mail de teste...")
        val corpo = JSONObject().apply { put("destinatario", email) }
        ApiClient.post("email/testar.php", corpo, onSucesso = {
            mostrarAviso(this, "E-mail de teste enviado! Confira sua caixa de entrada.")
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }
}

package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import org.json.JSONObject

class ConfiguracoesActivity : Activity() {

    private lateinit var campoNome: EditText
    private lateinit var campoEmailRemetente: EditText
    private lateinit var campoChaveApp: EditText
    private lateinit var campoEmailProfessor: EditText
    private lateinit var textoChaveStatus: TextView
    private lateinit var switchBloqueio: Switch

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

        val botaoSalvarPerfil = criarBotaoPrimario(this, "Salvar") { salvarPerfil() }
        val botaoSair = criarBotaoSecundario(this, "Sair da conta") { confirmarSair() }
        val botaoAtualizar = criarBotaoSecundario(this, "Verificar atualizações") { verificarAtualizacaoManual() }

        val explicacaoEmail = TextView(this).apply {
            text = "Use um e-mail do Gmail e gere uma senha de app de 16 caracteres em " +
                "myaccount.google.com/apppasswords. Essa chave é diferente da senha normal da conta."
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, dp(this@ConfiguracoesActivity, 4), 0, dp(this@ConfiguracoesActivity, 12))
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
        }

        val botaoSalvarEmail = criarBotaoPrimario(this, "Salvar configurações de e-mail") { salvarEmail() }
        val botaoTestar = criarBotaoSecundario(this, "Enviar e-mail de teste") { testarEnvio() }

        val linhaBloqueio = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(this@ConfiguracoesActivity, 4), 0, dp(this@ConfiguracoesActivity, 8))
        }
        val textoBloqueio = TextView(this).apply {
            text = "Bloqueio do aplicativo (pede sua digital toda vez que abrir)"
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchBloqueio = Switch(this).apply {
            isChecked = BloqueioManager.estaAtivado(this@ConfiguracoesActivity)
            setOnCheckedChangeListener { _, ativado -> alterarBloqueio(ativado) }
        }
        linhaBloqueio.addView(textoBloqueio)
        linhaBloqueio.addView(switchBloqueio)

        conteudo.addView(criarTextoSecao(this, "SEU PERFIL"))
        conteudo.addView(campoNome)
        conteudo.addView(campoEmailProfessor)
        conteudo.addView(botaoSalvarPerfil)

        conteudo.addView(criarTextoSecao(this, "ENVIO DE E-MAIL (PARA PROFESSORES)"))
        conteudo.addView(explicacaoEmail)
        conteudo.addView(botaoAbrirLink)
        conteudo.addView(campoEmailRemetente)
        conteudo.addView(campoChaveApp)
        conteudo.addView(textoChaveStatus)
        conteudo.addView(botaoSalvarEmail)
        conteudo.addView(botaoTestar)

        conteudo.addView(criarTextoSecao(this, "SEGURANÇA"))
        conteudo.addView(linhaBloqueio)

        conteudo.addView(criarTextoSecao(this, "CONTA"))
        conteudo.addView(botaoSair)
        conteudo.addView(botaoAtualizar)

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
            campoEmailRemetente.setText(config.optString("emailRemetente"))
            val temChave = config.optBoolean("temChaveApp", false)
            textoChaveStatus.text = if (temChave) {
                "Já existe uma chave salva. Deixe o campo em branco para manter a atual, ou digite uma nova para trocar."
            } else {
                "Nenhuma chave salva ainda."
            }
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun salvarPerfil() {
        val corpo = JSONObject().apply {
            put("nomeAluno", campoNome.text.toString().trim())
            put("emailProfessorPadrao", campoEmailProfessor.text.toString().trim())
        }
        ApiClient.post("conta/configuracoes.php", corpo, onSucesso = {
            mostrarAviso(this, "Perfil salvo!")
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

    private fun verificarAtualizacaoManual() {
        mostrarAviso(this, "Verificando atualizações...")
        var encontrou = false
        UpdateManager(this).verificarSilenciosamente { atualizacao ->
            encontrou = true
            AlertDialog.Builder(this)
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
        campoNome.postDelayed({
            if (!encontrou) mostrarAviso(this, "Você já está na versão mais recente.")
        }, 4000)
    }

    private fun alterarBloqueio(ativado: Boolean) {
        if (ativado && !BloqueioManager.biometriaDisponivel(this)) {
            mostrarErro(this, "Esse aparelho não tem uma digital cadastrada. Cadastre uma nas configurações do Android primeiro.")
            switchBloqueio.isChecked = false
            return
        }
        BloqueioManager.definirAtivado(this, ativado)
        if (ativado) {
            // Já desbloqueado nesse momento (acabou de configurar), não
            // precisa pedir a digital de novo até sair e voltar no app.
            CadernosApplication.desbloqueado = true
        }
    }

    private fun confirmarSair() {
        AlertDialog.Builder(this)
            .setTitle("Sair da conta")
            .setMessage("Tem certeza que quer sair?")
            .setPositiveButton("Sair") { _, _ ->
                GoogleAuthManager(this).sair {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import org.json.JSONObject

class ConfiguracoesActivity : Activity() {

    private lateinit var campoNome: EditText
    private lateinit var campoEmailAluno: EditText
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
        campoEmailAluno = criarCampoTexto(this, "Seu email de contato (pro professor poder te responder)")
        campoEmailProfessor = criarCampoTexto(this, "E-mail padrão do professor (opcional)")

        val botaoSalvar = criarBotaoPrimario(this, "Salvar") { salvar() }
        val botaoSair = criarBotaoSecundario(this, "Sair da conta") { confirmarSair() }
        val botaoAtualizar = criarBotaoSecundario(this, "Verificar atualizações") { verificarAtualizacaoManual() }

        conteudo.addView(criarTextoSecao(this, "SEU PERFIL"))
        conteudo.addView(campoNome)
        conteudo.addView(campoEmailAluno)
        conteudo.addView(criarTextoSecao(this, "E-MAIL"))
        conteudo.addView(campoEmailProfessor)
        conteudo.addView(botaoSalvar)
        conteudo.addView(criarTextoSecao(this, "CONTA"))
        conteudo.addView(botaoSair)
        conteudo.addView(botaoAtualizar)

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun verificarAtualizacaoManual() {
        mostrarAviso(this, "Verificando atualizações...")
        var encontrou = false
        UpdateManager(this).verificarSilenciosamente { atualizacao ->
            encontrou = true
            android.app.AlertDialog.Builder(this)
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

    private fun confirmarSair() {
        android.app.AlertDialog.Builder(this)
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

    private fun carregarConfiguracoes() {
        ApiClient.get("conta/configuracoes.php", onSucesso = { json ->
            val config = json.optJSONObject("configuracoes") ?: JSONObject()
            campoNome.setText(config.optString("nomeAluno"))
            campoEmailAluno.setText(config.optString("emailAluno"))
            campoEmailProfessor.setText(config.optString("emailProfessorPadrao"))
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun salvar() {
        val corpo = JSONObject().apply {
            put("nomeAluno", campoNome.text.toString().trim())
            put("emailAluno", campoEmailAluno.text.toString().trim())
            put("emailProfessorPadrao", campoEmailProfessor.text.toString().trim())
        }
        ApiClient.post("conta/configuracoes.php", corpo, onSucesso = {
            mostrarAviso(this, "Configurações salvas!")
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }
}

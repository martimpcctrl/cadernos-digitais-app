package com.audiogames.cadernos

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import org.json.JSONObject

class ConfigPerfilActivity : Activity() {

    private lateinit var campoNome: EditText
    private lateinit var checkboxEmailProfessor: CheckBox
    private lateinit var campoEmailProfessor: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
        carregarConfiguracoes()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Seu perfil")
        val padding = dp(this, 20)

        val conteudo = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        campoNome = criarCampoTexto(this, "Seu nome (aparece nos e-mails que você envia)")

        checkboxEmailProfessor = CheckBox(contextoTema(this)).apply {
            text = "Quero definir um e-mail padrão do professor"
        }
        campoEmailProfessor = criarCampoTexto(this, "E-mail padrão do professor").apply {
            visibility = View.GONE
        }
        checkboxEmailProfessor.setOnCheckedChangeListener { _, marcado ->
            campoEmailProfessor.visibility = if (marcado) View.VISIBLE else View.GONE
            if (!marcado) campoEmailProfessor.setText("")
        }

        conteudo.addView(campoNome)
        conteudo.addView(checkboxEmailProfessor)
        conteudo.addView(campoEmailProfessor)
        conteudo.addView(criarBotaoPrimario(this, "Salvar") { salvarPerfil() })

        raiz.addView(conteudo)
        setContentView(raiz)
    }

    private fun carregarConfiguracoes() {
        ApiClient.get("conta/configuracoes.php", onSucesso = { json ->
            val config = json.optJSONObject("configuracoes") ?: JSONObject()
            campoNome.setText(config.optString("nomeAluno"))
            val emailProfessorSalvo = config.optString("emailProfessorPadrao")
            campoEmailProfessor.setText(emailProfessorSalvo)
            checkboxEmailProfessor.isChecked = emailProfessorSalvo.isNotBlank()
            campoEmailProfessor.visibility = if (emailProfessorSalvo.isNotBlank()) View.VISIBLE else View.GONE
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
}

package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView

class CompartilharActivity : Activity() {

    private lateinit var cadernoId: String
    private lateinit var cadernoNome: String
    private lateinit var textoStatus: TextView
    private lateinit var botaoEnviar: android.widget.Button
    private lateinit var botaoRevogar: android.widget.Button
    private var linkGerado: String? = null
    private var tokenGerado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        cadernoId = intent.getStringExtra("cadernoId") ?: ""
        cadernoNome = intent.getStringExtra("cadernoNome") ?: "caderno"
        montarTela()
        gerarLink()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Compartilhar")
        val padding = dp(this, 24)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(padding, padding, padding, padding)
        }

        textoStatus = TextView(this).apply {
            text = "Gerando link..."
            textSize = 15f
            gravity = Gravity.CENTER
        }

        val avisoValidade = TextView(this).apply {
            text = "Esse link fica válido por 30 dias, ou até você revogar."
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, dp(this@CompartilharActivity, 8), 0, dp(this@CompartilharActivity, 20))
        }

        botaoEnviar = criarBotaoPrimario(this, "Enviar link") { compartilharLink() }.apply {
            visibility = android.view.View.GONE
        }
        botaoRevogar = criarBotaoSecundario(this, "Revogar este link") { confirmarRevogar() }.apply {
            visibility = android.view.View.GONE
        }

        conteudo.addView(textoStatus)
        conteudo.addView(avisoValidade)
        conteudo.addView(botaoEnviar)
        conteudo.addView(botaoRevogar)
        raiz.addView(conteudo)
        setContentView(raiz)
    }

    private fun gerarLink() {
        val corpo = org.json.JSONObject().apply {
            put("cadernoId", cadernoId)
        }
        ApiClient.post("compartilhar/gerar.php", corpo, onSucesso = { json ->
            linkGerado = json.optString("url")
            tokenGerado = json.optString("token")
            textoStatus.text = "Link pronto:\n\n$linkGerado"
            botaoEnviar.visibility = android.view.View.VISIBLE
            botaoRevogar.visibility = android.view.View.VISIBLE
        }, onErro = { mensagem ->
            textoStatus.text = "Não consegui gerar o link: $mensagem"
        })
    }

    private fun compartilharLink() {
        val link = linkGerado ?: return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "Dá uma olhada no meu caderno \"$cadernoNome\": $link")
        }
        startActivity(Intent.createChooser(intent, "Compartilhar caderno"))
    }

    private fun confirmarRevogar() {
        val token = tokenGerado ?: return
        AlertDialog.Builder(this)
            .setTitle("Revogar link")
            .setMessage("Quem já tiver esse link não vai mais conseguir abrir. Tem certeza?")
            .setPositiveButton("Revogar") { _, _ ->
                val corpo = org.json.JSONObject().apply { put("token", token) }
                ApiClient.post("compartilhar/revogar.php", corpo, onSucesso = {
                    mostrarAviso(this, "Link revogado.")
                    finish()
                }, onErro = { mensagem -> mostrarErro(this, mensagem) })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

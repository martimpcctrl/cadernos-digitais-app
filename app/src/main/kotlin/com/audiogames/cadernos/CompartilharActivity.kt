package com.audiogames.cadernos

import android.app.Activity
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
    private var linkGerado: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        botaoEnviar = criarBotaoPrimario(this, "Enviar link") { compartilharLink() }.apply {
            visibility = android.view.View.GONE
        }

        conteudo.addView(textoStatus)
        conteudo.addView(botaoEnviar)
        raiz.addView(conteudo)
        setContentView(raiz)
    }

    private fun gerarLink() {
        val corpo = org.json.JSONObject().apply {
            put("cadernoId", cadernoId)
        }
        ApiClient.post("compartilhar/gerar.php", corpo, onSucesso = { json ->
            linkGerado = json.optString("url")
            textoStatus.text = "Link pronto:\n\n$linkGerado"
            botaoEnviar.visibility = android.view.View.VISIBLE
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
}

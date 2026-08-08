package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class CompartilhadoActivity : Activity() {

    private var token: String? = null
    private lateinit var textoStatus: TextView
    private lateinit var containerAcoes: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        token = intent?.data?.getQueryParameter("token")
        montarTela()

        if (token.isNullOrBlank()) {
            textoStatus.text = "Link inválido: nenhum código foi encontrado."
            return
        }

        if (FirebaseAuth.getInstance().currentUser == null) {
            mostrarPedidoLogin()
            return
        }

        carregarPreVisualizacao()
    }

    private fun montarTela() {
        val padding = dp(this, 28)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(Cores.FUNDO))
            setPadding(padding, padding, padding, padding)
        }

        val titulo = TextView(this).apply {
            text = "Caderno compartilhado"
            textSize = 22f
            setTextColor(Color.parseColor(Cores.TEXTO))
            gravity = Gravity.CENTER
        }

        textoStatus = TextView(this).apply {
            text = "Carregando..."
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, dp(this@CompartilhadoActivity, 12), 0, dp(this@CompartilhadoActivity, 20))
        }

        containerAcoes = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        layout.addView(titulo)
        layout.addView(textoStatus)
        layout.addView(containerAcoes)
        setContentView(android.widget.ScrollView(this).apply { addView(layout) })
    }

    private fun mostrarPedidoLogin() {
        textoStatus.text = "Você precisa entrar na sua conta primeiro pra adicionar esse caderno."
        containerAcoes.addView(criarBotaoPrimario(this, "Entrar") {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        })
    }

    private fun carregarPreVisualizacao() {
        ApiClient.get(
            "compartilhar/ver.php",
            mapOf("token" to token!!),
            exigeLogin = false,
            onSucesso = { json ->
                if (json.optString("tipo") != "caderno") {
                    textoStatus.text = "Esse link compartilha uma mochila inteira, não dá pra adicionar assim."
                    return@get
                }
                val caderno = json.optJSONObject("caderno") ?: JSONObject()
                val paginas = json.optJSONArray("paginas")
                val totalPaginas = paginas?.length() ?: 0

                textoStatus.text = "\"${caderno.optString("nome")}\"\n$totalPaginas página(s)\n\nAdicionar uma cópia desse caderno aos seus?"
                containerAcoes.removeAllViews()
                containerAcoes.addView(criarBotaoPrimario(this, "Adicionar aos meus cadernos") { adicionar() })
                containerAcoes.addView(criarBotaoSecundario(this, "Agora não") { irParaDashboard() })
            },
            onErro = { mensagem -> textoStatus.text = mensagem }
        )
    }

    private fun adicionar() {
        textoStatus.text = "Adicionando..."
        containerAcoes.removeAllViews()
        val corpo = JSONObject().apply { put("token", token) }
        ApiClient.post("compartilhar/clonar.php", corpo, onSucesso = {
            mostrarAviso(this, "Caderno adicionado!")
            irParaDashboard()
        }, onErro = { mensagem ->
            textoStatus.text = mensagem
            containerAcoes.addView(criarBotaoSecundario(this, "Voltar") { irParaDashboard() })
        })
    }

    private fun irParaDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}

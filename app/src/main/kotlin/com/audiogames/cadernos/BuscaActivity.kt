package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class BuscaActivity : Activity() {

    private lateinit var campoBusca: EditText
    private lateinit var containerResultados: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Buscar")
        val padding = dp(this, 16)

        campoBusca = criarCampoTexto(this, "Buscar em cadernos e páginas...").apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    buscar()
                    true
                } else false
            }
        }
        val botaoBuscar = criarBotaoPrimario(this, "Buscar") { buscar() }

        containerResultados = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val topo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, 0)
            addView(campoBusca)
            addView(botaoBuscar)
        }

        raiz.addView(topo)
        raiz.addView(ScrollView(this).apply {
            addView(containerResultados.apply { setPadding(padding, padding, padding, padding) })
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun buscar() {
        val termo = campoBusca.text.toString().trim()
        if (termo.isEmpty()) return

        containerResultados.removeAllViews()
        containerResultados.addView(criarCarregando(this))

        ApiClient.get("busca/tudo.php", mapOf("termo" to termo), onSucesso = { json ->
            containerResultados.removeAllViews()

            val cadernos = json.optJSONArray("cadernos") ?: org.json.JSONArray()
            val paginas = json.optJSONArray("paginas") ?: org.json.JSONArray()

            if (cadernos.length() == 0 && paginas.length() == 0) {
                containerResultados.addView(criarMensagemVazio(this, "Nenhum resultado pra \"$termo\"."))
                return@get
            }

            if (cadernos.length() > 0) {
                containerResultados.addView(criarTextoSecao(this, "CADERNOS"))
                for (i in 0 until cadernos.length()) {
                    val c = cadernos.getJSONObject(i)
                    containerResultados.addView(linhaResultado(c.optString("nome"), c.optString("materia")) {
                        val intent = Intent(this, CadernoActivity::class.java)
                        intent.putExtra("cadernoId", c.optString("id"))
                        intent.putExtra("cadernoNome", c.optString("nome"))
                        startActivity(intent)
                    })
                }
            }

            if (paginas.length() > 0) {
                containerResultados.addView(criarTextoSecao(this, "PÁGINAS"))
                for (i in 0 until paginas.length()) {
                    val p = paginas.getJSONObject(i)
                    containerResultados.addView(linhaResultado(p.optString("titulo"), p.optString("conteudo").take(60)) {
                        val intent = Intent(this, PaginaActivity::class.java)
                        intent.putExtra("paginaId", p.optString("id"))
                        intent.putExtra("titulo", p.optString("titulo"))
                        intent.putExtra("conteudo", p.optString("conteudo"))
                        intent.putExtra("tipo", p.optString("tipo"))
                        intent.putExtra("dataEntrega", p.optString("dataEntrega"))
                        intent.putExtra("totalFotos", p.optInt("totalFotos", 0))
                        startActivity(intent)
                    })
                }
            }
        }, onErro = { mensagem ->
            containerResultados.removeAllViews()
            containerResultados.addView(criarMensagemVazio(this, mensagem))
        })
    }

    private fun linhaResultado(titulo: String, subtitulo: String, aoClicar: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Cores.SUPERFICIE))
            setPadding(dp(this@BuscaActivity, 14), dp(this@BuscaActivity, 12), dp(this@BuscaActivity, 14), dp(this@BuscaActivity, 12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(this@BuscaActivity, 6)
            }
            addView(TextView(this@BuscaActivity).apply { text = titulo; textSize = 16f; setTextColor(Color.parseColor(Cores.TEXTO)) })
            if (subtitulo.isNotBlank()) {
                addView(TextView(this@BuscaActivity).apply { text = subtitulo; textSize = 12f; setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO)) })
            }
            setOnClickListener { aoClicar() }
        }
    }
}

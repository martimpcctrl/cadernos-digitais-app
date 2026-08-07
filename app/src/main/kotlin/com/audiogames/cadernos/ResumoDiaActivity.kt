package com.audiogames.cadernos

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ResumoDiaActivity : Activity() {

    private lateinit var containerLista: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        montarTela()
        carregarResumo()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Resumo do dia")

        val dataFormatada = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR")).format(Date())
        val cabecalho = TextView(this).apply {
            text = dataFormatada.replaceFirstChar { it.uppercase() }
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, dp(this@ResumoDiaActivity, 16), 0, dp(this@ResumoDiaActivity, 8))
        }

        containerLista = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@ResumoDiaActivity, 16), 0, dp(this@ResumoDiaActivity, 16), dp(this@ResumoDiaActivity, 16))
        }

        raiz.addView(cabecalho)
        raiz.addView(ScrollView(this).apply {
            addView(containerLista)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun carregarResumo() {
        containerLista.removeAllViews()
        containerLista.addView(criarCarregando(this))

        val hoje = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        ApiClient.get("resumo/dia.php", mapOf("data" to hoje), onSucesso = { json ->
            containerLista.removeAllViews()
            val array = json.optJSONArray("tarefas") ?: org.json.JSONArray()

            if (array.length() == 0) {
                containerLista.addView(criarMensagemVazio(this, "Nenhuma tarefa com entrega pra hoje. 🎉"))
                return@get
            }

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                containerLista.addView(linhaTarefa(item.optString("titulo"), item.optString("nomeCaderno")))
            }
        }, onErro = { mensagem ->
            containerLista.removeAllViews()
            containerLista.addView(criarMensagemVazio(this, "Não consegui carregar: $mensagem"))
        })
    }

    private fun linhaTarefa(titulo: String, nomeCaderno: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
            setPadding(dp(this@ResumoDiaActivity, 14), dp(this@ResumoDiaActivity, 12), dp(this@ResumoDiaActivity, 14), dp(this@ResumoDiaActivity, 12))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(this@ResumoDiaActivity, 8)
            }
            addView(TextView(this@ResumoDiaActivity).apply {
                text = titulo
                textSize = 16f
                setTextColor(Color.parseColor(Cores.TEXTO))
            })
            addView(TextView(this@ResumoDiaActivity).apply {
                text = nomeCaderno
                textSize = 12f
                setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            })
        }
    }
}

package com.audiogames.cadernos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Mostra o histórico local de notificações push já recebidas nesse aparelho. */
class NotificacoesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Notificações")
        val notificacoes = NotificacoesStore.listar(this)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@NotificacoesActivity, 16), dp(this@NotificacoesActivity, 14), dp(this@NotificacoesActivity, 16), dp(this@NotificacoesActivity, 20))
        }

        if (notificacoes.isEmpty()) {
            val vazio = TextView(this).apply {
                text = "Nenhuma notificação recebida ainda."
                textSize = 14f
                setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
                gravity = Gravity.CENTER
                setPadding(0, dp(this@NotificacoesActivity, 40), 0, 0)
            }
            conteudo.addView(vazio)
        } else {
            val formato = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "BR"))

            notificacoes.forEach { n ->
                val card = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor(Cores.SUPERFICIE))
                    setPadding(dp(this@NotificacoesActivity, 14), dp(this@NotificacoesActivity, 12), dp(this@NotificacoesActivity, 14), dp(this@NotificacoesActivity, 12))
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = dp(this@NotificacoesActivity, 10)
                    }
                }

                val tituloView = TextView(this).apply {
                    text = n.optString("titulo")
                    textSize = 15f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.parseColor(Cores.TEXTO))
                }
                val corpoView = TextView(this).apply {
                    text = n.optString("corpo")
                    textSize = 14f
                    setTextColor(Color.parseColor(Cores.TEXTO))
                    setPadding(0, dp(this@NotificacoesActivity, 4), 0, dp(this@NotificacoesActivity, 6))
                }
                val dataView = TextView(this).apply {
                    text = formato.format(Date(n.optLong("recebidaEm")))
                    textSize = 12f
                    setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
                }

                card.addView(tituloView)
                card.addView(corpoView)
                card.addView(dataView)
                conteudo.addView(card)
            }

            val botaoLimpar = criarBotaoSecundario(this, "Limpar histórico") {
                NotificacoesStore.limpar(this)
                montarTela()
            }
            conteudo.addView(botaoLimpar)
        }

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }
}

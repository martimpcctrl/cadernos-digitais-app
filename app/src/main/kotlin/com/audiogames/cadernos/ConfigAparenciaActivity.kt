package com.audiogames.cadernos

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

class ConfigAparenciaActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Aparência")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val linhaTema = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(this@ConfigAparenciaActivity, 8), 0, dp(this@ConfigAparenciaActivity, 8))
        }
        val modoAtual = TemaManager.obterModo(this)
        linhaTema.addView(criarBotaoTema("Claro", modoAtual == TemaManager.CLARO) { selecionarTema(TemaManager.CLARO) })
        linhaTema.addView(criarBotaoTema("Escuro", modoAtual == TemaManager.ESCURO) { selecionarTema(TemaManager.ESCURO) })
        linhaTema.addView(criarBotaoTema("Automático", modoAtual == TemaManager.SISTEMA) { selecionarTema(TemaManager.SISTEMA) })

        conteudo.addView(linhaTema)
        conteudo.addView(TextView(this).apply {
            text = "\"Automático\" segue o modo claro/escuro do seu celular. Feche e abra o app de novo depois de trocar, pra ver em todas as telas."
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, dp(this@ConfigAparenciaActivity, 8), 0, 0)
        })

        raiz.addView(conteudo)
        setContentView(raiz)
    }

    private fun criarBotaoTema(rotulo: String, selecionado: Boolean, aoClicar: () -> Unit): android.widget.Button {
        return android.widget.Button(this).apply {
            text = if (selecionado) "✓ $rotulo" else rotulo
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(this@ConfigAparenciaActivity, 6)
            }
            if (selecionado) {
                setBackgroundColor(android.graphics.Color.parseColor(Cores.PRIMARIA))
                setTextColor(android.graphics.Color.WHITE)
            }
            setOnClickListener { aoClicar() }
        }
    }

    private fun selecionarTema(modo: String) {
        TemaManager.definirModo(this, modo)
        recreate()
    }
}

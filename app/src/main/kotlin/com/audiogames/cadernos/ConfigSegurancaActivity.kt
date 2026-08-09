package com.audiogames.cadernos

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class ConfigSegurancaActivity : Activity() {

    private lateinit var switchBloqueio: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Segurança")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val linhaBloqueio = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val textoBloqueio = TextView(this).apply {
            text = "Bloqueio do aplicativo (pede sua digital, PIN, padrão ou senha toda vez que abrir)"
            textSize = 15f
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        switchBloqueio = Switch(contextoTema(this)).apply {
            isChecked = BloqueioManager.estaAtivado(this@ConfigSegurancaActivity)
            setOnCheckedChangeListener { _, ativado -> alterarBloqueio(ativado) }
        }
        linhaBloqueio.addView(textoBloqueio)
        linhaBloqueio.addView(switchBloqueio)

        conteudo.addView(linhaBloqueio)
        raiz.addView(conteudo)
        setContentView(raiz)
    }

    private fun alterarBloqueio(ativado: Boolean) {
        if (ativado && !BloqueioManager.biometriaDisponivel(this)) {
            mostrarErro(this, "Esse aparelho não tem nenhuma trava configurada (digital, PIN, padrão ou senha). Configure uma nas configurações do Android primeiro.")
            switchBloqueio.isChecked = false
            return
        }
        BloqueioManager.definirAtivado(this, ativado)
        if (ativado) {
            CadernosApplication.desbloqueado = true
        }
    }
}

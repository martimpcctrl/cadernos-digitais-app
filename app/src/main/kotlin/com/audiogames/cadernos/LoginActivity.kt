package com.audiogames.cadernos

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.app.Activity

class LoginActivity : Activity() {

    private lateinit var googleAuth: GoogleAuthManager
    private lateinit var textoStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        googleAuth = GoogleAuthManager(this)

        if (googleAuth.estaLogado()) {
            irParaDashboard()
            return
        }

        montarTela()
    }

    private fun montarTela() {
        val padding = dp(this, 28)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor(Cores.FUNDO))
            setPadding(padding, padding, padding, padding)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }

        val titulo = TextView(this).apply {
            text = "Cadernos Digitais"
            textSize = 28f
            setTextColor(Color.parseColor(Cores.TEXTO))
            gravity = Gravity.CENTER
        }

        val subtitulo = TextView(this).apply {
            text = "Suas anotações e tarefas escolares, organizadas e sempre com você."
            textSize = 15f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            gravity = Gravity.CENTER
            setPadding(0, dp(this@LoginActivity, 12), 0, dp(this@LoginActivity, 40))
        }

        val botaoEntrar = criarBotaoPrimario(this, "Entrar com Google") { iniciarLogin() }

        textoStatus = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(Cores.ERRO))
            setPadding(0, dp(this@LoginActivity, 16), 0, 0)
        }

        layout.addView(titulo)
        layout.addView(subtitulo)
        layout.addView(botaoEntrar)
        layout.addView(textoStatus)

        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun iniciarLogin() {
        startActivityForResult(googleAuth.iniciarLogin(), GoogleAuthManager.REQUEST_CODE_GOOGLE_SIGN_IN)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != GoogleAuthManager.REQUEST_CODE_GOOGLE_SIGN_IN) return

        googleAuth.tratarResultadoLogin(
            data,
            onSucesso = { irParaDashboard() },
            onErro = { mensagem -> textoStatus.text = mensagem }
        )
    }

    private fun irParaDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}

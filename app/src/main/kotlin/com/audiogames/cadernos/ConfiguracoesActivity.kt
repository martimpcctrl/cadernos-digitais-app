package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Agora é só um menu - cada categoria abre a própria tela, em vez de
 * ficar tudo junto numa lista só (fácil de perder o fio da meada e
 * mais difícil de navegar com leitor de tela também).
 */
class ConfiguracoesActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Configurações")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        conteudo.addView(itemCategoria("Aparência", "Tema claro, escuro ou automático") {
            startActivity(Intent(this, ConfigAparenciaActivity::class.java))
        })
        conteudo.addView(itemCategoria("Seu perfil", "Nome e e-mail do professor") {
            startActivity(Intent(this, ConfigPerfilActivity::class.java))
        })
        conteudo.addView(itemCategoria("Envio de e-mail", "Configurar o e-mail que envia pros professores") {
            startActivity(Intent(this, ConfigEmailActivity::class.java))
        })
        conteudo.addView(itemCategoria("Segurança", "Bloqueio do aplicativo por digital") {
            startActivity(Intent(this, ConfigSegurancaActivity::class.java))
        })
        conteudo.addView(itemCategoria("Conta", "Sair da conta, verificar atualizações") {
            startActivity(Intent(this, ConfigContaActivity::class.java))
        })

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun itemCategoria(titulo: String, subtitulo: String, aoClicar: () -> Unit): android.widget.Button {
        val texto = "$titulo\n$subtitulo"
        return android.widget.Button(this, null, android.R.attr.buttonStyle).apply {
            text = texto
            isAllCaps = false
            gravity = android.view.Gravity.START or android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(this@ConfiguracoesActivity, 16), dp(this@ConfiguracoesActivity, 14), dp(this@ConfiguracoesActivity, 16), dp(this@ConfiguracoesActivity, 14))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(this@ConfiguracoesActivity, 10)
            }
            setOnClickListener { aoClicar() }
        }
    }
}

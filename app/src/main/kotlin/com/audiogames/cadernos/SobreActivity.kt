package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/**
 * Tela "Sobre" - nome do app, versão instalada (pega automaticamente do
 * pacote, não precisa atualizar na mão a cada versão nova) e créditos.
 */
class SobreActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Sobre")
        val padding = dp(this, 24)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(padding, padding, padding, padding)
        }

        val nomeApp = TextView(this).apply {
            text = "Cadernos Digitais"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(Cores.TEXTO))
            gravity = Gravity.CENTER
        }

        val versao = TextView(this).apply {
            text = "Versão ${obterVersaoInstalada()}"
            textSize = 13f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            gravity = Gravity.CENTER
            setPadding(0, dp(this@SobreActivity, 4), 0, dp(this@SobreActivity, 24))
        }

        val descricao = TextView(this).apply {
            text = "Suas anotações e tarefas escolares, sincronizadas entre celular, computador e o site - sempre com você."
            textSize = 14f
            setTextColor(Color.parseColor(Cores.TEXTO))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(this@SobreActivity, 28))
        }

        val tituloCreditos = criarTextoSecao(this, "CRÉDITOS")
        tituloCreditos.gravity = Gravity.CENTER

        val nomeDesenvolvedor = TextView(this).apply {
            text = "Martim Alves de Souto Neto"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor(Cores.TEXTO))
            gravity = Gravity.CENTER
            setPadding(0, dp(this@SobreActivity, 4), 0, dp(this@SobreActivity, 4))
        }

        val subtituloDesenvolvedor = TextView(this).apply {
            text = "Desenvolvimento, design e manutenção"
            textSize = 13f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(this@SobreActivity, 28))
        }

        val botaoContato = criarBotaoSecundario(this, "Encontrou algum bug? Entre em contato") {
            abrirEmailSuporte()
        }

        conteudo.addView(nomeApp)
        conteudo.addView(versao)
        conteudo.addView(descricao)
        conteudo.addView(tituloCreditos)
        conteudo.addView(nomeDesenvolvedor)
        conteudo.addView(subtituloDesenvolvedor)
        conteudo.addView(botaoContato)

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun obterVersaoInstalada(): String {
        return try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }

    private fun abrirEmailSuporte() {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:martim@audiogames.com.br")
            putExtra(Intent.EXTRA_SUBJECT, "Cadernos Digitais - Relato de bug")
        }
        try {
            startActivity(intent)
        } catch (e: Exception) {
            mostrarErro(this, "Nenhum app de e-mail encontrado. Escreva pra martim@audiogames.com.br")
        }
    }
}

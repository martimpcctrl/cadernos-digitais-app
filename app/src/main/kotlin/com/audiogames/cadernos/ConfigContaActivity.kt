package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout

class ConfigContaActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Conta")
        val padding = dp(this, 20)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        conteudo.addView(criarBotaoSecundario(this, "Sair da conta") { confirmarSair() })
        conteudo.addView(criarBotaoSecundario(this, "Verificar atualizações") { verificarAtualizacaoManual() })

        raiz.addView(conteudo)
        setContentView(raiz)
    }

    private fun verificarAtualizacaoManual() {
        mostrarAviso(this, "Verificando atualizações...")
        var encontrou = false
        UpdateManager(this).verificarSilenciosamente { atualizacao ->
            encontrou = true
            AlertDialog.Builder(contextoTema(this))
                .setTitle("Nova versão disponível")
                .setMessage("Versão ${atualizacao.versionName} já está pronta. Baixar e instalar agora?")
                .setPositiveButton("Atualizar") { _, _ ->
                    UpdateManager(this).baixarEInstalar(
                        atualizacao,
                        onProgresso = { mensagem -> mostrarAviso(this, mensagem) },
                        onErro = { mensagem -> mostrarErro(this, mensagem) }
                    )
                }
                .setNegativeButton("Agora não", null)
                .show()
        }
        window.decorView.postDelayed({
            if (!encontrou) mostrarAviso(this, "Você já está na versão mais recente.")
        }, 4000)
    }

    private fun confirmarSair() {
        AlertDialog.Builder(contextoTema(this))
            .setTitle("Sair da conta")
            .setMessage("Tem certeza que quer sair?")
            .setPositiveButton("Sair") { _, _ ->
                GoogleAuthManager(this).sair {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

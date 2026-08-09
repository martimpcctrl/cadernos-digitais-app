package com.audiogames.cadernos

import android.os.Bundle
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Tela que aparece por cima de tudo quando o app volta pro primeiro
 * plano (igual o WhatsApp) - só passa daqui com a digital certa.
 */
class TelaBloqueioActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(criarTelaBase(this, "Cadernos Digitais bloqueado", mostrarVoltar = false))
        pedirBiometria()
    }

    private fun pedirBiometria() {
        val executor = ContextCompat.getMainExecutor(this)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(resultado: BiometricPrompt.AuthenticationResult) {
                CadernosApplication.desbloqueado = true
                finish()
            }

            override fun onAuthenticationError(codigoErro: Int, mensagem: CharSequence) {
                // Usuário cancelou, ou deu erro - fecha o app inteiro em vez
                // de deixar a tela de bloqueio "presa" sem saída.
                finishAffinity()
            }

            override fun onAuthenticationFailed() {
                // Impressão digital não reconhecida - o próprio sistema já
                // mostra isso visualmente, deixa a pessoa tentar de novo.
            }
        }

        val prompt = BiometricPrompt(this, executor, callback)

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Cadernos Digitais")
            .setSubtitle("Use sua digital pra continuar")
            .setNegativeButtonText("Cancelar")
            .build()

        prompt.authenticate(info)
    }

    override fun onBackPressed() {
        // Não deixa "voltar" pra escapar da tela de bloqueio.
        finishAffinity()
    }
}

package com.audiogames.cadernos

import android.os.Bundle
import androidx.biometric.BiometricManager
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
        TemaManager.aplicarTemaAtual(this)
        setContentView(criarTelaBase(this, "Cadernos Digitais bloqueado", mostrarVoltar = false))

        // Proteção importante: se a pessoa ativou o bloqueio, mas depois
        // removeu a digital/PIN do celular (fora do app), não dá pra pedir
        // uma credencial que não existe mais - isso trancaria o app pra
        // sempre. Nesse caso, desativa o bloqueio sozinho e deixa passar.
        if (!BloqueioManager.biometriaDisponivel(this)) {
            BloqueioManager.definirAtivado(this, false)
            CadernosApplication.desbloqueado = true
            android.widget.Toast.makeText(
                this,
                "O bloqueio foi desativado porque não há mais nenhuma digital, PIN, padrão ou senha configurados nesse celular.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

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
            .setSubtitle("Use sua digital, PIN, padrão ou senha pra continuar")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            // Importante: quando DEVICE_CREDENTIAL está incluído, o Android
            // não deixa definir um botão "Cancelar" customizado (ele já
            // mostra a opção de PIN/padrão/senha sozinho como alternativa) -
            // combinar os dois dá erro.
            .build()

        prompt.authenticate(info)
    }

    override fun onBackPressed() {
        // Não deixa "voltar" pra escapar da tela de bloqueio.
        finishAffinity()
    }
}

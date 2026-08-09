package com.audiogames.cadernos

import android.content.Context
import androidx.biometric.BiometricManager

/**
 * Guarda se o bloqueio do app está ativado ou não - fica salvo só nesse
 * aparelho (não sincroniza pela conta, já que depende da biometria
 * cadastrada nesse celular específico).
 */
object BloqueioManager {
    private const val PREFS = "bloqueio_app"
    private const val CHAVE_ATIVADO = "ativado"

    fun estaAtivado(context: Context): Boolean {
        return prefs(context).getBoolean(CHAVE_ATIVADO, false)
    }

    fun definirAtivado(context: Context, ativado: Boolean) {
        prefs(context).edit().putBoolean(CHAVE_ATIVADO, ativado).apply()
    }

    /** Confirma se o aparelho tem alguma forma de trava configurada (digital, PIN, padrão ou senha). */
    fun biometriaDisponivel(context: Context): Boolean {
        val gerenciador = BiometricManager.from(context)
        val resultado = gerenciador.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return resultado == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

package com.audiogames.cadernos

import android.content.Context
import android.content.res.Configuration

/**
 * Guarda a preferência de tema (claro, escuro, ou seguir o sistema) -
 * fica salvo só nesse aparelho.
 */
object TemaManager {
    private const val PREFS = "tema_app"
    private const val CHAVE_TEMA = "modo"

    const val CLARO = "claro"
    const val ESCURO = "escuro"
    const val SISTEMA = "sistema"

    fun obterModo(context: Context): String {
        return prefs(context).getString(CHAVE_TEMA, SISTEMA) ?: SISTEMA
    }

    fun definirModo(context: Context, modo: String) {
        prefs(context).edit().putString(CHAVE_TEMA, modo).apply()
    }

    /** Decide se deve mostrar escuro AGORA, considerando a preferência e (se for "sistema") o modo do Android. */
    fun deveUsarEscuroAgora(context: Context): Boolean {
        return when (obterModo(context)) {
            CLARO -> false
            ESCURO -> true
            else -> {
                val modoNoturnoDoSistema = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                modoNoturnoDoSistema == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    /** Chame isso bem no início do onCreate de cada tela, antes de montar a interface. */
    fun aplicarTemaAtual(context: Context) {
        Cores.aplicarTema(deveUsarEscuroAgora(context))
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

package com.audiogames.cadernos

import android.content.Context

/** Guarda o token de notificação desse aparelho localmente, pra uso rápido (sem esperar chamada assíncrona). */
object TokenFcmCache {
    private const val PREFS = "token_fcm"
    private const val CHAVE = "token"
    private var appContext: Context? = null

    fun iniciar(context: Context) {
        appContext = context.applicationContext
    }

    fun salvar(token: String) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(CHAVE, token)?.apply()
    }

    fun obter(): String? {
        return appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getString(CHAVE, null)
    }
}

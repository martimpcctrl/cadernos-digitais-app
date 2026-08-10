package com.audiogames.cadernos

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle

/**
 * Detecta quando o app volta do segundo plano (não só troca de tela
 * dentro do próprio app) - é o mesmo tipo de mecanismo que WhatsApp e
 * bancos usam pra saber a hora certa de pedir a digital de novo.
 */
class CadernosApplication : Application() {

    companion object {
        /** Fica true depois que a biometria foi confirmada, até o app ir pro segundo plano de novo. */
        var desbloqueado: Boolean = false
    }

    private var atividadesVisiveis = 0

    override fun onCreate() {
        super.onCreate()

        TokenFcmCache.iniciar(this)
        CadernosMessagingService.criarCanalNotificacao(this)

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                val estavaEmSegundoPlano = atividadesVisiveis == 0
                atividadesVisiveis++

                if (estavaEmSegundoPlano && activity !is TelaBloqueioActivity) {
                    if (BloqueioManager.estaAtivado(activity) && !desbloqueado) {
                        activity.startActivity(
                            Intent(activity, TelaBloqueioActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        )
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                atividadesVisiveis--
                if (atividadesVisiveis == 0) {
                    // Voltou pro segundo plano - na próxima vez que abrir,
                    // pede a digital de novo.
                    desbloqueado = false
                }
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}

package com.audiogames.cadernos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.json.JSONObject

/**
 * Recebe as notificações push (quando um caderno/página é criado em
 * outra plataforma) e cuida de manter o "token" desse aparelho
 * atualizado com o servidor.
 */
class CadernosMessagingService : FirebaseMessagingService() {

    companion object {
        const val CANAL_ID = "cadernos_digitais_notificacoes"

        fun criarCanalNotificacao(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val canal = NotificationChannel(
                    CANAL_ID,
                    "Cadernos Digitais",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Avisa quando um caderno ou página é criado em outro aparelho"
                }
                val gerenciador = context.getSystemService(NotificationManager::class.java)
                gerenciador?.createNotificationChannel(canal)
            }
        }

        /** Manda o token atual pro servidor (chamado ao logar, e sempre que o token mudar sozinho). */
        fun registrarTokenAtual() {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> enviarTokenParaServidor(token) }
        }

        private fun enviarTokenParaServidor(token: String) {
            TokenFcmCache.salvar(token)
            val corpo = JSONObject().apply { put("token", token) }
            ApiClient.post("conta/registrar_token_fcm.php", corpo, onSucesso = {}, onErro = {})
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        enviarTokenParaServidor(token)
    }

    override fun onMessageReceived(mensagem: RemoteMessage) {
        super.onMessageReceived(mensagem)

        val titulo = mensagem.notification?.title ?: "Cadernos Digitais"
        val corpo = mensagem.notification?.body ?: ""

        criarCanalNotificacao(this)

        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificacao = NotificationCompat.Builder(this, CANAL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle(titulo)
            .setContentText(corpo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val gerenciador = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        gerenciador.notify(System.currentTimeMillis().toInt(), notificacao)
    }
}

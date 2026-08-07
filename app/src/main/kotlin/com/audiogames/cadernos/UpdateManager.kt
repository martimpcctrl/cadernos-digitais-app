package com.audiogames.cadernos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Verifica se existe uma versão mais nova do app publicada como Release
 * no GitHub, baixa o APK e abre o instalador do Android.
 */
class UpdateManager(private val context: Context) {

    data class Atualizacao(
        val versionCode: Int,
        val versionName: String,
        val apkUrl: String
    )

    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val REPO = "martimpcctrl/cadernos-digitais-app"
        private const val API_URL = "https://api.github.com/repos/$REPO/releases/latest"
    }

    fun verificarSilenciosamente(onEncontrada: (Atualizacao) -> Unit) {
        thread {
            try {
                val conn = URL(API_URL).openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/vnd.github+json")

                if (conn.responseCode != 200) {
                    conn.disconnect()
                    return@thread
                }

                val texto = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()

                val json = JSONObject(texto)
                val tag = json.optString("tag_name", "")
                val versionCodeRemoto = tag.substringAfterLast(".").toIntOrNull() ?: return@thread

                val assets = json.optJSONArray("assets") ?: return@thread
                var apkUrl: String? = null
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    if (asset.optString("name", "").endsWith(".apk")) {
                        apkUrl = asset.optString("browser_download_url", null)
                        break
                    }
                }
                if (apkUrl == null) return@thread

                if (versionCodeRemoto > BuildConfig.VERSION_CODE) {
                    val atualizacao = Atualizacao(versionCodeRemoto, tag, apkUrl)
                    handler.post { onEncontrada(atualizacao) }
                }
            } catch (e: Exception) {
                // Sem internet, GitHub fora do ar, etc. - ignora silenciosamente.
            }
        }
    }

    fun baixarEInstalar(atualizacao: Atualizacao, onProgresso: (String) -> Unit, onErro: (String) -> Unit) {
        thread {
            try {
                handler.post { onProgresso("Baixando atualização...") }

                val conn = URL(atualizacao.apkUrl).openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 15000
                conn.instanceFollowRedirects = true

                if (conn.responseCode != 200) {
                    handler.post { onErro("Não consegui baixar a atualização (erro ${conn.responseCode}).") }
                    conn.disconnect()
                    return@thread
                }

                val pasta = File(context.getExternalFilesDir(null), "atualizacoes")
                if (!pasta.exists()) pasta.mkdirs()
                val destino = File(pasta, "cadernos-update.apk")

                conn.inputStream.use { entrada ->
                    destino.outputStream().use { saida ->
                        entrada.copyTo(saida)
                    }
                }
                conn.disconnect()

                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    destino
                )

                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                handler.post {
                    onProgresso("Abrindo instalador...")
                    try {
                        context.startActivity(installIntent)
                    } catch (e: Exception) {
                        onErro("Não consegui abrir o instalador nesse aparelho.")
                    }
                }
            } catch (e: Exception) {
                handler.post { onErro("Erro ao baixar a atualização. Verifique sua conexão.") }
            }
        }
    }
}

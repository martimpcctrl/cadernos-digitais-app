package com.audiogames.cadernos

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Versão síncrona (bloqueante) de uma chamada GET simples - só faz
 * sentido usar de dentro de uma thread que você já controla (tipo o
 * widget), nunca na thread principal.
 */
object ApiClientSincrono {

    fun get(caminho: String, parametros: Map<String, String> = emptyMap()): JSONObject? {
        val usuario = FirebaseAuth.getInstance().currentUser ?: return null
        val token = try {
            Tasks.await(usuario.getIdToken(false)).token ?: return null
        } catch (e: Exception) {
            return null
        }

        val query = parametros.entries.joinToString("&") { (chave, valor) ->
            "${URLEncoder.encode(chave, "UTF-8")}=${URLEncoder.encode(valor, "UTF-8")}"
        }
        val url = if (query.isEmpty()) "${ApiClient.BASE_URL}/$caminho" else "${ApiClient.BASE_URL}/$caminho?$query"

        var conexao: HttpURLConnection? = null
        return try {
            conexao = URL(url).openConnection() as HttpURLConnection
            conexao.requestMethod = "GET"
            conexao.setRequestProperty("Authorization", "Bearer $token")
            conexao.connectTimeout = 10000
            conexao.readTimeout = 15000

            if (conexao.responseCode !in 200..299) return null

            val texto = conexao.inputStream.bufferedReader().use { it.readText() }
            JSONObject(texto)
        } catch (e: Exception) {
            null
        } finally {
            conexao?.disconnect()
        }
    }
}

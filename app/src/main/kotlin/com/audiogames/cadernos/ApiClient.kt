package com.audiogames.cadernos

import android.os.Handler
import android.os.Looper
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.concurrent.thread

/**
 * Fala com a API em PHP. Todo pedido roda numa thread separada, e o
 * resultado (sucesso ou erro) volta pra thread principal automaticamente
 * - a Activity não precisa se preocupar com isso.
 */
object ApiClient {

    const val BASE_URL = "https://audiogames.com.br/martim/api"

    private val handler = Handler(Looper.getMainLooper())

    class ErroApi(mensagem: String, val codigoHttp: Int) : Exception(mensagem)

    /** Pega o token do usuário logado agora, esperando de forma síncrona (chame só em thread de fundo). */
    private fun tokenAtual(): String? {
        val usuario = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            Tasks.await(usuario.getIdToken(false)).token
        } catch (e: Exception) {
            null
        }
    }

    fun get(
        caminho: String,
        parametros: Map<String, String> = emptyMap(),
        exigeLogin: Boolean = true,
        onSucesso: (JSONObject) -> Unit,
        onErro: (String) -> Unit
    ) {
        executar("GET", caminho, parametros, null, exigeLogin, onSucesso, onErro)
    }

    fun post(
        caminho: String,
        corpo: JSONObject = JSONObject(),
        exigeLogin: Boolean = true,
        onSucesso: (JSONObject) -> Unit,
        onErro: (String) -> Unit
    ) {
        executar("POST", caminho, emptyMap(), corpo, exigeLogin, onSucesso, onErro)
    }

    /** Baixa um arquivo binário (usado pra exportar PDF). */
    fun getBinario(
        caminho: String,
        parametros: Map<String, String> = emptyMap(),
        onSucesso: (ByteArray) -> Unit,
        onErro: (String) -> Unit
    ) {
        thread {
            try {
                val token = tokenAtual()
                if (token == null) {
                    postarErro(onErro, "Você precisa estar logado.")
                    return@thread
                }
                val url = montarUrl(caminho, parametros)
                val conexao = URL(url).openConnection() as HttpURLConnection
                conexao.requestMethod = "GET"
                conexao.setRequestProperty("Authorization", "Bearer $token")
                conexao.connectTimeout = 15000
                conexao.readTimeout = 30000

                if (conexao.responseCode != 200) {
                    postarErro(onErro, "Não foi possível baixar o arquivo (código ${conexao.responseCode}).")
                    return@thread
                }

                val bytes = conexao.inputStream.readBytes()
                handler.post { onSucesso(bytes) }
            } catch (e: Exception) {
                postarErro(onErro, "Falha de conexão: ${e.message}")
            }
        }
    }

    private fun executar(
        metodo: String,
        caminho: String,
        parametros: Map<String, String>,
        corpo: JSONObject?,
        exigeLogin: Boolean,
        onSucesso: (JSONObject) -> Unit,
        onErro: (String) -> Unit
    ) {
        thread {
            try {
                val token = if (exigeLogin) tokenAtual() else null
                if (exigeLogin && token == null) {
                    postarErro(onErro, "Sua sessão expirou. Entre novamente.")
                    return@thread
                }

                val url = montarUrl(caminho, parametros)
                val conexao = URL(url).openConnection() as HttpURLConnection
                conexao.requestMethod = metodo
                conexao.connectTimeout = 15000
                conexao.readTimeout = 20000
                if (token != null) {
                    conexao.setRequestProperty("Authorization", "Bearer $token")
                }

                if (corpo != null) {
                    conexao.doOutput = true
                    conexao.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    conexao.outputStream.use { it.write(corpo.toString().toByteArray(Charsets.UTF_8)) }
                }

                val codigo = conexao.responseCode
                val fluxo = if (codigo in 200..299) conexao.inputStream else conexao.errorStream
                val texto = fluxo?.bufferedReader()?.use { it.readText() } ?: ""
                val json = if (texto.isNotBlank()) JSONObject(texto) else JSONObject()

                if (codigo in 200..299 && json.optBoolean("sucesso", true)) {
                    handler.post { onSucesso(json) }
                } else {
                    val mensagem = json.optString("erro", "Erro inesperado (código $codigo).")
                    postarErro(onErro, mensagem)
                }
            } catch (e: Exception) {
                postarErro(onErro, "Falha de conexão: ${e.message ?: "verifique sua internet"}")
            }
        }
    }

    private fun montarUrl(caminho: String, parametros: Map<String, String>): String {
        val base = "$BASE_URL/$caminho"
        if (parametros.isEmpty()) return base
        val query = parametros.entries.joinToString("&") { (chave, valor) ->
            "${URLEncoder.encode(chave, "UTF-8")}=${URLEncoder.encode(valor, "UTF-8")}"
        }
        return "$base?$query"
    }

    private fun postarErro(onErro: (String) -> Unit, mensagem: String) {
        handler.post { onErro(mensagem) }
    }
}

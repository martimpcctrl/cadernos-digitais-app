package com.audiogames.cadernos

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Guarda um histórico local das últimas notificações push recebidas -
 * a barra de notificações do próprio Android some depois que a pessoa
 * mexe nela, então isso dá um jeito de rever o que já chegou dentro do
 * próprio app.
 */
object NotificacoesStore {
    private const val PREFS = "notificacoes_recebidas"
    private const val CHAVE_LISTA = "lista"
    private const val MAXIMO = 50 // não deixa crescer pra sempre

    fun salvar(context: Context, titulo: String, corpo: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val antiga = carregarArray(prefs)

        val novaEntrada = JSONObject().apply {
            put("titulo", titulo)
            put("corpo", corpo)
            put("recebidaEm", System.currentTimeMillis())
        }

        // Monta de novo com a mais recente primeiro, cortando no máximo.
        val novoArray = JSONArray()
        novoArray.put(novaEntrada)
        for (i in 0 until minOf(antiga.length(), MAXIMO - 1)) {
            novoArray.put(antiga.get(i))
        }

        prefs.edit().putString(CHAVE_LISTA, novoArray.toString()).apply()
    }

    fun listar(context: Context): List<JSONObject> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = carregarArray(prefs)
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    fun limpar(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun carregarArray(prefs: android.content.SharedPreferences): JSONArray {
        val bruto = prefs.getString(CHAVE_LISTA, "[]") ?: "[]"
        return try {
            JSONArray(bruto)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}

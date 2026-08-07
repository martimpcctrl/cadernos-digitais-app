package com.audiogames.cadernos

import org.json.JSONObject

data class Caderno(
    val id: String,
    val nome: String,
    val materia: String,
    val professor: String,
    val cor: String,
    val totalPaginas: Int,
    val tarefasPendentes: Int
) {
    companion object {
        fun deJson(json: JSONObject): Caderno = Caderno(
            id = json.optString("id"),
            nome = json.optString("nome"),
            materia = json.optString("materia"),
            professor = json.optString("professor"),
            cor = json.optString("cor", "#4A90D9"),
            totalPaginas = json.optInt("totalPaginas", 0),
            tarefasPendentes = json.optInt("tarefasPendentes", 0)
        )
    }
}

data class Pagina(
    val id: String,
    val cadernoId: String,
    val titulo: String,
    val conteudo: String,
    val tipo: String, // "nota" ou "tarefa"
    val dataEntrega: String,
    val concluida: Boolean,
    val criadoEm: String
) {
    val ehTarefa: Boolean get() = tipo == "tarefa"

    companion object {
        fun deJson(json: JSONObject): Pagina = Pagina(
            id = json.optString("id"),
            cadernoId = json.optString("cadernoId"),
            titulo = json.optString("titulo"),
            conteudo = json.optString("conteudo"),
            tipo = json.optString("tipo", "nota"),
            dataEntrega = json.optString("dataEntrega"),
            concluida = json.optBoolean("concluida", false),
            criadoEm = json.optString("criadoEm")
        )
    }
}

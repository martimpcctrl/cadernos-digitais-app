package com.audiogames.cadernos

/**
 * Lista de matérias prontas, cada uma com uma cor (tipo etiqueta de
 * caderno físico) - usada no diálogo de criar caderno.
 */
object Materias {
    data class Materia(val nome: String, val cor: String)

    val OUTRA = "Outra matéria..."

    val LISTA = listOf(
        Materia("Matemática", "#4A90D9"),
        Materia("Português", "#C0392B"),
        Materia("Redação", "#E67E22"),
        Materia("História", "#8E44AD"),
        Materia("Geografia", "#16A085"),
        Materia("Ciências", "#27AE60"),
        Materia("Física", "#2C3E50"),
        Materia("Química", "#F39C12"),
        Materia("Biologia", "#229954"),
        Materia("Inglês", "#2980B9"),
        Materia("Espanhol", "#D35400"),
        Materia("Educação Física", "#E74C3C"),
        Materia("Artes", "#9B59B6"),
        Materia("Filosofia", "#34495E"),
        Materia("Sociologia", "#1ABC9C"),
        Materia("Informática", "#7F8C8D"),
    )

    /** Nomes pra popular o seletor - a lista de matérias, mais "Escolher..." no topo e "Outra" no fim. */
    fun nomesParaSelecao(): List<String> {
        val nomes = mutableListOf("Escolher matéria (opcional)...")
        nomes.addAll(LISTA.map { it.nome })
        nomes.add(OUTRA)
        return nomes
    }

    fun corPara(nomeMateria: String): String {
        return LISTA.find { it.nome == nomeMateria }?.cor ?: "#4A90D9"
    }
}

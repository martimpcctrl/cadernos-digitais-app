package com.audiogames.cadernos

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdaptadorCadernos(
    private val activity: Activity,
    private var itens: List<Caderno>,
    private val aoClicar: (Caderno) -> Unit
) : RecyclerView.Adapter<AdaptadorCadernos.ViewHolder>() {

    class ViewHolder(val raiz: LinearLayout, val nome: TextView, val info: TextView) : RecyclerView.ViewHolder(raiz)

    fun atualizar(novos: List<Caderno>) {
        itens = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val margemH = dp(activity, 16)
        val margemV = dp(activity, 8)

        val raiz = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Cores.SUPERFICIE))
            setPadding(dp(activity, 16), dp(activity, 14), dp(activity, 16), dp(activity, 14))
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margemH, margemV / 2, margemH, margemV / 2) }
        }

        val nome = TextView(activity).apply {
            textSize = 17f
            setTextColor(Color.parseColor(Cores.TEXTO))
        }

        val info = TextView(activity).apply {
            textSize = 13f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            setPadding(0, dp(activity, 4), 0, 0)
        }

        raiz.addView(nome)
        raiz.addView(info)
        return ViewHolder(raiz, nome, info)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val caderno = itens[position]
        holder.nome.text = if (caderno.materia.isNotBlank()) "${caderno.nome} — ${caderno.materia}" else caderno.nome

        val partesInfo = mutableListOf("${caderno.totalPaginas} página(s)")
        if (caderno.professor.isNotBlank()) {
            partesInfo.add("Prof. ${caderno.professor}")
        }
        if (caderno.tarefasPendentes > 0) {
            partesInfo.add("${caderno.tarefasPendentes} tarefa(s) pendente(s)")
        }
        holder.info.text = partesInfo.joinToString(" · ")
        holder.info.setTextColor(
            Color.parseColor(if (caderno.tarefasPendentes > 0) Cores.ERRO else Cores.TEXTO_SECUNDARIO)
        )

        try {
            holder.raiz.setBackgroundColor(Color.parseColor(Cores.SUPERFICIE))
        } catch (e: Exception) { /* ignora cor inválida */ }

        holder.raiz.contentDescription = "${holder.nome.text}. ${holder.info.text}. Toque duas vezes pra abrir."
        holder.raiz.setOnClickListener { aoClicar(caderno) }
    }

    override fun getItemCount(): Int = itens.size
}

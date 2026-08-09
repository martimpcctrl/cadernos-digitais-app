package com.audiogames.cadernos

import android.app.Activity
import android.graphics.Color
import android.graphics.Paint
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdaptadorPaginas(
    private val activity: Activity,
    private var itens: List<Pagina>,
    private val aoClicar: (Pagina) -> Unit,
    private val aoMarcarConcluida: (Pagina, Boolean) -> Unit
) : RecyclerView.Adapter<AdaptadorPaginas.ViewHolder>() {

    class ViewHolder(
        val raiz: LinearLayout,
        val check: CheckBox,
        val titulo: TextView,
        val info: TextView
    ) : RecyclerView.ViewHolder(raiz)

    fun atualizar(novos: List<Pagina>) {
        itens = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val margemH = dp(activity, 16)
        val margemV = dp(activity, 6)

        val raiz = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor(Cores.SUPERFICIE))
            setPadding(dp(activity, 10), dp(activity, 10), dp(activity, 14), dp(activity, 10))
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(margemH, margemV / 2, margemH, margemV / 2) }
        }

        val check = CheckBox(activity)

        val coluna = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titulo = TextView(activity).apply {
            textSize = 16f
            setTextColor(Color.parseColor(Cores.TEXTO))
        }
        val info = TextView(activity).apply {
            textSize = 12f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
        }
        coluna.addView(titulo)
        coluna.addView(info)

        raiz.addView(check)
        raiz.addView(coluna)
        return ViewHolder(raiz, check, titulo, info)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pagina = itens[position]

        holder.titulo.text = pagina.titulo
        holder.titulo.paintFlags = if (pagina.concluida) {
            holder.titulo.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.titulo.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        val partes = mutableListOf(if (pagina.ehTarefa) "Tarefa" else "Nota")
        if (pagina.dataEntrega.isNotBlank()) partes.add("Entrega: ${pagina.dataEntrega}")
        holder.info.text = partes.joinToString(" · ")

        holder.check.visibility = if (pagina.ehTarefa) android.view.View.VISIBLE else android.view.View.INVISIBLE
        holder.check.setOnCheckedChangeListener(null)
        holder.check.isChecked = pagina.concluida
        holder.check.setOnCheckedChangeListener { _, marcado -> aoMarcarConcluida(pagina, marcado) }

        holder.raiz.contentDescription = "${if (pagina.ehTarefa) "Tarefa" else "Nota"}: ${pagina.titulo}." +
            if (pagina.concluida) " Concluída." else ""
        holder.raiz.setOnClickListener { aoClicar(pagina) }
    }

    override fun getItemCount(): Int = itens.size
}

package com.audiogames.cadernos

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast

object Cores {
    const val PRIMARIA = "#3A6EA5"
    const val FUNDO = "#F4F6F8"
    const val TEXTO = "#1C1C1C"
    const val TEXTO_SECUNDARIO = "#5B6470"
    const val BORDA = "#D8DEE4"
    const val ERRO = "#C0392B"
    const val SUCESSO = "#2E7D46"
}

fun dp(activity: Activity, valor: Int): Int =
    (valor * activity.resources.displayMetrics.density).toInt()

/** Monta o "esqueleto" padrão de tela: barra de título (com botão voltar opcional) + área de conteúdo. */
fun criarTelaBase(activity: Activity, titulo: String, mostrarVoltar: Boolean = true): LinearLayout {
    val raiz = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.parseColor(Cores.FUNDO))
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    val barra = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setBackgroundColor(Color.parseColor(Cores.PRIMARIA))
        setPadding(dp(activity, 12), dp(activity, 18), dp(activity, 16), dp(activity, 18))
    }

    if (mostrarVoltar) {
        val botaoVoltar = Button(activity).apply {
            text = "← Voltar"
            contentDescription = "Voltar"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.TRANSPARENT)
            setOnClickListener { activity.finish() }
        }
        barra.addView(botaoVoltar)
    }

    val tituloView = TextView(activity).apply {
        text = titulo
        textSize = 19f
        setTextColor(Color.WHITE)
        setTypeface(typeface, Typeface.BOLD)
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    barra.addView(tituloView)

    raiz.addView(barra)
    return raiz
}

fun criarBotaoPrimario(activity: Activity, texto: String, onClick: () -> Unit): Button {
    return Button(activity).apply {
        text = texto
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(Cores.PRIMARIA))
        setOnClickListener { onClick() }
    }
}

fun criarBotaoSecundario(activity: Activity, texto: String, onClick: () -> Unit): Button {
    return Button(activity).apply {
        text = texto
        setTextColor(Color.parseColor(Cores.PRIMARIA))
        setBackgroundColor(Color.WHITE)
        setOnClickListener { onClick() }
    }
}

fun criarCampoTexto(activity: Activity, dica: String, multilinha: Boolean = false): EditText {
    return EditText(activity).apply {
        hint = dica
        if (multilinha) {
            minLines = 4
            gravity = Gravity.TOP or Gravity.START
        }
        setPadding(dp(activity, 12), dp(activity, 12), dp(activity, 12), dp(activity, 12))
    }
}

fun criarTextoSecao(activity: Activity, texto: String): TextView {
    return TextView(activity).apply {
        text = texto
        textSize = 13f
        setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
        setPadding(0, dp(activity, 16), 0, dp(activity, 6))
    }
}

fun criarCarregando(activity: Activity): ProgressBar {
    return ProgressBar(activity).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { gravity = Gravity.CENTER; topMargin = dp(activity, 40) }
    }
}

fun criarMensagemVazio(activity: Activity, texto: String): TextView {
    return TextView(activity).apply {
        text = texto
        textSize = 15f
        gravity = Gravity.CENTER
        setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
        setPadding(dp(activity, 24), dp(activity, 48), dp(activity, 24), dp(activity, 24))
    }
}

fun mostrarErro(activity: Activity, mensagem: String) {
    Toast.makeText(activity, mensagem, Toast.LENGTH_LONG).show()
}

fun mostrarAviso(activity: Activity, mensagem: String) {
    Toast.makeText(activity, mensagem, Toast.LENGTH_SHORT).show()
}

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
    // Paleta clara (padrão)
    private const val PRIMARIA_CLARA = "#3A6EA5"
    private const val FUNDO_CLARO = "#F4F6F8"
    private const val SUPERFICIE_CLARA = "#FFFFFF"
    private const val TEXTO_CLARO = "#1C1C1C"
    private const val TEXTO_SECUNDARIO_CLARO = "#5B6470"
    private const val BORDA_CLARA = "#D8DEE4"
    private const val ERRO_CLARO = "#C0392B"
    private const val SUCESSO_CLARO = "#2E7D46"

    // Paleta escura
    private const val PRIMARIA_ESCURA = "#5B9BD9"
    private const val FUNDO_ESCURO = "#14181C"
    private const val SUPERFICIE_ESCURA = "#1E242A"
    private const val TEXTO_ESCURO = "#EEF1F3"
    private const val TEXTO_SECUNDARIO_ESCURO = "#93A0AB"
    private const val BORDA_ESCURA = "#2C343C"
    private const val ERRO_ESCURO = "#E57373"
    private const val SUCESSO_ESCURO = "#66BB6A"

    // Os valores "de verdade" que o resto do app usa - mudam quando o
    // tema troca. Começam na paleta clara, mas `aplicarTema()` é
    // chamado bem cedo (no início de cada tela) antes de qualquer
    // coisa ser desenhada.
    var PRIMARIA = PRIMARIA_CLARA
        private set
    var FUNDO = FUNDO_CLARO
        private set
    var SUPERFICIE = SUPERFICIE_CLARA
        private set
    var TEXTO = TEXTO_CLARO
        private set
    var TEXTO_SECUNDARIO = TEXTO_SECUNDARIO_CLARO
        private set
    var BORDA = BORDA_CLARA
        private set
    var ERRO = ERRO_CLARO
        private set
    var SUCESSO = SUCESSO_CLARO
        private set

    var escuroAtivo: Boolean = false
        private set

    fun aplicarTema(escuro: Boolean) {
        escuroAtivo = escuro
        if (escuro) {
            PRIMARIA = PRIMARIA_ESCURA
            FUNDO = FUNDO_ESCURO
            SUPERFICIE = SUPERFICIE_ESCURA
            TEXTO = TEXTO_ESCURO
            TEXTO_SECUNDARIO = TEXTO_SECUNDARIO_ESCURO
            BORDA = BORDA_ESCURA
            ERRO = ERRO_ESCURO
            SUCESSO = SUCESSO_ESCURO
        } else {
            PRIMARIA = PRIMARIA_CLARA
            FUNDO = FUNDO_CLARO
            SUPERFICIE = SUPERFICIE_CLARA
            TEXTO = TEXTO_CLARO
            TEXTO_SECUNDARIO = TEXTO_SECUNDARIO_CLARO
            BORDA = BORDA_CLARA
            ERRO = ERRO_CLARO
            SUCESSO = SUCESSO_CLARO
        }
    }
}

fun dp(activity: Activity, valor: Int): Int =
    (valor * activity.resources.displayMetrics.density).toInt()

/**
 * Diálogos e alguns controles nativos (Switch, Spinner, CheckBox) seguem
 * o tema do SISTEMA por padrão, não a paleta escolhida dentro do app -
 * isso causa uma mistura estranha (diálogo claro numa tela escura, por
 * exemplo). Essa função devolve um "contexto" que força esses controles
 * a usar o tema certo, de acordo com o que está ativo no app agora.
 */
fun contextoTema(activity: Activity): android.content.Context {
    val temaId = if (Cores.escuroAtivo) {
        android.R.style.Theme_DeviceDefault
    } else {
        android.R.style.Theme_DeviceDefault_Light
    }
    return android.view.ContextThemeWrapper(activity, temaId)
}

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

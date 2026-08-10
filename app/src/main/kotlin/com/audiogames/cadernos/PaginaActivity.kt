package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONObject

class PaginaActivity : Activity() {

    private lateinit var paginaId: String
    private lateinit var titulo: String
    private lateinit var conteudo: String
    private lateinit var tipo: String
    private var cadernoIdRef: String = ""
    private var cadernoNomeRef: String = ""
    private var professorRef: String = ""
    private var temFoto: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        paginaId = intent.getStringExtra("paginaId") ?: run { finish(); return }
        titulo = intent.getStringExtra("titulo") ?: ""
        conteudo = intent.getStringExtra("conteudo") ?: ""
        tipo = intent.getStringExtra("tipo") ?: "nota"
        val dataEntrega = intent.getStringExtra("dataEntrega") ?: ""
        cadernoIdRef = intent.getStringExtra("cadernoId") ?: ""
        cadernoNomeRef = intent.getStringExtra("cadernoNome") ?: ""
        professorRef = intent.getStringExtra("professor") ?: ""
        temFoto = intent.getBooleanExtra("temFoto", false)

        montarTela(dataEntrega)
    }

    private fun montarTela(dataEntrega: String) {
        val raiz = criarTelaBase(this, if (tipo == "tarefa") "Tarefa" else "Nota")
        val padding = dp(this, 20)

        val corpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val tituloView = TextView(this).apply {
            text = titulo
            textSize = 22f
            setTextColor(Color.parseColor(Cores.TEXTO))
        }

        val metaView = TextView(this).apply {
            text = if (dataEntrega.isNotBlank()) "Entrega: $dataEntrega" else ""
            textSize = 13f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            visibility = if (dataEntrega.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        }

        val conteudoView = TextView(this).apply {
            text = conteudo.ifBlank { "(sem conteúdo)" }
            textSize = 16f
            setTextColor(Color.parseColor(Cores.TEXTO))
            setPadding(0, dp(this@PaginaActivity, 16), 0, dp(this@PaginaActivity, 24))
        }

        corpo.addView(tituloView)
        corpo.addView(metaView)
        corpo.addView(conteudoView)

        if (temFoto) {
            val imagemView = android.widget.ImageView(this).apply {
                adjustViewBounds = true
                scaleType = android.widget.ImageView.ScaleType.FIT_CENTER
                setPadding(0, 0, 0, dp(this@PaginaActivity, 20))
                contentDescription = "Foto anexada à página"
            }
            val statusFoto = TextView(this).apply {
                text = "Carregando foto..."
                textSize = 13f
                setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
                setPadding(0, 0, 0, dp(this@PaginaActivity, 8))
            }
            corpo.addView(statusFoto)
            corpo.addView(imagemView)

            ApiClient.getBinario("paginas/foto.php", mapOf("id" to paginaId), onSucesso = { bytes ->
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                if (bitmap != null) {
                    imagemView.setImageBitmap(bitmap)
                    statusFoto.visibility = android.view.View.GONE
                } else {
                    statusFoto.text = "Não consegui abrir a foto."
                }
            }, onErro = { mensagem -> statusFoto.text = mensagem })
        }

        val botaoEmail = criarBotaoSecundario(this, "Enviar por e-mail") { enviarPorEmail() }
        val botaoExcluir = criarBotaoSecundario(this, "Excluir página") { confirmarExcluir() }

        corpo.addView(botaoEmail)
        corpo.addView(botaoExcluir)

        raiz.addView(ScrollView(this).apply {
            addView(corpo)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun enviarPorEmail() {
        val intent = Intent(this, EnviarEmailActivity::class.java)
        intent.putExtra("cadernoId", cadernoIdRef)
        intent.putExtra("cadernoNome", cadernoNomeRef)
        intent.putExtra("professor", professorRef)
        startActivity(intent)
    }

    private fun confirmarExcluir() {
        AlertDialog.Builder(contextoTema(this))
            .setTitle("Excluir página")
            .setMessage("Tem certeza que quer excluir \"$titulo\"? Não dá pra desfazer.")
            .setPositiveButton("Excluir") { _, _ ->
                val corpo = JSONObject().apply { put("paginaId", paginaId) }
                ApiClient.post("paginas/excluir.php", corpo, onSucesso = {
                    mostrarAviso(this, "Página excluída.")
                    finish()
                }, onErro = { mensagem -> mostrarErro(this, mensagem) })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

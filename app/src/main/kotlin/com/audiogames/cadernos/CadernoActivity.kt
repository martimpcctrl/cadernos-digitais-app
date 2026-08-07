package com.audiogames.cadernos

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import org.json.JSONArray
import org.json.JSONObject

class CadernoActivity : Activity() {

    private lateinit var cadernoId: String
    private lateinit var cadernoNome: String
    private lateinit var adaptador: AdaptadorPaginas
    private lateinit var refresh: SwipeRefreshLayout
    private lateinit var containerVazio: LinearLayout
    private lateinit var lista: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cadernoId = intent.getStringExtra("cadernoId") ?: run { finish(); return }
        cadernoNome = intent.getStringExtra("cadernoNome") ?: "Caderno"
        montarTela()
        carregarPaginas()
    }

    override fun onResume() {
        super.onResume()
        if (::adaptador.isInitialized) carregarPaginas()
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, cadernoNome)

        val acoes = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(this@CadernoActivity, 12), dp(this@CadernoActivity, 10), dp(this@CadernoActivity, 12), dp(this@CadernoActivity, 4))
        }
        acoes.addView(botaoAcao("Resumo IA") { abrirResumoIa() })
        acoes.addView(botaoAcao("Exportar PDF") { exportarPdf() })
        acoes.addView(botaoAcao("Compartilhar") { compartilhar() })
        acoes.addView(botaoAcao("Excluir") { confirmarExcluirCaderno() })

        adaptador = AdaptadorPaginas(
            this,
            emptyList(),
            aoClicar = { pagina -> abrirPagina(pagina) },
            aoMarcarConcluida = { pagina, marcada -> marcarConcluida(pagina, marcada) }
        )
        lista = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@CadernoActivity)
            adapter = adaptador
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        containerVazio = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = android.view.View.GONE
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            addView(criarMensagemVazio(this@CadernoActivity, "Esse caderno ainda não tem páginas.\nToque em \"+ Nova página\" pra começar."))
        }
        val pilha = FrameLayout(this).apply {
            addView(lista)
            addView(containerVazio)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        refresh = SwipeRefreshLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
            setOnRefreshListener { carregarPaginas() }
            addView(pilha)
        }

        val botaoNova = criarBotaoPrimario(this, "+ Nova página") { abrirNovaPagina() }
        val rodape = LinearLayout(this).apply {
            setPadding(dp(this@CadernoActivity, 16), dp(this@CadernoActivity, 8), dp(this@CadernoActivity, 16), dp(this@CadernoActivity, 16))
            addView(botaoNova)
        }

        raiz.addView(acoes)
        raiz.addView(refresh)
        raiz.addView(rodape)
        setContentView(raiz)
    }

    private fun botaoAcao(texto: String, onClick: () -> Unit) = criarBotaoSecundario(this, texto, onClick).apply {
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
            marginEnd = dp(this@CadernoActivity, 4)
        }
        textSize = 11f
    }

    private fun carregarPaginas() {
        refresh.isRefreshing = true
        ApiClient.get("paginas/listar.php", mapOf("cadernoId" to cadernoId), onSucesso = { json ->
            refresh.isRefreshing = false
            val array = json.optJSONArray("paginas") ?: JSONArray()
            val paginas = (0 until array.length()).map { Pagina.deJson(array.getJSONObject(it)) }
            adaptador.atualizar(paginas)
            containerVazio.visibility = if (paginas.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            lista.visibility = if (paginas.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE
        }, onErro = { mensagem ->
            refresh.isRefreshing = false
            mostrarErro(this, mensagem)
        })
    }

    private fun abrirPagina(pagina: Pagina) {
        val intent = Intent(this, PaginaActivity::class.java)
        intent.putExtra("paginaId", pagina.id)
        intent.putExtra("titulo", pagina.titulo)
        intent.putExtra("conteudo", pagina.conteudo)
        intent.putExtra("tipo", pagina.tipo)
        intent.putExtra("dataEntrega", pagina.dataEntrega)
        intent.putExtra("concluida", pagina.concluida)
        startActivity(intent)
    }

    private fun abrirNovaPagina() {
        val intent = Intent(this, NovaPaginaActivity::class.java)
        intent.putExtra("cadernoId", cadernoId)
        startActivity(intent)
    }

    private fun marcarConcluida(pagina: Pagina, concluida: Boolean) {
        val corpo = JSONObject().apply {
            put("paginaId", pagina.id)
            put("concluida", concluida)
        }
        ApiClient.post("paginas/concluir.php", corpo, onSucesso = { carregarPaginas() }, onErro = { mensagem ->
            mostrarErro(this, mensagem)
            carregarPaginas()
        })
    }

    private fun abrirResumoIa() {
        AlertDialog.Builder(this).setTitle("Gerando resumo...").setMessage("Aguarde um instante.").show()
        ApiClient.get("resumo/ia.php", mapOf("cadernoId" to cadernoId), onSucesso = { json ->
            AlertDialog.Builder(this)
                .setTitle("Resumo com IA")
                .setMessage(json.optString("resumo"))
                .setPositiveButton("Fechar", null)
                .show()
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun exportarPdf() {
        mostrarAviso(this, "Baixando PDF...")
        ApiClient.getBinario("exportar/pdf.php", mapOf("cadernoId" to cadernoId), onSucesso = { bytes ->
            try {
                val pasta = java.io.File(getExternalFilesDir(null), "pdfs")
                if (!pasta.exists()) pasta.mkdirs()
                val arquivo = java.io.File(pasta, "$cadernoNome.pdf")
                arquivo.writeBytes(bytes)

                val uri = androidx.core.content.FileProvider.getUriForFile(this, "$packageName.fileprovider", arquivo)
                val intentAbrir = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    startActivity(intentAbrir)
                } catch (e: Exception) {
                    mostrarAviso(this, "PDF salvo em Arquivos > Android > data > $packageName > files > pdfs")
                }
            } catch (e: Exception) {
                mostrarErro(this, "Não consegui salvar o PDF: ${e.message}")
            }
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    private fun compartilhar() {
        val intent = Intent(this, CompartilharActivity::class.java)
        intent.putExtra("cadernoId", cadernoId)
        intent.putExtra("cadernoNome", cadernoNome)
        startActivity(intent)
    }

    private fun confirmarExcluirCaderno() {
        AlertDialog.Builder(this)
            .setTitle("Excluir caderno")
            .setMessage("Isso vai apagar \"$cadernoNome\" e todas as páginas dentro dele. Não dá pra desfazer.")
            .setPositiveButton("Excluir") { _, _ ->
                val corpo = JSONObject().apply { put("cadernoId", cadernoId) }
                ApiClient.post("cadernos/excluir.php", corpo, onSucesso = {
                    mostrarAviso(this, "Caderno excluído.")
                    finish()
                }, onErro = { mensagem -> mostrarErro(this, mensagem) })
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}

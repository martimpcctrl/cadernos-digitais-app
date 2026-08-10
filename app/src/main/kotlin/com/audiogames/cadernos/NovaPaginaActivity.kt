package com.audiogames.cadernos

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import org.json.JSONObject
import java.util.Calendar

class NovaPaginaActivity : Activity() {

    private lateinit var cadernoId: String
    private lateinit var campoTitulo: android.widget.EditText
    private lateinit var campoConteudo: android.widget.EditText
    private lateinit var grupoTipo: RadioGroup
    private lateinit var botaoData: android.widget.Button
    private var dataEntregaEscolhida: String? = null
    private var caminhoFotoAnexada: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        cadernoId = intent.getStringExtra("cadernoId") ?: run { finish(); return }
        montarTela()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OCR && resultCode == RESULT_OK) {
            val textoLido = data?.getStringExtra("textoExtraido") ?: return
            val atual = campoConteudo.text.toString()
            campoConteudo.setText(if (atual.isBlank()) textoLido else "$atual\n$textoLido")

            caminhoFotoAnexada = data.getStringExtra("caminhoFoto")
            if (caminhoFotoAnexada != null) {
                mostrarAviso(this, "Foto anexada! Ela vai ser salva junto com a página.")
            }
        }
    }

    private fun montarTela() {
        val raiz = criarTelaBase(this, "Nova página")
        val padding = dp(this, 16)

        val conteudo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        campoTitulo = criarCampoTexto(this, "Título")

        grupoTipo = RadioGroup(contextoTema(this)).apply {
            orientation = RadioGroup.HORIZONTAL
            val opcaoNota = RadioButton(contextoTema(this@NovaPaginaActivity)).apply { text = "Nota"; id = 1; isChecked = true }
            val opcaoTarefa = RadioButton(contextoTema(this@NovaPaginaActivity)).apply { text = "Tarefa"; id = 2 }
            addView(opcaoNota)
            addView(opcaoTarefa)
            setOnCheckedChangeListener { _, checkedId -> botaoData.visibility = if (checkedId == 2) android.view.View.VISIBLE else android.view.View.GONE }
        }

        botaoData = criarBotaoSecundario(this, "Escolher data de entrega") { abrirSeletorData() }.apply {
            visibility = android.view.View.GONE
        }

        val botaoOcr = criarBotaoSecundario(this, "📷 Lousa Digital (tirar foto e extrair texto)") {
            startActivityForResult(Intent(this, OcrActivity::class.java), REQUEST_CODE_OCR)
        }

        campoConteudo = criarCampoTexto(this, "Conteúdo da página", multilinha = true)

        val botaoSalvar = criarBotaoPrimario(this, "Salvar página") { salvarPagina() }

        conteudo.addView(campoTitulo)
        conteudo.addView(criarTextoSecao(this, "Tipo"))
        conteudo.addView(grupoTipo)
        conteudo.addView(botaoData)
        conteudo.addView(criarTextoSecao(this, "Conteúdo"))
        conteudo.addView(botaoOcr)
        conteudo.addView(campoConteudo)
        conteudo.addView(botaoSalvar)

        raiz.addView(ScrollView(this).apply {
            addView(conteudo)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(raiz)
    }

    private fun abrirSeletorData() {
        val agora = Calendar.getInstance()
        DatePickerDialog(this, { _, ano, mes, dia ->
            dataEntregaEscolhida = "%04d-%02d-%02d".format(ano, mes + 1, dia)
            botaoData.text = "Entrega: $dia/${mes + 1}/$ano"
        }, agora.get(Calendar.YEAR), agora.get(Calendar.MONTH), agora.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun salvarPagina() {
        val titulo = campoTitulo.text.toString().trim()
        if (titulo.isEmpty()) {
            mostrarErro(this, "Digite um título pra página.")
            return
        }
        val tipo = if (grupoTipo.checkedRadioButtonId == 2) "tarefa" else "nota"

        val corpo = JSONObject().apply {
            put("cadernoId", cadernoId)
            put("titulo", titulo)
            put("conteudo", campoConteudo.text.toString())
            put("tipo", tipo)
            put("dataEntrega", dataEntregaEscolhida ?: "")
            TokenFcmCache.obter()?.let { put("meuTokenFcm", it) }

            caminhoFotoAnexada?.let { caminho ->
                val arquivo = java.io.File(caminho)
                if (arquivo.exists()) {
                    val bytes = arquivo.readBytes()
                    put("imagemBase64", android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP))
                }
            }
        }

        ApiClient.post("paginas/criar.php", corpo, onSucesso = {
            java.io.File(caminhoFotoAnexada ?: "").delete()  // não precisa mais guardar localmente, já subiu
            mostrarAviso(this, "Página salva!")
            finish()
        }, onErro = { mensagem -> mostrarErro(this, mensagem) })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Se tirou foto mas não chegou a salvar a página (voltou sem
        // salvar), não deixa o arquivo esquecido no celular. Se já foi
        // salva com sucesso, o arquivo já não existe mais nesse ponto
        // (apagado em salvarPagina()), então isso não faz nada nesse caso.
        caminhoFotoAnexada?.let { java.io.File(it).delete() }
    }

    companion object {
        private const val REQUEST_CODE_OCR = 501
    }
}

package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File

class OcrActivity : Activity() {

    private lateinit var textoStatus: TextView
    private var arquivoFoto: File? = null
    private var uriFoto: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        montarTela()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CODE_PERMISSAO)
        } else {
            abrirCamera()
        }
    }

    private fun montarTela() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(this@OcrActivity, 24), dp(this@OcrActivity, 24), dp(this@OcrActivity, 24), dp(this@OcrActivity, 24))
        }
        textoStatus = TextView(this).apply {
            text = "Lousa Digital — abrindo a câmera..."
            textSize = 16f
            gravity = Gravity.CENTER
        }
        layout.addView(textoStatus)
        setContentView(layout)
    }

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSAO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                abrirCamera()
            } else {
                mostrarErro(this, "Preciso da permissão da câmera pra usar a Lousa Digital.")
                finish()
            }
        }
    }

    private fun abrirCamera() {
        val pasta = File(getExternalFilesDir(null), "fotos")
        if (!pasta.exists()) pasta.mkdirs()
        val arquivo = File(pasta, "ocr_${System.currentTimeMillis()}.jpg")
        arquivoFoto = arquivo
        uriFoto = FileProvider.getUriForFile(this, "$packageName.fileprovider", arquivo)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, uriFoto)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }

        try {
            startActivityForResult(intent, REQUEST_CODE_CAMERA)
        } catch (e: Exception) {
            mostrarErro(this, "Não encontrei um app de câmera nesse aparelho.")
            finish()
        }
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQUEST_CODE_CAMERA) return

        if (resultCode != RESULT_OK) {
            arquivoFoto?.delete()
            finish()
            return
        }

        textoStatus.text = "Processando imagem..."
        Thread {
            try {
                val (base64, arquivoComprimido) = comprimirEGuardar(arquivoFoto!!)
                runOnUiThread { textoStatus.text = "Lendo o texto da foto..." }
                enviarParaOcr(base64, arquivoComprimido)
            } catch (e: Exception) {
                runOnUiThread {
                    mostrarErro(this, "Não consegui processar a foto: ${e.message}")
                    arquivoFoto?.delete()
                    finish()
                }
            }
        }.start()
    }

    /** Comprime a foto, salva um arquivo menor (que fica guardado), e apaga o original (tamanho cheio da câmera). */
    private fun comprimirEGuardar(arquivoOriginal: File): Pair<String, File> {
        val opcoes = BitmapFactory.Options().apply { inSampleSize = 2 }
        val bitmapOriginal = BitmapFactory.decodeFile(arquivoOriginal.absolutePath, opcoes)
            ?: throw IllegalStateException("Não consegui ler a foto tirada.")

        val bitmap = redimensionarSeNecessario(bitmapOriginal, 1600)
        val bytesComprimidos = ByteArrayOutputStream().also {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, it)
        }.toByteArray()

        val arquivoComprimido = File(arquivoOriginal.parentFile, "comprimida_${arquivoOriginal.name}")
        arquivoComprimido.writeBytes(bytesComprimidos)
        arquivoOriginal.delete()  // não precisa mais do tamanho cheio da câmera

        val base64 = android.util.Base64.encodeToString(bytesComprimidos, android.util.Base64.NO_WRAP)
        return Pair(base64, arquivoComprimido)
    }

    private fun redimensionarSeNecessario(bitmap: Bitmap, ladoMaximo: Int): Bitmap {
        val maiorLado = maxOf(bitmap.width, bitmap.height)
        if (maiorLado <= ladoMaximo) return bitmap
        val escala = ladoMaximo.toFloat() / maiorLado
        val novaLargura = (bitmap.width * escala).toInt()
        val novaAltura = (bitmap.height * escala).toInt()
        return Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true)
    }

    private fun enviarParaOcr(imagemBase64: String, arquivoComprimido: File) {
        val corpo = JSONObject().apply { put("imagemBase64", imagemBase64) }
        ApiClient.post("ocr/extrair.php", corpo, onSucesso = { json ->
            val texto = json.optString("texto")
            val resultado = Intent().apply {
                putExtra("textoExtraido", texto)
                putExtra("caminhoFoto", arquivoComprimido.absolutePath)
            }
            setResult(RESULT_OK, resultado)
            finish()
        }, onErro = { mensagem ->
            mostrarErro(this, mensagem)
            arquivoComprimido.delete()
            finish()
        })
    }

    companion object {
        private const val REQUEST_CODE_CAMERA = 601
        private const val REQUEST_CODE_PERMISSAO = 602
    }
}

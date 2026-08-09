package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : Activity() {

    private lateinit var googleAuth: GoogleAuthManager
    private lateinit var textoStatus: TextView

    private lateinit var campoNome: EditText
    private lateinit var campoEmail: EditText
    private lateinit var campoSenha: EditText
    private lateinit var linhaNome: LinearLayout
    private lateinit var botaoAcaoPrincipalRef: android.widget.Button

    /** true = tela de criar conta (mostra campo de nome), false = tela de entrar. */
    private var modoCriarConta = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TemaManager.aplicarTemaAtual(this)
        googleAuth = GoogleAuthManager(this)

        if (googleAuth.estaLogado()) {
            irParaDashboard()
            return
        }

        montarTela()
    }

    private fun montarTela() {
        val padding = dp(this, 28)

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor(Cores.FUNDO))
            setPadding(padding, dp(this@LoginActivity, 48), padding, padding)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }

        val titulo = TextView(this).apply {
            text = "Cadernos Digitais"
            textSize = 28f
            setTextColor(Color.parseColor(Cores.TEXTO))
            gravity = Gravity.CENTER
        }

        val subtitulo = TextView(this).apply {
            text = "Suas anotações e tarefas escolares, organizadas e sempre com você."
            textSize = 15f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            gravity = Gravity.CENTER
            setPadding(0, dp(this@LoginActivity, 12), 0, dp(this@LoginActivity, 32))
        }

        val botaoGoogle = criarBotaoPrimario(this, "Entrar com Google") { iniciarLoginGoogle() }

        val separador = TextView(this).apply {
            text = "ou, com email e senha"
            textSize = 13f
            setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
            gravity = Gravity.CENTER
            setPadding(0, dp(this@LoginActivity, 24), 0, dp(this@LoginActivity, 8))
        }

        linhaNome = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }
        campoNome = criarCampoTexto(this, "Seu nome")
        linhaNome.addView(campoNome)

        campoEmail = criarCampoTexto(this, "Email")
        campoSenha = criarCampoTexto(this, "Senha (mínimo 6 caracteres)").apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        val botaoAcaoPrincipal = criarBotaoPrimario(this, "Entrar") { executarAcaoEmailSenha() }
        val botaoAlternarModo = criarBotaoSecundario(this, "Não tem conta? Criar uma agora") { alternarModo() }
        val botaoEsqueciSenha = criarBotaoSecundario(this, "Esqueci minha senha") { recuperarSenha() }

        textoStatus = TextView(this).apply {
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor(Cores.ERRO))
            setPadding(0, dp(this@LoginActivity, 16), 0, 0)
        }

        layout.addView(titulo)
        layout.addView(subtitulo)
        layout.addView(botaoGoogle)
        layout.addView(separador)
        layout.addView(linhaNome)
        layout.addView(campoEmail)
        layout.addView(campoSenha)
        layout.addView(botaoAcaoPrincipal)
        layout.addView(botaoAlternarModo)
        layout.addView(botaoEsqueciSenha)
        layout.addView(textoStatus)

        botaoAcaoPrincipalRef = botaoAcaoPrincipal

        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun alternarModo() {
        modoCriarConta = !modoCriarConta
        linhaNome.visibility = if (modoCriarConta) View.VISIBLE else View.GONE
        botaoAcaoPrincipalRef.text = if (modoCriarConta) "Criar conta" else "Entrar"
        textoStatus.text = ""
    }

    private fun executarAcaoEmailSenha() {
        val email = campoEmail.text.toString().trim()
        val senha = campoSenha.text.toString()

        if (email.isEmpty() || senha.isEmpty()) {
            textoStatus.text = "Preencha email e senha."
            return
        }
        if (senha.length < 6) {
            textoStatus.text = "A senha precisa ter pelo menos 6 caracteres."
            return
        }

        if (modoCriarConta) {
            val nome = campoNome.text.toString().trim()
            if (nome.isEmpty()) {
                textoStatus.text = "Digite seu nome."
                return
            }
            criarConta(nome, email, senha)
        } else {
            entrar(email, senha)
        }
    }

    private fun criarConta(nome: String, email: String, senha: String) {
        textoStatus.setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
        textoStatus.text = "Criando conta..."
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, senha)
            .addOnSuccessListener { resultado ->
                val perfil = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                    .setDisplayName(nome)
                    .build()
                resultado.user?.updateProfile(perfil)?.addOnCompleteListener {
                    // Salva o nome direto no servidor também - o token de
                    // login às vezes ainda não traz o nome atualizado logo
                    // depois de criar a conta, então não dá pra confiar só
                    // nele pra essa primeira gravação.
                    val corpo = org.json.JSONObject().apply { put("nomeAluno", nome) }
                    ApiClient.post("conta/configuracoes.php", corpo, onSucesso = { irParaDashboard() }, onErro = { irParaDashboard() })
                } ?: irParaDashboard()
            }
            .addOnFailureListener { erro -> mostrarErroFirebase(erro) }
    }

    private fun entrar(email: String, senha: String) {
        textoStatus.setTextColor(Color.parseColor(Cores.TEXTO_SECUNDARIO))
        textoStatus.text = "Entrando..."
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, senha)
            .addOnSuccessListener { irParaDashboard() }
            .addOnFailureListener { erro -> mostrarErroFirebase(erro) }
    }

    private fun recuperarSenha() {
        val email = campoEmail.text.toString().trim()
        if (email.isEmpty()) {
            textoStatus.setTextColor(Color.parseColor(Cores.ERRO))
            textoStatus.text = "Digite seu email no campo acima primeiro."
            return
        }
        FirebaseAuth.getInstance().sendPasswordResetEmail(email)
            .addOnSuccessListener {
                textoStatus.setTextColor(Color.parseColor(Cores.SUCESSO))
                textoStatus.text = "Enviamos um link de redefinição pro seu email."
            }
            .addOnFailureListener { erro -> mostrarErroFirebase(erro) }
    }

    private fun mostrarErroFirebase(erro: Exception) {
        textoStatus.setTextColor(Color.parseColor(Cores.ERRO))
        textoStatus.text = when {
            erro.message?.contains("badly formatted") == true -> "Email inválido."
            erro.message?.contains("no user record") == true -> "Não existe conta com esse email."
            erro.message?.contains("password is invalid") == true -> "Senha incorreta."
            erro.message?.contains("already in use") == true -> "Já existe uma conta com esse email. Tente entrar."
            erro.message?.contains("network") == true -> "Sem conexão com a internet."
            else -> erro.message ?: "Algo deu errado. Tente novamente."
        }
    }

    private fun iniciarLoginGoogle() {
        startActivityForResult(googleAuth.iniciarLogin(), GoogleAuthManager.REQUEST_CODE_GOOGLE_SIGN_IN)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != GoogleAuthManager.REQUEST_CODE_GOOGLE_SIGN_IN) return

        googleAuth.tratarResultadoLogin(
            data,
            onSucesso = { irParaDashboard() },
            onErro = { mensagem -> textoStatus.text = mensagem }
        )
    }

    private fun irParaDashboard() {
        startActivity(Intent(this, DashboardActivity::class.java))
        finish()
    }
}

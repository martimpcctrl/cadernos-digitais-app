package com.audiogames.cadernos

import android.app.Activity
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

class GoogleAuthManager(private val activity: Activity) {

    private val googleClient: GoogleSignInClient by lazy {
        val webClientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            null
        }
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .apply { if (webClientId != null) requestIdToken(webClientId) }
            .requestEmail()
            .build()
        GoogleSignIn.getClient(activity, options)
    }

    fun usuarioAtual(): FirebaseUser? = FirebaseAuth.getInstance().currentUser

    fun estaLogado(): Boolean = usuarioAtual() != null

    fun iniciarLogin(): Intent = googleClient.signInIntent

    fun tratarResultadoLogin(data: Intent?, onSucesso: (FirebaseUser) -> Unit, onErro: (String) -> Unit) {
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            val conta = task.getResult(ApiException::class.java)
            val idToken = conta?.idToken

            if (idToken == null) {
                onErro("Não consegui confirmar sua conta Google.")
                return
            }

            val credencial = GoogleAuthProvider.getCredential(idToken, null)
            FirebaseAuth.getInstance().signInWithCredential(credencial)
                .addOnSuccessListener { resultado ->
                    val usuario = resultado.user
                    if (usuario != null) onSucesso(usuario) else onErro("Login falhou, tente novamente.")
                }
                .addOnFailureListener { erro -> onErro("Erro ao conectar: ${erro.message}") }
        } catch (e: ApiException) {
            onErro("Login cancelado ou falhou (código ${e.statusCode}).")
        }
    }

    fun sair(onConcluido: () -> Unit) {
        FirebaseAuth.getInstance().signOut()
        googleClient.signOut().addOnCompleteListener { onConcluido() }
    }

    companion object {
        const val REQUEST_CODE_GOOGLE_SIGN_IN = 9001
    }
}

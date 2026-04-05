package com.mediakasir.apotekpos.ui.auth

import android.os.CancellationSignal
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialManagerCallback
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Returns Google ID token for the configured **Web** OAuth client ID, or null if the user cancels.
 */
suspend fun requestGoogleIdToken(
    activity: ComponentActivity,
    webClientId: String,
): Result<String?> = suspendCoroutine { cont ->
    val credentialManager = CredentialManager.create(activity)
    val option = GetSignInWithGoogleOption.Builder(webClientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(option)
        .build()
    val executor = ContextCompat.getMainExecutor(activity)
    credentialManager.getCredentialAsync(
        activity,
        request,
        CancellationSignal(),
        executor,
        object : CredentialManagerCallback<GetCredentialResponse, GetCredentialException> {
            override fun onResult(result: GetCredentialResponse) {
                val cred = result.credential
                if (cred is CustomCredential &&
                    cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val parsed = GoogleIdTokenCredential.createFrom(cred.data)
                    cont.resume(Result.success(parsed.idToken))
                } else {
                    cont.resume(Result.failure(IllegalStateException("Unexpected credential type")))
                }
            }

            override fun onError(e: GetCredentialException) {
                if (e is GetCredentialCancellationException || e is NoCredentialException) {
                    cont.resume(Result.success(null))
                } else {
                    cont.resume(Result.failure(e))
                }
            }
        },
    )
}

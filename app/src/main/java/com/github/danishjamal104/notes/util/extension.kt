package com.github.danishjamal104.notes.util

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.lang.RuntimeException
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

fun String.toSHA1(): String {
    try {
        val md = MessageDigest.getInstance("SHA-1")
        val messageDigest = md.digest(this.toByteArray())
        val no = BigInteger(1, messageDigest)
        var hashtext: String = no.toString(16)
        while (hashtext.length < 32) {
            hashtext = "0$hashtext"
        }
        return hashtext
    }catch (e: NoSuchAlgorithmException) {
        throw RuntimeException(e)
    }
}

fun String.encodeToBase64(): String {
    val bytes = this.toByteArray(StandardCharsets.UTF_8)
    return Base64.encodeToString(bytes, Base64.DEFAULT)
}

fun String.decodeFromBase64(): String {
    val data = Base64.decode(this, Base64.DEFAULT)
    return String(data, StandardCharsets.UTF_8)
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.disable() {
    this.isEnabled = false
}

fun View.enable() {
    this.isEnabled = true
}

fun Fragment.shortToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Fragment.longToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}

fun Context.showDefaultMaterialAlert(
    title: String,
    message: String,
    positiveButtonPress: () -> Unit
) {
    MaterialAlertDialogBuilder(this)
        .setTitle(title).setMessage(message)
        .setPositiveButton("Yes") { _, _ -> positiveButtonPress() }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }.create().show()
}

fun Context.hideKeyboard(view: View) {
    val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
}

fun Activity.hideKeyboard() {
    hideKeyboard(currentFocus ?: View(this))
}

fun Fragment.hideKeyboard() {
    view?.let { activity?.hideKeyboard(it) }
}

fun Fragment.performActionThroughSecuredChannel(error: (reason: String) -> Unit, success: () -> Unit, failed: () -> Unit) {
    val title = "Requires authentication"
    val subtitle = "You are trying to access/perform secured content/task which require authentication"
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    val executor = ContextCompat.getMainExecutor(requireContext())
    val biometricPrompt = BiometricPrompt(this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int,
                                               errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                error.invoke(errString.toString())
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                success.invoke()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                failed.invoke()
            }
        })
    biometricPrompt.authenticate(promptInfo)
}

fun Fragment.performActionThroughSecuredChannel(success: () -> Unit) {
    val title = "Requires authentication"
    val subtitle = "You are trying to access/perform secured content/task which require authentication"
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
        .build()
    val executor = ContextCompat.getMainExecutor(requireContext())
    val biometricPrompt = BiometricPrompt(this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int,
                                               errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                shortToast("Authentication error: $errString")
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                success.invoke()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                shortToast("Authentication failed")
            }
        })
    biometricPrompt.authenticate(promptInfo)
}
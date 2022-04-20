package com.github.danishjamal104.notes.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.github.danishjamal104.notes.data.backupandrestore.PassKeyProcessor
import com.github.danishjamal104.notes.util.encryption.EncryptionHelper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    } catch (e: NoSuchAlgorithmException) {
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

fun String.encrypt(secret: String): String {
    val keyProcessor = PassKeyProcessor.load(secret, true)
    val key = keyProcessor.getSecret()
    return EncryptionHelper.encrypt(this, key, EncryptionHelper.generateIv(keyProcessor.ivString))
}

fun String.decrypt(secret: String): String {
    val keyProcessor = PassKeyProcessor.load(secret, true)
    val key = keyProcessor.getSecret()
    return EncryptionHelper.decrypt(this, key, EncryptionHelper.generateIv(keyProcessor.ivString))
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

fun Context.shortToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Context.longToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Fragment.shortToast(message: String) {
    requireContext().shortToast(message)
}

fun Fragment.longToast(message: String) {
    requireContext().longToast(message)
}

fun Context.showDefaultMaterialAlert(
    title: String,
    message: String,
    positiveButtonPress: () -> Unit
) {
    MaterialAlertDialogBuilder(this)
        .setTitle(title).setMessage(message)
        .setPositiveButton("Yes") { _, _ -> positiveButtonPress.invoke() }
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

fun Context.copyToClipboard(text: String) {
    val clipboard: ClipboardManager? =
        this.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
    val clip = ClipData.newPlainText("COPIED", text)
    clipboard?.setPrimaryClip(clip)
}

fun View.setMargins(
    left: Int = this.marginLeft,
    top: Int = this.marginTop,
    right: Int = this.marginRight,
    bottom: Int = this.marginBottom,
) {
    layoutParams = (layoutParams as RelativeLayout.LayoutParams).apply {
        setMargins(left, top, right, bottom)
    }
}

@SuppressLint("NewApi")
fun Fragment.performActionThroughSecuredChannel(
    error: (reason: String) -> Unit,
    success: () -> Unit,
    failed: () -> Unit
) {
    val promptInfo = getVersionSpecificBiometricPromptInfo()
    val executor = ContextCompat.getMainExecutor(requireContext())
    val biometricPrompt = BiometricPrompt(this, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                error.invoke(errString.toString())
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
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

fun Context.performActionThroughSecuredChannel(success: () -> Unit) {
    val promptInfo = getVersionSpecificBiometricPromptInfo()
    val executor = ContextCompat.getMainExecutor(this)
    val biometricPrompt = BiometricPrompt(this as FragmentActivity, executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence
            ) {
                super.onAuthenticationError(errorCode, errString)
                shortToast("Authentication error: $errString")
            }

            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult
            ) {
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

fun Fragment.performActionThroughSecuredChannel(success: () -> Unit) {
    requireActivity().performActionThroughSecuredChannel(success)
}

private fun getVersionSpecificBiometricPromptInfo(): BiometricPrompt.PromptInfo {
    val title = "Requires authentication"
    val subtitle =
        "You are trying to access/perform secured content/task which require authentication"
    var promptBuilder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)
    if (Build.VERSION.SDK_INT in listOf(Build.VERSION_CODES.P, Build.VERSION_CODES.Q)) {
        promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Cancel")
    } else {
        promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL)
    }
    return promptBuilder.build()
}
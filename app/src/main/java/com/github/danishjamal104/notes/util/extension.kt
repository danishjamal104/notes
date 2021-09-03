package com.github.danishjamal104.notes.util

import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.lang.RuntimeException
import java.math.BigInteger
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

fun View.Gone() {
    this.visibility = View.GONE
}

fun View.Visible() {
    this.visibility = View.VISIBLE
}

fun View.Invisible() {
    this.visibility = View.INVISIBLE
}

fun View.Disable() {
    this.isEnabled = false
}

fun View.Enable() {
    this.isEnabled = true
}

fun Fragment.ShortToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun Fragment.LongToast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
}
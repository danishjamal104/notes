package com.github.danishjamal104.notes.util

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
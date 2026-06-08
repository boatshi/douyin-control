package com.yunfan.douyincontrol.util

import java.security.MessageDigest

object PasswordUtil {
    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verify(input: String, storedHash: String): Boolean {
        return hash(input) == storedHash
    }
}

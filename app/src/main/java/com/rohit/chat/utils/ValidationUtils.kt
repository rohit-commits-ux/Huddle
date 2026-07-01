package com.rohit.chat.utils

object ValidationUtils {
    fun isValidUsername(username: String): Boolean {
        val usernameRegex = "^[a-z0-9_]{4,20}$".toRegex()
        return username.matches(usernameRegex)
    }
}
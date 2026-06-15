package com.example.vetapp.util

import android.content.Context

class SessionManager(context: Context) {

    private val prefs = context.getSharedPreferences(
        "vetapp_session",
        Context.MODE_PRIVATE
    )

    fun saveSession(token: String, email: String, rol: String) {
        prefs.edit()
            .putString("token", token)
            .putString("email", email)
            .putString("rol", rol)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)
    fun getEmail(): String? = prefs.getString("email", null)
    fun getUserEmail(): String = prefs.getString("email", "") ?: ""
    fun getRol(): String? = prefs.getString("rol", null)
    fun isVeterinario(): Boolean = getRol() == "veterinario"

    fun isLoggedIn(): Boolean = getToken() != null

    fun logout() {
        prefs.edit().clear().apply()
    }
}
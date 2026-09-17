package com.example.radarcamera.security

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

// Mantiene el nombre de preferencias y algoritmo para conservar el PIN existente.
class PinStore(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "radar_coaching_access",
            Context.MODE_PRIVATE
        )

    fun tienePin(): Boolean {
        return preferences.contains("pin_hash") &&
                preferences.contains("pin_salt")
    }

    fun guardarPin(pin: String) {

        val salt = ByteArray(16)

        SecureRandom().nextBytes(salt)

        val hash = crearHash(
            pin = pin,
            salt = salt
        )

        val saved = preferences.edit()
            .putString(
                "pin_salt",
                Base64.encodeToString(
                    salt,
                    Base64.NO_WRAP
                )
            )
            .putString(
                "pin_hash",
                Base64.encodeToString(
                    hash,
                    Base64.NO_WRAP
                )
            )
            .commit()
        check(saved) { "No se pudo guardar el PIN." }
    }

    fun verificarPin(pin: String): Boolean {

        val saltTexto =
            preferences.getString(
                "pin_salt",
                null
            ) ?: return false

        val hashTexto =
            preferences.getString(
                "pin_hash",
                null
            ) ?: return false

        return try {

            val salt = Base64.decode(
                saltTexto,
                Base64.NO_WRAP
            )

            val hashGuardado = Base64.decode(
                hashTexto,
                Base64.NO_WRAP
            )

            val hashIngresado = crearHash(
                pin = pin,
                salt = salt
            )

            MessageDigest.isEqual(
                hashGuardado,
                hashIngresado
            )

        } catch (_: Exception) {
            false
        }
    }

    private fun crearHash(
        pin: String,
        salt: ByteArray
    ): ByteArray {

        val digest =
            MessageDigest.getInstance("SHA-256")

        digest.update(salt)

        return digest.digest(
            pin.toByteArray(Charsets.UTF_8)
        )
    }
}

package com.example.radarcamera.security

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.util.Base64
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.MessageDigest
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class PinCompatibilityTest {
    private lateinit var context: Context
    private lateinit var preferences: SharedPreferences
    private lateinit var testContext: Context
    private val testName = "pin-test-${UUID.randomUUID()}"

    @Before fun prepare() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences(testName, Context.MODE_PRIVATE)
        testContext = object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                check(name == "radar_coaching_access")
                return preferences
            }
        }
    }

    @After fun cleanup() {
        // Solo preferencias creadas por esta prueba, nunca el PIN del usuario.
        context.deleteSharedPreferences(testName)
    }

    @Test fun acceptsExistingV07HashAndSaltWithoutOverwriting() {
        val salt = ByteArray(16) { it.toByte() }
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val hash = digest.digest("1234".toByteArray(Charsets.UTF_8))
        val encodedHash = Base64.encodeToString(hash, Base64.NO_WRAP)
        preferences.edit()
            .putString("pin_salt", Base64.encodeToString(salt, Base64.NO_WRAP))
            .putString("pin_hash", encodedHash).commit()

        val store = PinStore(testContext)
        assertTrue(store.tienePin())
        assertTrue(store.verificarPin("1234"))
        assertFalse(store.verificarPin("9999"))
        assertEquals(encodedHash, preferences.getString("pin_hash", null))
    }

    @Test fun savesPinForSubsequentStoreInstances() {
        PinStore(testContext).guardarPin("567890")
        val reopened = PinStore(testContext)
        assertTrue(reopened.tienePin())
        assertTrue(reopened.verificarPin("567890"))
        assertFalse(reopened.verificarPin("56789"))
    }
}

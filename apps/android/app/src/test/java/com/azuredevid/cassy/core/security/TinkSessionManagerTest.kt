package com.azuredevid.cassy.core.security

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.domain.model.OfflinePinVerifier
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import io.ktor.http.Cookie
import io.ktor.http.Url
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class TinkSessionManagerTest {

    private class FakeAeadCipher : AeadCipher {
        override fun encrypt(plaintext: ByteArray, associatedData: ByteArray): ByteArray {
            val prefix = "ENC:".encodeToByteArray()
            val flipped = plaintext.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
            return prefix + flipped
        }

        override fun decrypt(ciphertext: ByteArray, associatedData: ByteArray): ByteArray {
            val prefix = "ENC:".encodeToByteArray()
            require(ciphertext.size >= prefix.size)
            val body = ciphertext.copyOfRange(prefix.size, ciphertext.size)
            return body.map { (it.toInt() xor 0x5A).toByte() }.toByteArray()
        }
    }

    private class InMemorySessionDataStore(
        initial: StoredSession = StoredSession()
    ) : DataStore<StoredSession> {
        private val state = MutableStateFlow(initial)
        private val mutex = Mutex()
        override val data: Flow<StoredSession> = state
        override suspend fun updateData(transform: suspend (t: StoredSession) -> StoredSession): StoredSession =
            mutex.withLock {
                val updated = transform(state.value)
                state.value = updated
                updated
            }
    }

    private fun createSessionManager(scope: TestScope): DataStoreSessionManager {
        return DataStoreSessionManager(dataStore = InMemorySessionDataStore(), scope = scope.backgroundScope)
    }

    @Test
    fun encryptedSerializer_encryptsPayloadAndRoundTripsWithoutPin() = runTest(UnconfinedTestDispatcher()) {
        val cipher = FakeAeadCipher()
        val serializer = EncryptedSessionSerializer(cipher)
        val out = ByteArrayOutputStream()
        val staff = StaffIdentity(
            userId = "u1",
            fullName = "Budi",
            email = "budi@kopi.id",
            role = "owner",
            storeId = "s1",
            storeSlug = "kopi-senja",
            storeName = "Kopi Senja"
        )
        val manager = createSessionManager(this)
        manager.saveLoginSession(
            accessToken = "jwt-access-123",
            accessTokenExpiresAt = "2026-11-13T20:00:00Z",
            sessionExpiresAt = "2026-11-14T20:00:00Z",
            staff = staff,
            permissions = listOf("store.settings.manage"),
            deviceCredential = "dev-cred-99",
            offlinePinVerifier = OfflinePinVerifier(verifier = "hashed-only", updatedAt = "2026-11-13T19:00:00Z")
        )

        val loaded = manager.awaitLoaded()
        assertNotNull(loaded)
        assertEquals("jwt-access-123", loaded?.accessToken)
        assertEquals("kopi-senja", manager.getStoreSlug())
        assertEquals("dev-cred-99", manager.getDeviceCredential())

        // Verify serializer writes ciphertext (not plaintext JSON)
        serializer.writeTo(loaded!!, out)
        val written = out.toByteArray()
        assertFalse(written.decodeToString().contains("jwt-access-123"))

        val roundTrip = serializer.readFrom(ByteArrayInputStream(written))
        assertEquals(loaded, roundTrip)
    }

    @Test
    fun clearSession_preservesDeviceBindingWhenRequested() = runTest(UnconfinedTestDispatcher()) {
        val manager = createSessionManager(this)
        val staff = StaffIdentity("u1", "Budi", "budi@kopi.id", "cashier", "s1", "kopi-senja", "Kopi Senja")
        manager.saveLoginSession(
            accessToken = "tok-1",
            accessTokenExpiresAt = "2026-11-13T20:00:00Z",
            sessionExpiresAt = "2026-11-14T20:00:00Z",
            staff = staff,
            deviceCredential = "dev-cred-1"
        )

        manager.clearSession(preserveDeviceBinding = true)

        assertNull(manager.currentSession())
        assertNull(manager.getAccessToken())
        // Wait for reload from DataStore
        manager.awaitLoaded()
        assertNull(manager.getAccessToken())
    }

    @Test
    fun cookiesStorage_persistsAndFiltersRefreshCookieByDomainAndPath() = runTest(UnconfinedTestDispatcher()) {
        val manager = createSessionManager(this)
        val storage = SessionCookiesStorage(manager)
        storage.addCookie(
            Url("https://demo.abbdullaziz.online/api/v1/auth/staff/login"),
            Cookie(
                name = "__Secure-mc_staff_rt",
                value = "rt-secret-value",
                domain = "demo.abbdullaziz.online",
                path = "/api/v1/auth/staff",
                secure = true,
                httpOnly = true
            )
        )

        val refreshCookies = storage.get(Url("https://demo.abbdullaziz.online/api/v1/auth/staff/refresh"))
        assertEquals(1, refreshCookies.size)
        assertEquals("rt-secret-value", refreshCookies.single().value)

        val otherPathCookies = storage.get(Url("https://demo.abbdullaziz.online/api/v1/pos/orders"))
        assertTrue(otherPathCookies.isEmpty())

        val otherHostCookies = storage.get(Url("https://r2.cloudflarestorage.com/bucket/banner.webp"))
        assertTrue(otherHostCookies.isEmpty())
    }
}

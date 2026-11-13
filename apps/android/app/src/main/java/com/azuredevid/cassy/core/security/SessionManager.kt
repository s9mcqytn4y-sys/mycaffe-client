package com.azuredevid.cassy.core.security

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.dataStoreFile
import com.azuredevid.cassy.domain.model.OfflinePinVerifier
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredCookie
import com.azuredevid.cassy.domain.model.StoredSession
import io.ktor.client.plugins.cookies.CookiesStorage
import io.ktor.http.Cookie
import io.ktor.http.Url
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import java.security.GeneralSecurityException

interface SessionManager {
    val sessionFlow: StateFlow<StoredSession?>
    fun currentSession(): StoredSession?
    fun getAccessToken(): String?
    fun getStoreSlug(): String?
    fun getDeviceCredential(): String?
    suspend fun awaitLoaded(): StoredSession?
    suspend fun saveLoginSession(
        accessToken: String,
        accessTokenExpiresAt: String,
        sessionExpiresAt: String,
        staff: StaffIdentity,
        permissions: List<String> = emptyList(),
        pinChangeRequired: Boolean = false,
        deviceCredential: String? = null,
        offlinePinVerifier: OfflinePinVerifier? = null
    )
    suspend fun updateRefreshedTokens(
        accessToken: String,
        accessTokenExpiresAt: String,
        sessionExpiresAt: String,
        staff: StaffIdentity? = null,
        permissions: List<String>? = null
    )
    suspend fun saveCookies(cookies: List<StoredCookie>)
    suspend fun getCookies(): List<StoredCookie>
    suspend fun clearSession(preserveDeviceBinding: Boolean = true)
}

class EncryptedSessionSerializer(
    private val cipher: AeadCipher,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) : Serializer<StoredSession> {

    override val defaultValue: StoredSession = StoredSession()

    override suspend fun readFrom(input: InputStream): StoredSession {
        val rawBytes = input.readBytes()
        if (rawBytes.isEmpty()) return defaultValue
        return try {
            val decrypted = cipher.decrypt(rawBytes, AAD)
            json.decodeFromString(StoredSession.serializer(), decrypted.decodeToString())
        } catch (e: GeneralSecurityException) {
            throw CorruptionException("Gagal mendekripsi sesi Cassy.", e)
        } catch (e: SerializationException) {
            throw CorruptionException("Format sesi Cassy rusak.", e)
        }
    }

    override suspend fun writeTo(t: StoredSession, output: OutputStream) {
        val plaintext = json.encodeToString(StoredSession.serializer(), t).encodeToByteArray()
        val encrypted = cipher.encrypt(plaintext, AAD)
        output.write(encrypted)
    }

    companion object {
        private val AAD = "cassy_pos_session_v1".encodeToByteArray()
    }
}

class DataStoreSessionManager(
    private val dataStore: DataStore<StoredSession>,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : SessionManager {

    constructor(
        context: Context,
        cipher: AeadCipher,
        scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    ) : this(
        dataStore = DataStoreFactory.create(
            serializer = EncryptedSessionSerializer(cipher),
            scope = scope,
            produceFile = { context.applicationContext.dataStoreFile(SESSION_FILE_NAME) }
        ),
        scope = scope
    )

    private val _sessionFlow = MutableStateFlow<StoredSession?>(null)
    override val sessionFlow: StateFlow<StoredSession?> = _sessionFlow.asStateFlow()

    private val writeMutex = Mutex()

    init {
        scope.launch {
            dataStore.data.collect { stored ->
                _sessionFlow.value = stored.takeIf { it.isAuthenticated }
            }
        }
    }

    override fun currentSession(): StoredSession? = _sessionFlow.value

    override fun getAccessToken(): String? = _sessionFlow.value?.accessToken?.takeIf { it.isNotBlank() }

    override fun getStoreSlug(): String? = _sessionFlow.value?.staff?.storeSlug?.takeIf { it.isNotBlank() }

    override fun getDeviceCredential(): String? = _sessionFlow.value?.deviceCredential?.takeIf { it.isNotBlank() }

    override suspend fun awaitLoaded(): StoredSession? {
        val stored = dataStore.data.first()
        val active = stored.takeIf { it.isAuthenticated }
        _sessionFlow.value = active
        return active
    }

    override suspend fun saveLoginSession(
        accessToken: String,
        accessTokenExpiresAt: String,
        sessionExpiresAt: String,
        staff: StaffIdentity,
        permissions: List<String>,
        pinChangeRequired: Boolean,
        deviceCredential: String?,
        offlinePinVerifier: OfflinePinVerifier?
    ) {
        writeMutex.withLock {
            val updated = dataStore.updateData { current ->
                current.copy(
                    accessToken = accessToken,
                    accessTokenExpiresAt = accessTokenExpiresAt,
                    sessionExpiresAt = sessionExpiresAt,
                    staff = staff,
                    permissions = permissions,
                    pinChangeRequired = pinChangeRequired,
                    deviceCredential = deviceCredential ?: current.deviceCredential,
                    offlinePinVerifier = offlinePinVerifier ?: current.offlinePinVerifier
                )
            }
            _sessionFlow.value = updated.takeIf { it.isAuthenticated }
        }
    }

    override suspend fun updateRefreshedTokens(
        accessToken: String,
        accessTokenExpiresAt: String,
        sessionExpiresAt: String,
        staff: StaffIdentity?,
        permissions: List<String>?
    ) {
        writeMutex.withLock {
            val updated = dataStore.updateData { current ->
                current.copy(
                    accessToken = accessToken,
                    accessTokenExpiresAt = accessTokenExpiresAt,
                    sessionExpiresAt = sessionExpiresAt,
                    staff = staff ?: current.staff,
                    permissions = permissions ?: current.permissions
                )
            }
            _sessionFlow.value = updated.takeIf { it.isAuthenticated }
        }
    }

    override suspend fun saveCookies(cookies: List<StoredCookie>) {
        writeMutex.withLock {
            dataStore.updateData { current ->
                current.copy(cookies = cookies)
            }
        }
    }

    override suspend fun getCookies(): List<StoredCookie> = dataStore.data.first().cookies

    override suspend fun clearSession(preserveDeviceBinding: Boolean) {
        writeMutex.withLock {
            val updated = dataStore.updateData { current ->
                if (preserveDeviceBinding) {
                    StoredSession(
                        deviceCredential = current.deviceCredential,
                        offlinePinVerifier = current.offlinePinVerifier
                    )
                } else {
                    StoredSession()
                }
            }
            _sessionFlow.value = updated.takeIf { it.isAuthenticated }
        }
    }

    companion object {
        private const val SESSION_FILE_NAME = "cassy_session.pb.enc"
    }
}

/**
 * Ktor CookiesStorage backed by Tink-encrypted DataStore so HttpOnly refresh cookies
 * (__Secure-mc_staff_rt) persist safely across app restarts.
 */
class SessionCookiesStorage(
    private val sessionManager: SessionManager
) : CookiesStorage {

    private val mutex = Mutex()

    override suspend fun addCookie(requestUrl: Url, cookie: Cookie) {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val domain = cookie.domain ?: requestUrl.host
            val path = cookie.path ?: "/"
            val expiresAt = cookie.expires?.timestamp
            val stored = StoredCookie(
                name = cookie.name,
                value = cookie.value,
                domain = domain,
                path = path,
                expiresAtEpochMillis = expiresAt,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly
            )
            val existing = sessionManager.getCookies()
                .filterNot { it.name == stored.name && (it.domain ?: requestUrl.host) == domain }
                .filterNot { it.expiresAtEpochMillis != null && it.expiresAtEpochMillis <= now }
            val next = if (stored.value.isEmpty() || (expiresAt != null && expiresAt <= now)) {
                existing
            } else {
                existing + stored
            }
            sessionManager.saveCookies(next)
        }
    }

    override suspend fun get(requestUrl: Url): List<Cookie> = mutex.withLock {
        val now = System.currentTimeMillis()
        val requestPath = requestUrl.encodedPath.ifBlank { "/" }
        sessionManager.getCookies()
            .filter { cookie ->
                val notExpired = cookie.expiresAtEpochMillis == null || cookie.expiresAtEpochMillis > now
                val domainMatch = cookie.domain.isNullOrBlank() ||
                    requestUrl.host.equals(cookie.domain, ignoreCase = true) ||
                    requestUrl.host.endsWith(".${cookie.domain.trimStart('.')}", ignoreCase = true)
                val pathMatch = cookie.path.isNullOrBlank() || requestPath.startsWith(cookie.path)
                notExpired && domainMatch && pathMatch
            }
            .map { stored ->
                Cookie(
                    name = stored.name,
                    value = stored.value,
                    domain = stored.domain,
                    path = stored.path,
                    expires = stored.expiresAtEpochMillis?.let { GMTDate(it) },
                    secure = stored.secure,
                    httpOnly = stored.httpOnly
                )
            }
    }

    override fun close() = Unit
}

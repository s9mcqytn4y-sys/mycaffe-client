package com.azuredevid.cassy.data.repository

import androidx.datastore.core.DataStore
import com.azuredevid.cassy.core.security.DataStoreSessionManager
import com.azuredevid.cassy.data.model.ProductUpsertInput
import com.azuredevid.cassy.data.network.CassyHttpClientFactory
import com.azuredevid.cassy.domain.model.StaffIdentity
import com.azuredevid.cassy.domain.model.StoredSession
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KtorCatalogRepositoryTest {

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

    private class FakeServerSettings(private val url: String = "https://api.cassy.test") : ServerSettingsRepository {
        override fun getBaseUrl(): String = url
        override fun setBaseUrl(url: String) = Unit
    }

    @Test
    fun getStaffCatalog_parsesCatalogViewWithDefaultVariantPrice() = runTest {
        val engine = MockEngine { request ->
            assertEquals("/api/v1/pos/catalog", request.url.encodedPath)
            respond(
                content = """
                    {
                      "categories": [{"id": "cat-1", "name": "Kopi", "display_order": 1, "is_active": true}],
                      "products": [{
                        "id": "prod-1",
                        "category_id": "cat-1",
                        "name": "Kopi Susu",
                        "description": "Gula aren",
                        "type": "prepared",
                        "prep_station": "bar",
                        "available": true,
                        "variants": [{"id": "var-1", "name": "Reguler", "price": 25000, "is_default": true, "available": true}]
                      }]
                    }
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val settings = FakeServerSettings()
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        val client = CassyHttpClientFactory.create(sessionManager, settings, engine)
        val repo = KtorCatalogRepository(null, client, settings)

        val snapshot = repo.getStaffCatalog().getOrThrow()
        assertEquals(1, snapshot.categories.size)
        assertEquals("Kopi", snapshot.categories.single().name)
        assertEquals(1, snapshot.products.size)
        assertEquals(25000L, snapshot.products.single().basePriceIdr)
        assertEquals("Kopi", snapshot.products.single().categoryName)
    }

    @Test
    fun uploadProductImage_executesThreeStepsWithoutLeakingBearerToR2() = runTest {
        var r2AuthHeader: String? = "NOT_CHECKED"
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/api/v1/media/uploads" && request.method == HttpMethod.Post -> {
                    respond(
                        content = """{"id":"up-1","upload_url":"https://r2.example.com/presigned-put","headers":{"x-amz-meta-test":"1"}}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                request.url.host == "r2.example.com" && request.method == HttpMethod.Put -> {
                    r2AuthHeader = request.headers[HttpHeaders.Authorization]
                    respond(content = "", status = HttpStatusCode.OK)
                }
                request.url.encodedPath == "/api/v1/media/uploads/up-1/complete" && request.method == HttpMethod.Post -> {
                    respond(
                        content = """{"image_url":"https://cdn.example.com/prod-1/600.jpg"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(content = "{}", status = HttpStatusCode.NotFound)
            }
        }
        val settings = FakeServerSettings()
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        sessionManager.saveLoginSession(
            accessToken = "secret-jwt",
            accessTokenExpiresAt = "2026-11-14T00:00:00Z",
            sessionExpiresAt = "2026-11-15T00:00:00Z",
            staff = StaffIdentity("u1", "Owner", "owner@cassy.id", "owner", "s1", "kopi", "Kopi")
        )
        val client = CassyHttpClientFactory.create(sessionManager, settings, engine)
        val repo = KtorCatalogRepository(null, client, settings)

        val uploadedUrl = repo.uploadProductImage("prod-1", byteArrayOf(1, 2, 3)).getOrThrow()
        assertEquals("https://cdn.example.com/prod-1/600.jpg", uploadedUrl)
        assertNull(r2AuthHeader)
    }

    @Test
    fun setProductAvailability_andCreateProduct_callExpectedEndpoints() = runTest {
        var patchAvailabilityCalled = false
        var createProductCalled = false
        val engine = MockEngine { request ->
            when {
                request.url.encodedPath == "/api/v1/catalog/products/prod-1/availability" && request.method == HttpMethod.Patch -> {
                    patchAvailabilityCalled = true
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
                request.url.encodedPath == "/api/v1/catalog/products" && request.method == HttpMethod.Post -> {
                    createProductCalled = true
                    respond(
                        content = """{"id":"prod-2","name":"Matcha Latte","product_type":"prepared","prep_station":"bar","is_available":true,"base_price":32000}""",
                        status = HttpStatusCode.Created,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
                else -> respond(content = "{}", status = HttpStatusCode.NotFound)
            }
        }
        val settings = FakeServerSettings()
        val sessionManager = DataStoreSessionManager(InMemorySessionDataStore(), backgroundScope)
        val client = CassyHttpClientFactory.create(sessionManager, settings, engine)
        val repo = KtorCatalogRepository(null, client, settings)

        assertTrue(repo.setProductAvailability("prod-1", false).isSuccess)
        val created = repo.createProduct(
            ProductUpsertInput(
                categoryId = null,
                name = "Matcha Latte",
                description = "",
                productType = "prepared",
                prepStation = "bar",
                basePriceIdr = 32000L
            )
        ).getOrThrow()

        assertTrue(patchAvailabilityCalled)
        assertTrue(createProductCalled)
        assertEquals("prod-2", created.id)
        assertEquals(32000L, created.basePriceIdr)
    }
}

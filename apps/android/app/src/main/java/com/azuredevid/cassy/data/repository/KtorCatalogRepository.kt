package com.azuredevid.cassy.data.repository

import android.content.Context
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.domain.repository.CatalogRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

class KtorCatalogRepository(
    private val context: Context,
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CatalogRepository {

    override suspend fun getCatalog(storeSlug: String): Result<List<CatalogProduct>> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.get("$baseUrl/api/v1/public/stores/${storeSlug.trim()}/catalog")
            if (response.status.isSuccess()) {
                val items = response.body<List<CatalogProduct>>()
                if (items.isNotEmpty()) {
                    return@withContext Result.success(items)
                }
            } else {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memuat katalog menu dari server.")
                )
            }

            // If server returned an empty list in demo mode, read bundled seed cleanly
            Result.success(readBundledSeedCatalog())
        } catch (t: Throwable) {
            // Only fall back to bundled seed on connectivity failure if available, otherwise return typed error
            val seed = runCatching { readBundledSeedCatalog() }.getOrNull()
            if (!seed.isNullOrEmpty()) {
                Result.success(seed)
            } else {
                Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat katalog menu."))
            }
        }
    }

    private fun readBundledSeedCatalog(): List<CatalogProduct> {
        val jsonString = context.assets.open("seeds/catalog_seed.json").bufferedReader().use { it.readText() }
        val jsonArray = JSONArray(jsonString)
        val products = mutableListOf<CatalogProduct>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            products.add(
                CatalogProduct(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    categoryId = obj.getString("categoryId"),
                    categoryName = obj.getString("categoryName"),
                    description = obj.optString("description", ""),
                    basePriceIdr = obj.getLong("basePriceIdr"),
                    imageUrl = obj.getString("imageUrl"),
                    prepStation = obj.optString("prepStation", "bar"),
                    isAvailable = obj.optBoolean("isAvailable", true),
                    stockQuantity = obj.optInt("stockQuantity", 50)
                )
            )
        }
        return products
    }
}

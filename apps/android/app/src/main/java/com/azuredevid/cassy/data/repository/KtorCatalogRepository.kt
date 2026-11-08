package com.azuredevid.cassy.data.repository

import android.content.Context
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.domain.repository.CatalogRepository
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Robust, SOLID-compliant Catalog Repository.
 * Fetches dynamic menu catalog from Go Backend API, with offline graceful fallback to assets/seeds/catalog_seed.json.
 */
class KtorCatalogRepository(
    private val context: Context,
    private val httpClient: HttpClient? = null,
    private val apiBaseUrl: String = "https://api.abbdullaziz.online"
) : CatalogRepository {

    override suspend fun getCatalog(storeSlug: String): Result<List<CatalogProduct>> = withContext(Dispatchers.IO) {
        runCatching {
            // 1. Coba request dinamis ke backend jika client aktif
            if (httpClient != null) {
                try {
                    val response = httpClient.get("$apiBaseUrl/api/v1/public/menu/$storeSlug")
                    val items = response.body<List<CatalogProduct>>()
                    if (items.isNotEmpty()) return@runCatching items
                } catch (_: Exception) {
                    // Fall through to offline cache
                }
            }

            // 2. Fallback offline: Baca dari assets/seeds/catalog_seed.json
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
            products
        }
    }
}


package com.azuredevid.cassy.data.repository

import android.content.Context
import com.azuredevid.cassy.data.model.CatalogCategory
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.CatalogSnapshot
import com.azuredevid.cassy.data.model.CatalogVariant
import com.azuredevid.cassy.data.model.ProductUpsertInput
import com.azuredevid.cassy.data.model.VariantUpsertInput
import com.azuredevid.cassy.data.network.HttpErrorMapper
import com.azuredevid.cassy.domain.repository.CatalogRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.json.JSONArray

class KtorCatalogRepository(
    private val context: Context? = null,
    private val httpClient: HttpClient,
    private val serverSettings: ServerSettingsRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : CatalogRepository {

    override suspend fun getCatalog(storeSlug: String): Result<List<CatalogProduct>> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.get("$baseUrl/api/v1/public/stores/${storeSlug.trim()}/catalog")
            if (response.status.isSuccess()) {
                val view = response.body<CatalogViewDto>()
                val mapped = view.toSnapshot().products
                if (mapped.isNotEmpty()) {
                    return@withContext Result.success(mapped)
                }
                return@withContext Result.success(readBundledSeedCatalog())
            }
            Result.failure(HttpErrorMapper.toDomainException(response, "Gagal memuat katalog menu dari server."))
        } catch (t: Throwable) {
            val seed = runCatching { readBundledSeedCatalog() }.getOrNull()
            if (!seed.isNullOrEmpty()) {
                Result.success(seed)
            } else {
                Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat katalog menu."))
            }
        }
    }

    override suspend fun getStaffCatalog(): Result<CatalogSnapshot> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.get("$baseUrl/api/v1/pos/catalog")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memuat katalog staf.")
                )
            }
            Result.success(response.body<CatalogViewDto>().toSnapshot())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memuat katalog staf."))
        }
    }

    override suspend fun createCategory(
        name: String,
        displayOrder: Int,
        isActive: Boolean
    ): Result<CatalogCategory> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/catalog/categories") {
                contentType(ContentType.Application.Json)
                setBody(CategoryUpsertDto(name.trim(), displayOrder, isActive))
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal menambah kategori.")
                )
            }
            Result.success(response.body<CategoryDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menambah kategori."))
        }
    }

    override suspend fun updateCategory(
        id: String,
        name: String,
        displayOrder: Int,
        isActive: Boolean
    ): Result<CatalogCategory> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.patch("$baseUrl/api/v1/catalog/categories/${id.trim()}") {
                contentType(ContentType.Application.Json)
                setBody(CategoryUpsertDto(name.trim(), displayOrder, isActive))
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memperbarui kategori.")
                )
            }
            Result.success(response.body<CategoryDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memperbarui kategori."))
        }
    }

    override suspend fun deleteCategory(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.delete("$baseUrl/api/v1/catalog/categories/${id.trim()}")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal menghapus kategori.")
                )
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menghapus kategori."))
        }
    }

    override suspend fun createProduct(input: ProductUpsertInput): Result<CatalogProduct> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val price = input.basePriceIdr ?: 0L
            val body = ProductCreateDto(
                categoryId = input.categoryId?.takeIf { it.isNotBlank() },
                name = input.name.trim(),
                description = input.description.trim(),
                productType = input.productType,
                prepStation = input.prepStation,
                isAvailable = input.isAvailable,
                variants = listOf(
                    VariantUpsertDto(
                        name = input.defaultVariantName.ifBlank { "Reguler" },
                        price = price,
                        isDefault = true,
                        isAvailable = true
                    )
                )
            )
            val response = httpClient.post("$baseUrl/api/v1/catalog/products") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal membuat produk.")
                )
            }
            Result.success(response.body<ProductDto>().toDomain(categoryNames = emptyMap()))
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal membuat produk."))
        }
    }

    override suspend fun updateProduct(id: String, input: ProductUpsertInput): Result<CatalogProduct> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val body = ProductUpdateDto(
                categoryId = input.categoryId?.takeIf { it.isNotBlank() },
                clearCategory = input.clearCategory,
                name = input.name.trim(),
                description = input.description.trim(),
                productType = input.productType,
                prepStation = input.prepStation,
                isAvailable = input.isAvailable
            )
            val response = httpClient.patch("$baseUrl/api/v1/catalog/products/${id.trim()}") {
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memperbarui produk.")
                )
            }
            Result.success(response.body<ProductDto>().toDomain(categoryNames = emptyMap()))
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memperbarui produk."))
        }
    }

    override suspend fun archiveProduct(id: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.delete("$baseUrl/api/v1/catalog/products/${id.trim()}")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal mengarsipkan produk.")
                )
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal mengarsipkan produk."))
        }
    }

    override suspend fun setProductAvailability(id: String, isAvailable: Boolean): Result<Unit> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.patch("$baseUrl/api/v1/catalog/products/${id.trim()}/availability") {
                contentType(ContentType.Application.Json)
                setBody(AvailabilityRequestDto(isAvailable))
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal mengubah ketersediaan produk.")
                )
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal mengubah ketersediaan produk."))
        }
    }

    override suspend fun createVariant(productId: String, input: VariantUpsertInput): Result<CatalogVariant> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.post("$baseUrl/api/v1/catalog/products/${productId.trim()}/variants") {
                contentType(ContentType.Application.Json)
                setBody(
                    VariantUpsertDto(
                        name = input.name.trim(),
                        sku = input.sku.trim().ifBlank { null },
                        price = input.price,
                        isDefault = input.isDefault,
                        isAvailable = input.isAvailable
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal menambah varian.")
                )
            }
            Result.success(response.body<VariantDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal menambah varian."))
        }
    }

    override suspend fun updateVariant(variantId: String, input: VariantUpsertInput): Result<CatalogVariant> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.patch("$baseUrl/api/v1/catalog/variants/${variantId.trim()}") {
                contentType(ContentType.Application.Json)
                setBody(
                    VariantUpsertDto(
                        name = input.name.trim(),
                        sku = input.sku.trim().ifBlank { null },
                        price = input.price,
                        isDefault = input.isDefault,
                        isAvailable = input.isAvailable
                    )
                )
            }
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal memperbarui varian.")
                )
            }
            Result.success(response.body<VariantDto>().toDomain())
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal memperbarui varian."))
        }
    }

    override suspend fun archiveVariant(variantId: String): Result<Unit> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val response = httpClient.delete("$baseUrl/api/v1/catalog/variants/${variantId.trim()}")
            if (!response.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(response, "Gagal mengarsipkan varian.")
                )
            }
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal mengarsipkan varian."))
        }
    }

    override suspend fun uploadProductImage(productId: String, webpBytes: ByteArray): Result<String> = withContext(ioDispatcher) {
        try {
            val baseUrl = serverSettings.getBaseUrl()
            val ticketResponse = httpClient.post("$baseUrl/api/v1/media/uploads") {
                contentType(ContentType.Application.Json)
                setBody(
                    CreateMediaUploadDto(
                        kind = "product_image",
                        productId = productId.trim(),
                        contentType = "image/webp",
                        size = webpBytes.size.toLong()
                    )
                )
            }
            if (!ticketResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(ticketResponse, "Gagal meminta URL unggah foto.")
                )
            }
            val ticket = ticketResponse.body<MediaUploadTicketDto>()

            val putResponse = httpClient.put(ticket.uploadUrl) {
                ticket.headers.forEach { (key, value) ->
                    if (!key.equals(HttpHeaders.ContentType, ignoreCase = true)) {
                        header(key, value)
                    }
                }
                contentType(ContentType("image", "webp"))
                setBody(webpBytes)
            }
            if (!putResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(putResponse, "Gagal mengunggah file gambar ke penyimpanan.")
                )
            }

            val completeResponse = httpClient.post("$baseUrl/api/v1/media/uploads/${ticket.id}/complete")
            if (!completeResponse.status.isSuccess()) {
                return@withContext Result.failure(
                    HttpErrorMapper.toDomainException(completeResponse, "Gagal memverifikasi foto produk.")
                )
            }
            val completed = completeResponse.body<CompletedMediaUploadDto>()
            Result.success(completed.imageUrl)
        } catch (t: Throwable) {
            Result.failure(HttpErrorMapper.fromThrowable(t, "Gagal mengunggah foto produk."))
        }
    }

    private fun readBundledSeedCatalog(): List<CatalogProduct> {
        val ctx = context ?: return emptyList()
        val jsonString = ctx.assets.open("seeds/catalog_seed.json").bufferedReader().use { it.readText() }
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

    @Serializable
    private data class CatalogViewDto(
        val categories: List<CategoryDto> = emptyList(),
        val products: List<ProductDto> = emptyList()
    ) {
        fun toSnapshot(): CatalogSnapshot {
            val cats = categories.map { it.toDomain() }
            val catNames = cats.associate { it.id to it.name }
            return CatalogSnapshot(
                categories = cats,
                products = products.map { it.toDomain(catNames) }
            )
        }
    }

    @Serializable
    private data class CategoryDto(
        val id: String,
        val name: String,
        @SerialName("display_order") val displayOrder: Int = 0,
        @SerialName("is_active") val isActive: Boolean = true
    ) {
        fun toDomain() = CatalogCategory(id = id, name = name, displayOrder = displayOrder, isActive = isActive)
    }

    @Serializable
    private data class CategoryUpsertDto(
        val name: String,
        @SerialName("display_order") val displayOrder: Int,
        @SerialName("is_active") val isActive: Boolean
    )

    @Serializable
    private data class VariantDto(
        val id: String,
        val name: String,
        val sku: String? = null,
        val price: Long,
        @SerialName("is_default") val isDefault: Boolean = false,
        val available: Boolean? = null,
        @SerialName("is_available") val isAvailable: Boolean? = null
    ) {
        fun toDomain() = CatalogVariant(
            id = id,
            name = name,
            sku = sku.orEmpty(),
            price = price,
            isDefault = isDefault,
            available = available ?: isAvailable ?: true
        )
    }

    @Serializable
    private data class VariantUpsertDto(
        val name: String,
        val sku: String? = null,
        val price: Long,
        @SerialName("is_default") val isDefault: Boolean,
        @SerialName("is_available") val isAvailable: Boolean
    )

    @Serializable
    private data class ProductDto(
        val id: String,
        @SerialName("category_id") val categoryId: String? = null,
        val name: String,
        val description: String = "",
        val type: String = "",
        @SerialName("product_type") val productType: String = "",
        @SerialName("prep_station") val prepStation: String = "bar",
        @SerialName("image_url") val imageUrl: String? = null,
        val available: Boolean? = null,
        @SerialName("is_available") val isAvailable: Boolean? = null,
        @SerialName("stock_quantity") val stockQuantity: Int? = null,
        @SerialName("base_price") val basePrice: Long? = null,
        val variants: List<VariantDto> = emptyList()
    ) {
        fun toDomain(categoryNames: Map<String, String>): CatalogProduct {
            val domainVariants = variants.map { it.toDomain() }
            val resolvedPrice = basePrice
                ?: domainVariants.firstOrNull { it.isDefault }?.price
                ?: domainVariants.firstOrNull()?.price
                ?: 0L
            return CatalogProduct(
                id = id,
                name = name,
                categoryId = categoryId.orEmpty(),
                categoryName = categoryId?.let { categoryNames[it] }.orEmpty(),
                description = description,
                basePriceIdr = resolvedPrice,
                imageUrl = imageUrl.orEmpty(),
                prepStation = prepStation.ifBlank { "bar" },
                isAvailable = available ?: isAvailable ?: true,
                stockQuantity = stockQuantity ?: 0,
                productType = type.ifBlank { productType }.ifBlank { "prepared" },
                variants = domainVariants
            )
        }
    }

    @Serializable
    private data class ProductCreateDto(
        @SerialName("category_id") val categoryId: String? = null,
        val name: String,
        val description: String,
        @SerialName("product_type") val productType: String,
        @SerialName("prep_station") val prepStation: String,
        @SerialName("is_available") val isAvailable: Boolean,
        val variants: List<VariantUpsertDto>
    )

    @Serializable
    private data class ProductUpdateDto(
        @SerialName("category_id") val categoryId: String? = null,
        @SerialName("clear_category") val clearCategory: Boolean = false,
        val name: String,
        val description: String,
        @SerialName("product_type") val productType: String,
        @SerialName("prep_station") val prepStation: String,
        @SerialName("is_available") val isAvailable: Boolean
    )

    @Serializable
    private data class AvailabilityRequestDto(
        @SerialName("is_available") val isAvailable: Boolean
    )

    @Serializable
    private data class CreateMediaUploadDto(
        val kind: String,
        @SerialName("product_id") val productId: String,
        @SerialName("content_type") val contentType: String,
        val size: Long
    )

    @Serializable
    private data class MediaUploadTicketDto(
        val id: String,
        @SerialName("upload_url") val uploadUrl: String,
        val headers: Map<String, String> = emptyMap()
    )

    @Serializable
    private data class CompletedMediaUploadDto(
        @SerialName("image_url") val imageUrl: String
    )
}

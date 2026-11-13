package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.data.model.CatalogCategory
import com.azuredevid.cassy.data.model.CatalogProduct
import com.azuredevid.cassy.data.model.CatalogSnapshot
import com.azuredevid.cassy.data.model.CatalogVariant
import com.azuredevid.cassy.data.model.ProductUpsertInput
import com.azuredevid.cassy.data.model.VariantUpsertInput

/**
 * CatalogRepository abstraction adhering to DIP (Dependency Inversion Principle).
 */
interface CatalogRepository {
    suspend fun getCatalog(storeSlug: String = ""): Result<List<CatalogProduct>>
    suspend fun getStaffCatalog(): Result<CatalogSnapshot>
    suspend fun createCategory(name: String, displayOrder: Int = 0, isActive: Boolean = true): Result<CatalogCategory>
    suspend fun updateCategory(id: String, name: String, displayOrder: Int, isActive: Boolean): Result<CatalogCategory>
    suspend fun deleteCategory(id: String): Result<Unit>
    suspend fun createProduct(input: ProductUpsertInput): Result<CatalogProduct>
    suspend fun updateProduct(id: String, input: ProductUpsertInput): Result<CatalogProduct>
    suspend fun archiveProduct(id: String): Result<Unit>
    suspend fun setProductAvailability(id: String, isAvailable: Boolean): Result<Unit>
    suspend fun createVariant(productId: String, input: VariantUpsertInput): Result<CatalogVariant>
    suspend fun updateVariant(variantId: String, input: VariantUpsertInput): Result<CatalogVariant>
    suspend fun archiveVariant(variantId: String): Result<Unit>
    suspend fun uploadProductImage(productId: String, webpBytes: ByteArray): Result<String>
}

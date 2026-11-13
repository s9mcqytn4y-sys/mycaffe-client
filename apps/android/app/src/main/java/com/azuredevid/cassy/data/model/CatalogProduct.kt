package com.azuredevid.cassy.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Pure Domain Entity for Menu Catalog Items (SOLID / Clean Architecture).
 */
@Serializable
data class CatalogProduct(
    val id: String,
    val name: String,
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val basePriceIdr: Long,
    val imageUrl: String,
    val prepStation: String,
    val isAvailable: Boolean = true,
    val stockQuantity: Int = 0,
    val productType: String = "prepared",
    val variants: List<CatalogVariant> = emptyList()
)

@Serializable
data class CatalogCategory(
    val id: String,
    val name: String,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

@Serializable
data class CatalogVariant(
    val id: String,
    val name: String,
    val sku: String = "",
    val price: Long,
    @SerialName("is_default") val isDefault: Boolean = false,
    val available: Boolean = true
)

data class CatalogSnapshot(
    val categories: List<CatalogCategory>,
    val products: List<CatalogProduct>
)

data class ProductUpsertInput(
    val categoryId: String?,
    val clearCategory: Boolean = false,
    val name: String,
    val description: String,
    val productType: String,
    val prepStation: String,
    val isAvailable: Boolean = true,
    val basePriceIdr: Long? = null,
    val defaultVariantName: String = "Reguler"
)

data class VariantUpsertInput(
    val name: String,
    val sku: String = "",
    val price: Long,
    val isDefault: Boolean = false,
    val isAvailable: Boolean = true
)

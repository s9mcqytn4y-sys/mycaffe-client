package com.azuredevid.cassy.data.model

import kotlinx.serialization.Serializable

/**
 * Pure Domain Entity for Menu Catalog Items (SOLID / Clean Architecture).
 * No hardcoded fixtures in domain layer; data is fetched via CatalogRepository.
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
    val stockQuantity: Int = 0
)

@Serializable
data class CatalogCategory(
    val id: String,
    val name: String,
    val displayOrder: Int = 0
)



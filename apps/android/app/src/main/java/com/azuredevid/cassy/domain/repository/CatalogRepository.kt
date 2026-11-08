package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.data.model.CatalogProduct

/**
 * CatalogRepository abstraction adhering to DIP (Dependency Inversion Principle).
 * Exposes menu retrieval without tying UI callers to the network or local database layer.
 */
interface CatalogRepository {
    suspend fun getCatalog(storeSlug: String = "central-cafe"): Result<List<CatalogProduct>>
}


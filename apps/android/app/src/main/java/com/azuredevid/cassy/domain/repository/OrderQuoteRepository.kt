package com.azuredevid.cassy.domain.repository

import com.azuredevid.cassy.data.model.OrderQuote
import com.azuredevid.cassy.data.model.OrderQuoteRequest

/** Prices a cart on the server. The tablet never computes tax or totals itself (CLAUDE.md). */
interface OrderQuoteRepository {
    suspend fun quote(request: OrderQuoteRequest): Result<OrderQuote>
}

/** No signed-in staff session, so there is nothing to price against: the POS stays fail-closed. */
class NoStaffSessionException : IllegalStateException("No staff session; totals come from the server after sign-in")

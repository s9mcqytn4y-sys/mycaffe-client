package com.azuredevid.cassy.ui.navigation

import kotlinx.serialization.Serializable

sealed interface CassyRoute {
    @Serializable
    data object StorePairing : CassyRoute

    @Serializable
    data object ServerConfig : CassyRoute

    @Serializable
    data object PosCashier : CassyRoute

    @Serializable
    data object StoreSettings : CassyRoute
}

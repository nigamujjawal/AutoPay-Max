package com.uj.appstorysautopaymanager.data.remote.dto

// email/merchant_name dropped - the app no longer edits those. upi_id stays optional/nullable so
// Gson omits it when unset, matching the doc's "update only the fields it needs" contract.
data class UpdateUserRequest(
    val upi_id: String? = null
)

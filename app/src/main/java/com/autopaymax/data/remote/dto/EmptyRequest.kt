package com.autopaymax.data.remote.dto

// POST /subscriptions needs no request body per the docs, but OkHttp requires a non-null body
// for POST. Gson serializes a no-field class to "{}", which the server can safely ignore.
class EmptyRequest

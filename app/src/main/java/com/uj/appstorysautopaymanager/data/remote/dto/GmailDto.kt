package com.uj.appstorysautopaymanager.data.remote.dto

// Minimal Gson mapping of only the Gmail REST fields GmailSyncWorker consumes - not the full
// message resource. Unmapped keys are ignored by Gson.
data class GmailMessageListResponse(
    val messages: List<GmailMessageRef>?,
    val nextPageToken: String?
)

data class GmailMessageRef(
    val id: String
)

data class GmailMessageResponse(
    val id: String,
    val internalDate: String?,     // epoch millis, as a string
    val payload: GmailMessagePayload?
)

data class GmailMessagePayload(
    val mimeType: String?,
    val headers: List<GmailHeader>?,
    val body: GmailMessageBody?,
    val parts: List<GmailMessagePart>?
)

data class GmailHeader(
    val name: String,
    val value: String
)

data class GmailMessageBody(
    val data: String?              // base64url
)

data class GmailMessagePart(
    val mimeType: String?,
    val body: GmailMessageBody?,
    val parts: List<GmailMessagePart>?
)

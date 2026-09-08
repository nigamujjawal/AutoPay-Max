package com.uj.appstorysautopaymanager.data.remote

import com.uj.appstorysautopaymanager.data.remote.dto.GmailMessageListResponse
import com.uj.appstorysautopaymanager.data.remote.dto.GmailMessageResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

// Just the two Gmail REST calls the sync worker needs - a full Gmail client library would be
// heavyweight for this. Base URL: https://gmail.googleapis.com/gmail/v1/
interface GmailApi {

    @GET("users/me/messages")
    suspend fun listMessages(
        @Query("q") q: String,
        @Query("pageToken") pageToken: String? = null,
        @Query("maxResults") maxResults: Int = 25
    ): GmailMessageListResponse

    @GET("users/me/messages/{id}")
    suspend fun getMessage(
        @Path("id") id: String,
        @Query("format") format: String = "full"
    ): GmailMessageResponse
}

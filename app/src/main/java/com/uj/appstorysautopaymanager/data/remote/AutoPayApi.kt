package com.uj.appstorysautopaymanager.data.remote

import com.uj.appstorysautopaymanager.data.remote.dto.CaptureSubscriptionRequest
import com.uj.appstorysautopaymanager.data.remote.dto.CreatePaymentRequest
import com.uj.appstorysautopaymanager.data.remote.dto.EmptyRequest
import com.uj.appstorysautopaymanager.data.remote.dto.LoginRequest
import com.uj.appstorysautopaymanager.data.remote.dto.LoginResponse
import com.uj.appstorysautopaymanager.data.remote.dto.PaymentDto
import com.uj.appstorysautopaymanager.data.remote.dto.SubscriptionDto
import com.uj.appstorysautopaymanager.data.remote.dto.UpdateUserRequest
import com.uj.appstorysautopaymanager.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

// Mirrors the routes in section 8 of the SoundBox backend API doc exactly. /webhooks/razorpay is
// intentionally not here - it's a Razorpay server-to-server callback, never called by this app.
interface AutoPayApi {

    @POST("auth/firebase")
    suspend fun firebaseLogin(@Body request: LoginRequest): LoginResponse

    @POST("payments")
    suspend fun savePayment(@Body request: CreatePaymentRequest): PaymentDto

    @GET("payments")
    suspend fun getPayments(): List<PaymentDto>

    // Server returns 201 with no response body - Void is Retrofit's built-in "don't touch the
    // body" type (unlike Kotlin's Unit, it bypasses the Gson converter entirely), so this stays
    // safe even though the status code isn't the usual 204/205 empty-body convention.
    @PUT("users")
    suspend fun updateUser(@Body request: UpdateUserRequest): Void?

    @GET("users")
    suspend fun getUser(): UserDto

    @POST("subscriptions")
    suspend fun createSubscription(@Body body: EmptyRequest): SubscriptionDto

    // subscription_id here is the `id` field from POST /subscriptions' response, not the
    // provider's external_subscription_id. Response shape isn't documented for this endpoint -
    // confirmed assumption (not a guess anymore): same SubscriptionDto shape as create.
    @POST("subscriptions/capture")
    suspend fun captureSubscription(@Body request: CaptureSubscriptionRequest): SubscriptionDto
}

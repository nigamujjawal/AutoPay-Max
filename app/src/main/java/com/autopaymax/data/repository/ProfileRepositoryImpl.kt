package com.autopaymax.data.repository

import com.autopaymax.common.Resource
import com.autopaymax.common.safeApiCall
import com.autopaymax.data.local.dao.AuthTokenDao
import com.autopaymax.data.remote.AutoPayApi
import com.autopaymax.data.remote.dto.UpdateUserRequest
import com.autopaymax.data.remote.dto.UserDto
import com.autopaymax.domain.profile.model.UserProfile
import com.autopaymax.domain.profile.repository.ProfileRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    private val api: AutoPayApi,
    private val tokenDao: AuthTokenDao
) : ProfileRepository {

    override suspend fun getUserProfile(): Resource<UserProfile> {
        val token = tokenDao.getToken()
        if (token == null || token.accessToken.isBlank()) {
            // No backend session (Google users the SoundBox backend can't onboard - see
            // AuthRepositoryImpl). Serve a local-only profile so Settings / Edit Profile still
            // work without a network round-trip or an error toast.
            return Resource.Success(
                UserProfile(
                    id = "", firebaseUid = token?.uid.orEmpty(), phoneNumber = "",
                    name = token?.name.orEmpty(), upiId = "", provider = "google",
                    subscriptionId = null, subscriptionStatus = "", subscriptionEndsAt = null,
                    lastLogin = null, createdAt = "", updatedAt = ""
                )
            )
        }
        return safeApiCall { api.getUser().toDomain(name = token.name) }
    }

    // name is purely local (Room), upiId is the only field still sent to the backend. Each is
    // independent - saving just a name doesn't touch the network at all.
    override suspend fun updateUserProfile(
        name: String?,
        upiId: String?
    ): Resource<Unit> {
        if (name != null) {
            tokenDao.getToken()?.let { tokenDao.updateName(it.uid, name) }
        }
        if (upiId == null) return Resource.Success(Unit)
        // upiId is backend-only - skip it silently without a backend session (the local name,
        // if any, was already saved above).
        if (tokenDao.getToken()?.accessToken.isNullOrBlank()) return Resource.Success(Unit)
        return safeApiCall {
            api.updateUser(UpdateUserRequest(upi_id = upiId))
            Unit
        }
    }

    private fun UserDto.toDomain(name: String) = UserProfile(
        id = id,
        firebaseUid = firebase_uid,
        phoneNumber = phone_number,
        name = name,
        upiId = upi_id,
        provider = provider,
        subscriptionId = subscription_id,
        subscriptionStatus = subscription_status,
        subscriptionEndsAt = subscription_endsAt,
        lastLogin = last_login,
        createdAt = created_at,
        updatedAt = updated_at
    )
}

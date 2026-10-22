package com.autopaymax.domain.profile.repository

import com.autopaymax.common.Resource
import com.autopaymax.domain.profile.model.UserProfile

interface ProfileRepository {
    suspend fun getUserProfile(): Resource<UserProfile>

    // name is written straight to Room (local-only, never sent to the backend). upiId goes to
    // PUT /users, which returns 201 with no body on success - there's no updated profile to hand
    // back here. Callers that need the fresh profile should follow up with getUserProfile().
    suspend fun updateUserProfile(
        name: String?,
        upiId: String?
    ): Resource<Unit>
}

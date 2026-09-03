package com.uj.appstorysautopaymanager.domain.auth.model

// No Firebase types here on purpose. AutoVerified carries no credential - when Firebase's SMS
// auto-retrieval fires, the data layer completes sign-in itself before surfacing this state, so
// a PhoneAuthCredential never has to cross into domain.
sealed interface OtpRequestState {
    data class CodeSent(val verificationId: String) : OtpRequestState
    data object AutoVerified : OtpRequestState
    data class Failed(val message: String) : OtpRequestState
}

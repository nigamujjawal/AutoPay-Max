package com.uj.appstorysautopaymanager.data.remote

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// Fired by AuthInterceptor the instant any backend call comes back 401 - every request shares
// that one interceptor, so this is a single choke point regardless of which endpoint tripped it.
// Room's own "is a token stored" check (AuthRepositoryImpl.getStoredUser) can only ever answer
// "did we save one", never "does the backend still honor it" - this is the one signal that
// actually knows the session died server-side, not just locally.
@Singleton
class SessionExpiredNotifier @Inject constructor() {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    fun notifyExpired() {
        _events.tryEmit(Unit)
    }
}

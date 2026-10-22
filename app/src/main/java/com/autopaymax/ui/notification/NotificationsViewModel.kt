package com.autopaymax.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopaymax.data.local.entity.NotificationEntity
import com.autopaymax.data.repository.AutoPayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repository: AutoPayRepository
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = repository.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAllRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    fun dismiss(notificationId: Long) {
        viewModelScope.launch { repository.deleteNotificationById(notificationId) }
    }
}

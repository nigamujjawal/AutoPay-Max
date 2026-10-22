package com.autopaymax.ui.autopay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.autopaymax.data.local.entity.Category
import com.autopaymax.data.local.entity.Mandate
import com.autopaymax.data.repository.AutoPayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import com.autopaymax.worker.GmailSyncWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MandateViewModel @Inject constructor(
    private val repository: AutoPayRepository,
    private val preferenceManager: com.autopaymax.data.local.pref.PreferenceManager
) : ViewModel() {

    // True once the Room query has actually emitted at least once. Dashboard uses this to tell
    // "we haven't loaded yet" apart from "we loaded and there truly are zero mandates" - without
    // it, the empty-state screen flashes for a frame on every cold start before real data
    // arrives, because `mandates` has to start somewhere and emptyList() is that starting value.
    private val _hasLoadedMandates = MutableStateFlow(false)
    val hasLoadedMandates: StateFlow<Boolean> = _hasLoadedMandates.asStateFlow()

    // Includes CANCELLED mandates (sorted below active) so the Home list can still show and
    // open them - callers that only want live autopays filter on status == "ACTIVE".
    // Eagerly, not WhileSubscribed: this ViewModel is created once at Activity start, well
    // before Dashboard is ever navigated to (Splash's own delay gives Room a head start), so
    // starting the query immediately instead of waiting for a subscriber all but eliminates the
    // empty-state flash rather than merely papering over it with the loaded flag above.
    val mandates: StateFlow<List<Mandate>> = repository.allMandatesForDisplay
        .onEach { _hasLoadedMandates.value = true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // One-time Home prompt to kick off the first Gmail scan. Shows until the user runs or
    // dismisses it; GmailSyncWorker still self-schedules regardless, this just makes the first
    // sync visible and intentional.
    val showMailSyncPrompt: StateFlow<Boolean> = preferenceManager.isMailSyncPromptSeenFlow
        .map { !it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun dismissMailSyncPrompt() = viewModelScope.launch {
        preferenceManager.setMailSyncPromptSeen(true)
    }

    fun syncMailsOnce(context: android.content.Context) {
        GmailSyncWorker.syncNow(context)
        viewModelScope.launch { preferenceManager.setMailSyncPromptSeen(true) }
    }

    fun addMandate(mandate: Mandate, context: android.content.Context) {
        viewModelScope.launch {
            repository.insertMandate(mandate)
            val ttsHelper = com.autopaymax.tts.TextToSpeechHelper(context, preferenceManager)
            val merchantName = mandate.merchant.ifEmpty { "Subscription" }
            ttsHelper.speak(com.autopaymax.tts.AnnouncementKind.AUTOPAY_SET, merchantName, mandate.amount.toInt())
        }
    }

    fun updateMandate(mandate: Mandate) {
        viewModelScope.launch {
            repository.updateMandate(mandate)
        }
    }

    fun toggleMandateStatus(mandate: Mandate) {
        viewModelScope.launch {
            val newStatus = if (mandate.status == "ACTIVE") "CANCELLED" else "ACTIVE"
            repository.updateMandate(mandate.copy(status = newStatus))
        }
    }

    // One-way: the cancellation sheet only ever marks a mandate cancelled (it stays visible on
    // Home with a "Cancelled" label). Actually stopping the charge happens at Google Play / the
    // vendor / the UPI app - see MandateDetailScreen.
    fun cancelMandate(mandate: Mandate) {
        viewModelScope.launch {
            repository.updateMandate(mandate.copy(status = "CANCELLED"))
        }
    }
}

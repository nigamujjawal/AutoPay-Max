package com.uj.appstorysautopaymanager.ui.autopay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.data.local.entity.Category
import com.uj.appstorysautopaymanager.data.local.entity.Mandate
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MandateViewModel @Inject constructor(
    private val repository: AutoPayRepository,
    private val preferenceManager: com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
) : ViewModel() {

    // Includes CANCELLED mandates (sorted below active) so the Home list can still show and
    // open them - callers that only want live autopays filter on status == "ACTIVE".
    val mandates: StateFlow<List<Mandate>> = repository.allMandatesForDisplay
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<Category>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMandate(mandate: Mandate, context: android.content.Context) {
        viewModelScope.launch {
            repository.insertMandate(mandate)
            val ttsHelper = com.uj.appstorysautopaymanager.tts.TextToSpeechHelper(context, preferenceManager)
            val merchantName = mandate.merchant.ifEmpty { "Subscription" }
            ttsHelper.speak(com.uj.appstorysautopaymanager.tts.AnnouncementKind.AUTOPAY_SET, merchantName, mandate.amount.toInt())
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

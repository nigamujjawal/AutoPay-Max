package com.uj.appstorysautopaymanager.ui.autopay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    val mandates: StateFlow<List<Mandate>> = repository.allMandates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addMandate(mandate: Mandate, context: android.content.Context) {
        viewModelScope.launch {
            repository.insertMandate(mandate)
            val ttsHelper = com.uj.appstorysautopaymanager.tts.TextToSpeechHelper(context, preferenceManager)
            val merchantName = mandate.merchant.ifEmpty { "Subscription" }
            ttsHelper.speak("AutoPay set for $merchantName of ${mandate.amount.toInt()} rupees.")
        }
    }

    fun updateMandate(mandate: Mandate) {
        viewModelScope.launch {
            repository.updateMandate(mandate)
        }
    }

    fun deleteMandate(mandate: Mandate) {
        viewModelScope.launch {
            repository.deleteMandate(mandate)
        }
    }

    fun toggleMandateStatus(mandate: Mandate) {
        viewModelScope.launch {
            val newStatus = if (mandate.status == "ACTIVE") "CANCELLED" else "ACTIVE"
            repository.updateMandate(mandate.copy(status = newStatus))
        }
    }
}

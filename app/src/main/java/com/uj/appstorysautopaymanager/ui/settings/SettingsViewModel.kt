package com.uj.appstorysautopaymanager.ui.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uj.appstorysautopaymanager.data.local.pref.PreferenceManager
import com.uj.appstorysautopaymanager.data.repository.AutoPayRepository
import com.uj.appstorysautopaymanager.util.ExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val repository: AutoPayRepository
) : ViewModel() {

    val theme: StateFlow<String> = preferenceManager.themeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "System")

    val currency: StateFlow<String> = preferenceManager.currencyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "₹")

    val language: StateFlow<String> = preferenceManager.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "English")

    val pinCode: StateFlow<String> = preferenceManager.pinCodeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isBiometricEnabled: StateFlow<Boolean> = preferenceManager.isBiometricEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isVoiceAlertsEnabled: StateFlow<Boolean> = preferenceManager.isVoiceAlertsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val speechSpeed: StateFlow<Float> = preferenceManager.speechSpeedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val speechLanguage: StateFlow<String> = preferenceManager.speechLanguageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "en")

    val defaultReminderDays: StateFlow<Int> = preferenceManager.defaultReminderDaysFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    val isOnboarded: StateFlow<Boolean> = preferenceManager.isOnboardedFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val voiceEngine: StateFlow<String> = preferenceManager.voiceEngineFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Male Voice")

    val voiceVolume: StateFlow<Float> = preferenceManager.voiceVolumeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 100f)

    val playChimeFirst: StateFlow<Boolean> = preferenceManager.playChimeFirstFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val alertTone: StateFlow<String> = preferenceManager.alertToneFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cashier")

    private val _backupStatus = MutableSharedFlow<String>()
    val backupStatus = _backupStatus.asSharedFlow()

    fun setIsOnboarded(value: Boolean) = viewModelScope.launch { preferenceManager.setIsOnboarded(value) }

    fun setTheme(value: String) = viewModelScope.launch { preferenceManager.setTheme(value) }
    fun setCurrency(value: String) = viewModelScope.launch { preferenceManager.setCurrency(value) }
    fun setLanguage(value: String) = viewModelScope.launch { preferenceManager.setLanguage(value) }
    fun setPinCode(value: String) = viewModelScope.launch { preferenceManager.setPinCode(value) }
    fun setBiometricEnabled(value: Boolean) = viewModelScope.launch { preferenceManager.setBiometricEnabled(value) }
    fun setVoiceAlertsEnabled(value: Boolean) = viewModelScope.launch { preferenceManager.setVoiceAlertsEnabled(value) }
    fun setSpeechSpeed(value: Float) = viewModelScope.launch { preferenceManager.setSpeechSpeed(value) }
    fun setSpeechLanguage(value: String) = viewModelScope.launch { preferenceManager.setSpeechLanguage(value) }
    fun setDefaultReminderDays(value: Int) = viewModelScope.launch { preferenceManager.setDefaultReminderDays(value) }
    fun setVoiceEngine(value: String) = viewModelScope.launch { preferenceManager.setVoiceEngine(value) }
    fun setVoiceVolume(value: Float) = viewModelScope.launch { preferenceManager.setVoiceVolume(value) }
    fun setPlayChimeFirst(value: Boolean) = viewModelScope.launch { preferenceManager.setPlayChimeFirst(value) }
    fun setAlertTone(value: String) = viewModelScope.launch { preferenceManager.setAlertTone(value) }

    fun exportBackup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bills = repository.allBills.first()
                val txns = repository.allTransactions.first()
                val json = ExportHelper.exportToJsonBackup(bills, txns)
                
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(json.toByteArray())
                }
                _backupStatus.emit("Backup Exported Successfully!")
            } catch (e: Exception) {
                _backupStatus.emit("Export Failed: ${e.localizedMessage}")
            }
        }
    }

    fun importBackup(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    val json = sb.toString()
                    val data = ExportHelper.importFromJsonBackup(json)
                    
                    for (bill in data.bills) {
                        repository.insertBill(bill)
                    }
                    for (txn in data.transactions) {
                        repository.insertTransaction(txn)
                    }
                    _backupStatus.emit("Backup Restored Successfully!")
                }
            } catch (e: Exception) {
                _backupStatus.emit("Restore Failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportToCsvFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bills = repository.allBills.first()
                val txns = repository.allTransactions.first()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    ExportHelper.exportToCsv(bills, txns, os)
                }
                _backupStatus.emit("CSV Exported Successfully!")
            } catch (e: Exception) {
                _backupStatus.emit("CSV Export Failed: ${e.localizedMessage}")
            }
        }
    }

    fun exportToPdfFile(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bills = repository.allBills.first()
                val txns = repository.allTransactions.first()
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    ExportHelper.exportToPdf(bills, txns, os)
                }
                _backupStatus.emit("PDF Exported Successfully!")
            } catch (e: Exception) {
                _backupStatus.emit("PDF Export Failed: ${e.localizedMessage}")
            }
        }
    }
}

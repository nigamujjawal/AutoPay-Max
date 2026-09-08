package com.uj.appstorysautopaymanager.tts

// The announcement shapes this app speaks. Kept as an enum + template map (not raw strings passed
// around) specifically so the *whole sentence* changes with the speech-language setting, not just
// the TTS engine's pronunciation accent on otherwise-English text - setting Locale("es") alone
// does nothing to translate the words, it only affects how the engine voices what it's handed.
//
// AUTOPAY_SET / AUTOPAY_DUE are the only ones the app actually speaks now (manual mandate setup /
// mandate payment reminder). CREDIT_RECEIVED / DEBIT_PAID are kept for the dormant
// detection->TTS path (SmsReceiver / UpiNotificationListenerService / GmailSyncWorker), which is
// currently dewired.
enum class AnnouncementKind { AUTOPAY_SET, AUTOPAY_DUE, CREDIT_RECEIVED, DEBIT_PAID }

object SpeechTemplates {
    // ponytail: translations are reasonable direct phrasing, not reviewed by a native speaker per
    // language - fix the wording the first time it sounds off in practice.
    // `money` is the currency-prefixed amount ("$649", "€649", "₹649") - the TTS engine voices the
    // symbol in the target language, so templates stay currency-neutral.
    private data class Templates(
        val autopaySet: (merchant: String, money: String) -> String,
        val autopayDue: (merchant: String, money: String) -> String,
        val creditReceived: (merchant: String, money: String) -> String,
        val debitPaid: (merchant: String, money: String) -> String
    )

    private val TEMPLATES: Map<String, Templates> = mapOf(
        "en" to Templates(
            autopaySet = { m, a -> "AutoPay set for $m of $a." },
            autopayDue = { m, a -> "$m autopay of $a is due in 2 days." },
            creditReceived = { _, a -> "$a received in your account." },
            debitPaid = { m, a -> "Paid $a to $m." }
        ),
        "es" to Templates(
            autopaySet = { m, a -> "Pago automático configurado para $m de $a." },
            autopayDue = { m, a -> "El pago automático de $m de $a vence en 2 días." },
            creditReceived = { _, a -> "$a recibidos en tu cuenta." },
            debitPaid = { m, a -> "Pagaste $a a $m." }
        ),
        "fr" to Templates(
            autopaySet = { m, a -> "Paiement automatique configuré pour $m de $a." },
            autopayDue = { m, a -> "Le paiement automatique de $m de $a est dû dans 2 jours." },
            creditReceived = { _, a -> "$a reçus sur votre compte." },
            debitPaid = { m, a -> "$a payés à $m." }
        ),
        "de" to Templates(
            autopaySet = { m, a -> "Automatische Zahlung für $m über $a eingerichtet." },
            autopayDue = { m, a -> "Automatische Zahlung für $m über $a fällig in 2 Tagen." },
            creditReceived = { _, a -> "$a auf Ihrem Konto eingegangen." },
            debitPaid = { m, a -> "$a an $m gezahlt." }
        ),
        "it" to Templates(
            autopaySet = { m, a -> "Pagamento automatico impostato per $m di $a." },
            autopayDue = { m, a -> "Il pagamento automatico di $m di $a scade tra 2 giorni." },
            creditReceived = { _, a -> "$a ricevuti sul tuo conto." },
            debitPaid = { m, a -> "Pagati $a a $m." }
        ),
        "pt" to Templates(
            autopaySet = { m, a -> "Pagamento automático configurado para $m de $a." },
            autopayDue = { m, a -> "O pagamento automático de $m de $a vence em 2 dias." },
            creditReceived = { _, a -> "$a recebidos na sua conta." },
            debitPaid = { m, a -> "$a pagos para $m." }
        ),
        "nl" to Templates(
            autopaySet = { m, a -> "Automatische betaling ingesteld voor $m van $a." },
            autopayDue = { m, a -> "Automatische betaling van $m van $a is over 2 dagen verschuldigd." },
            creditReceived = { _, a -> "$a ontvangen op je rekening." },
            debitPaid = { m, a -> "$a betaald aan $m." }
        ),
        "ru" to Templates(
            autopaySet = { m, a -> "Автоплатёж настроен для $m на $a." },
            autopayDue = { m, a -> "Автоплатёж $m на $a спишется через 2 дня." },
            creditReceived = { _, a -> "$a зачислено на ваш счёт." },
            debitPaid = { m, a -> "$a оплачено получателю $m." }
        ),
        "ar" to Templates(
            autopaySet = { m, a -> "تم إعداد الدفع التلقائي لـ $m بمبلغ $a." },
            autopayDue = { m, a -> "الدفع التلقائي لـ $m بمبلغ $a مستحق خلال يومين." },
            creditReceived = { _, a -> "تم استلام $a في حسابك." },
            debitPaid = { m, a -> "تم دفع $a إلى $m." }
        ),
        "ja" to Templates(
            autopaySet = { m, a -> "$m の自動支払いを $a で設定しました。" },
            autopayDue = { m, a -> "$m の自動支払い $a が2日後に予定されています。" },
            creditReceived = { _, a -> "$a を口座で受け取りました。" },
            debitPaid = { m, a -> "$m に $a を支払いました。" }
        ),
        "ko" to Templates(
            autopaySet = { m, a -> "$m 자동 결제를 $a 으로 설정했습니다." },
            autopayDue = { m, a -> "$m 자동 결제 $a 이 2일 후 예정되어 있습니다." },
            creditReceived = { _, a -> "$a 이 계좌로 입금되었습니다." },
            debitPaid = { m, a -> "$m 에 $a 를 결제했습니다." }
        ),
        "zh" to Templates(
            autopaySet = { m, a -> "已为 $m 设置 $a 的自动付款。" },
            autopayDue = { m, a -> "$m 的自动付款 $a 将在2天后扣款。" },
            creditReceived = { _, a -> "已收到 $a 到您的账户。" },
            debitPaid = { m, a -> "已向 $m 支付 $a。" }
        )
    )

    fun build(
        languageCode: String,
        kind: AnnouncementKind,
        merchant: String,
        amount: Int,
        currencySymbol: String
    ): String {
        val t = TEMPLATES[languageCode] ?: TEMPLATES.getValue("en")
        val money = "$currencySymbol$amount"
        return when (kind) {
            AnnouncementKind.AUTOPAY_SET -> t.autopaySet(merchant, money)
            AnnouncementKind.AUTOPAY_DUE -> t.autopayDue(merchant, money)
            AnnouncementKind.CREDIT_RECEIVED -> t.creditReceived(merchant, money)
            AnnouncementKind.DEBIT_PAID -> t.debitPaid(merchant, money)
        }
    }
}

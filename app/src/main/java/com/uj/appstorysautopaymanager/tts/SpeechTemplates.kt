package com.uj.appstorysautopaymanager.tts

// The 3 real announcement shapes this app ever speaks. Kept as an enum + template map (not raw
// strings passed around) specifically so the *whole sentence* changes with the speech-language
// setting, not just the TTS engine's pronunciation accent on otherwise-English text - setting
// Locale("hi") alone does nothing to translate the words, it only affects how the engine voices
// whatever text it's handed.
enum class AnnouncementKind { AUTOPAY_SET, CREDIT_RECEIVED, DEBIT_PAID }

object SpeechTemplates {
    // ponytail: translations are reasonable direct phrasing, not reviewed by a native speaker per
    // language - fix the wording for a specific language the first time it sounds off in practice.
    private data class Templates(
        val autopaySet: (merchant: String, amount: Int) -> String,
        val creditReceived: (merchant: String, amount: Int) -> String,
        val debitPaid: (merchant: String, amount: Int) -> String
    )

    private val TEMPLATES: Map<String, Templates> = mapOf(
        "en" to Templates(
            autopaySet = { m, a -> "AutoPay set for $m of $a rupees." },
            creditReceived = { m, a -> "$a Rupees received in your account" },
            debitPaid = { m, a -> "Paid $a rupees to $m." }
        ),
        "hi" to Templates(
            autopaySet = { m, a -> "$m के लिए $a रुपये का ऑटोपे सेट किया गया।" },
            creditReceived = { m, a -> "$a रुपये आपके खाते में प्राप्त हुए" },
            debitPaid = { m, a -> "$m को $a रुपये भेजे गए।" }
        ),
        "mr" to Templates(
            autopaySet = { m, a -> "$m साठी $a रुपयांचे ऑटोपे सेट केले." },
            creditReceived = { m, a -> "$a रुपये आपल्या खात्यात प्राप्त झाले." },
            debitPaid = { m, a -> "$m ला $a रुपये दिले." }
        ),
        "gu" to Templates(
            autopaySet = { m, a -> "$m માટે $a રૂપિયાનું ઓટોપે સેટ થયું." },
            creditReceived = { m, a -> "$a રૂપિયા તમારા ખાતામાં મળ્યા" },
            debitPaid = { m, a -> "$m ને $a રૂપિયા ચૂકવાયા." }
        ),
        "ta" to Templates(
            autopaySet = { m, a -> "$m க்கு $a ரூபாய் ஆட்டோபே அமைக்கப்பட்டது." },
            creditReceived = { m, a -> "உங்கள் கணக்கில் $a ரூபாய் பெறப்பட்டது" },
            debitPaid = { m, a -> "$m க்கு $a ரூபாய் செலுத்தப்பட்டது." }
        ),
        "te" to Templates(
            autopaySet = { m, a -> "$m కోసం $a రూపాయల ఆటోపే సెట్ చేయబడింది." },
            creditReceived = { m, a -> "మీ ఖాతాలో $a రూపాయలు పొందబడ్డాయి" },
            debitPaid = { m, a -> "$m కి $a రూపాయలు చెల్లించబడ్డాయి." }
        ),
        "kn" to Templates(
            autopaySet = { m, a -> "$m ಗಾಗಿ $a ರೂಪಾಯಿಗಳ ಆಟೋಪೇ ಹೊಂದಿಸಲಾಗಿದೆ." },
            creditReceived = { m, a -> "ನಿಮ್ಮ ಖಾತೆಗೆ $a ರೂಪಾಯಿಗಳು ಸ್ವೀಕರಿಸಲಾಗಿದೆ" },
            debitPaid = { m, a -> "$m ಗೆ $a ರೂಪಾಯಿಗಳು ಪಾವತಿಸಲಾಗಿದೆ." }
        ),
        "bn" to Templates(
            autopaySet = { m, a -> "$m এর জন্য $a টাকার অটোপে সেট করা হয়েছে।" },
            creditReceived = { m, a -> "$a টাকা আপনার অ্যাকাউন্টে জমা হয়েছে" },
            debitPaid = { m, a -> "$m কে $a টাকা প্রদান করা হয়েছে।" }
        ),
        "pa" to Templates(
            autopaySet = { m, a -> "$m ਲਈ $a ਰੁਪਏ ਦਾ ਆਟੋਪੇ ਸੈੱਟ ਕੀਤਾ ਗਿਆ।" },
            creditReceived = { m, a -> "$a ਰੁਪਏ ਤੁਹਾਡੇ ਖਾਤੇ ਵਿੱਚ ਪ੍ਰਾਪਤ ਹੋਏ।" },
            debitPaid = { m, a -> "$m ਨੂੰ $a ਰੁਪਏ ਭੇਜੇ ਗਏ।" }
        )
    )

    fun build(languageCode: String, kind: AnnouncementKind, merchant: String, amount: Int): String {
        val t = TEMPLATES[languageCode] ?: TEMPLATES.getValue("en")
        return when (kind) {
            AnnouncementKind.AUTOPAY_SET -> t.autopaySet(merchant, amount)
            AnnouncementKind.CREDIT_RECEIVED -> t.creditReceived(merchant, amount)
            AnnouncementKind.DEBIT_PAID -> t.debitPaid(merchant, amount)
        }
    }
}

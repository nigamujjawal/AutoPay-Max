package com.uj.appstorysautopaymanager.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.regex.Pattern

// Exercises EmailParser's mandate/debit branch logic with injected patterns (bypassing Remote
// Config / Firebase), same approach as AutoPayRepositoryRecurringTest.
class EmailParserTest {

    private val now = 1_700_000_000_000L

    // The real shipped Google Play patterns (EmailPatternConfig.DEFAULT_LISTS) - referenced
    // directly so this test breaks if those regexes are changed without re-verifying.
    private val playMandatePatterns = EmailPatternConfig.DEFAULT_LISTS.getValue("google_play_mandate")
    private val playCancelPatterns = EmailPatternConfig.DEFAULT_LISTS.getValue("google_play_cancel")

    // --- Real captured Google Play receipt emails (2026-08 / 2026-09) --------------------------

    private val claudeRenewalSubject = "Your Google Play Order Receipt from 20 Aug 2026"
    private val claudeRenewalBody = """
        Google Play
        Thank you
        Your subscription from Anthropic, PBC on Google Play has renewed. Manage your subscriptions
        Order number: GPA.3326-2876-5025-81188..2
        Order date: 20 Aug 2026 8:52:22 pm IST
        Your account: sharmavijaystore@gmail.com
        Item    Price
        Claude Pro Subscription (Claude by Anthropic)    ₹1,999.00/month
        Auto-renewing subscription
        Total: ₹1,999.00/month
        (Includes GST of ₹304.93)
        Payment method:
        UPI: Paytm
    """.trimIndent()

    private val feenkoTrialSubject = "Your Google Play Order Receipt from 1 Sept 2026"
    private val feenkoTrialBody = """
        Google Play
        Thank you
        You have signed up for a trial subscription from GOTYOURBACK TECHNOLOGIES LTD on Google Play. Your trial will end on 4 Sept 2026. You will be automatically charged the subscription cost (currently ₹4,550.00/6 months) at the end of your trial unless cancelled. You can cancel at any time. Manage your subscriptions
        Order number: GPA.3351-1587-6057-26561
        Order date: 1 Sept 2026 5:37:11 pm IST
        Your account: sharmavijaystore@gmail.com
        Item    Price
        Premium 6 month (trial) (Feenko: Subscription Manager)    ₹0.00
        Auto-renewing subscription
        Total: ₹0.00
        (Includes GST of ₹0.00)
    """.trimIndent()

    @Test
    fun `google play renewal - mandate on the recurring price plus a real charge`() {
        val result = EmailParser.parseWith(
            "google_play", claudeRenewalSubject, claudeRenewalBody, now, "gp1",
            mandatePatterns = playMandatePatterns, debitPatterns = emptyList()
        )
        assertNotNull(result)
        val mandate = result!!.mandate
        assertNotNull(mandate)
        assertEquals(1999.0, mandate!!.amount, 0.001)
        assertEquals("MONTHLY", mandate.frequency)
        // merchant is the app name from the parens, not the product string / company
        assertEquals("Claude by Anthropic", mandate.merchant)
        // Play-billed -> cancellable via the Play Store (MandateDetailScreen forks on this).
        assertEquals("GOOGLE_PLAY", mandate.source)
        // A renewal actually billed money -> a Passbook charge for the same amount.
        assertEquals(1999.0, result.transaction.amount, 0.001)
        assertTrue(result.transaction.isAutoPay)
        assertEquals("email_gp1", result.transaction.smsId)
    }

    @Test
    fun `google play free trial - mandate on the future price, no phantom charge`() {
        val result = EmailParser.parseWith(
            "google_play", feenkoTrialSubject, feenkoTrialBody, now, "gp2",
            mandatePatterns = playMandatePatterns, debitPatterns = emptyList()
        )
        assertNotNull(result)
        val mandate = result!!.mandate
        assertNotNull(mandate)
        assertEquals(4550.0, mandate!!.amount, 0.001)
        assertEquals("HALF_YEARLY", mandate.frequency)
        // app name from the item-line parens, consistent with the cancellation email
        assertEquals("Feenko: Subscription Manager", mandate.merchant)
        // Trial billed ₹0 now -> transaction amount is 0, GmailSyncWorker logs no Passbook row.
        assertEquals(0.0, result.transaction.amount, 0.001)
    }

    @Test
    fun `google play cancellation flips the mandate to CANCELLED with the matching app name`() {
        val result = EmailParser.parseWith(
            "google_play",
            "Your Feenko: Subscription Manager subscription will be cancelled",
            "Your trial ends on 4 Sept 2026 and the subscription will not renew.",
            now, "gpc1",
            mandatePatterns = playMandatePatterns, debitPatterns = emptyList(),
            cancelPatterns = playCancelPatterns
        )
        assertNotNull(result)
        assertEquals("CANCELLED", result!!.mandate?.status)
        // must equal the mandate merchant from the sign-up so the DB cancel matches
        assertEquals("Feenko: Subscription Manager", result.mandate?.merchant)
        assertEquals(0.0, result.transaction.amount, 0.001)
    }

    @Test
    fun `a renewal email is not misread as a cancellation`() {
        val result = EmailParser.parseWith(
            "google_play", claudeRenewalSubject, claudeRenewalBody, now, "gp9",
            mandatePatterns = playMandatePatterns, debitPatterns = emptyList(),
            cancelPatterns = playCancelPatterns
        )
        assertEquals("ACTIVE", result?.mandate?.status)
    }

    @Test
    fun `a non-receipt google email returns null`() {
        val result = EmailParser.parseWith(
            "google_play", "Security alert", "A new device signed in to your Google Account.",
            now, "gp3", mandatePatterns = playMandatePatterns, debitPatterns = emptyList()
        )
        assertNull(result)
    }

    @Test
    fun `vendorKeyFor matches the play sender address and subscription domains`() {
        assertEquals("google_play", EmailParser.vendorKeyFor("Google Play <googleplay-noreply@google.com>"))
        assertEquals("netflix", EmailParser.vendorKeyFor("Netflix <info@account.netflix.com>"))
        assertEquals("spotify", EmailParser.vendorKeyFor("no-reply@spotify.com"))
        // plain google.com (not the play address) is not tracked
        assertNull(EmailParser.vendorKeyFor("no-reply@google.com"))
        assertNull(EmailParser.vendorKeyFor("friend@gmail.com"))
    }

    @Test
    fun `a non-play vendor mandate is tagged source EMAIL`() {
        val membership = UpiPatternConfig.TxnPattern(
            regex = Pattern.compile("(?i)membership .*?(?:₹|Rs\\.?|INR)\\s*([\\d,]+).*?renew"),
            amountGroup = 1, senderGroup = -1
        )
        val result = EmailParser.parseWith(
            "netflix", "Your receipt", "Your membership of Rs 649 will renew monthly.",
            now, "nf1", mandatePatterns = listOf(membership), debitPatterns = emptyList()
        )
        assertNotNull(result?.mandate)
        assertEquals("EMAIL", result!!.mandate?.source)
    }

    @Test
    fun `injected debit pattern produces a plain transaction`() {
        val debit = UpiPatternConfig.TxnPattern(
            regex = Pattern.compile("(?i)your (?:₹|Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{2})?) payment .* was successful"),
            amountGroup = 1, senderGroup = -1
        )
        val result = EmailParser.parseWith(
            "spotify", "Receipt", "Your Rs 119.00 payment to Spotify was successful.",
            now, "sp1", mandatePatterns = emptyList(), debitPatterns = listOf(debit)
        )
        assertNotNull(result)
        assertNull(result!!.mandate)
        assertEquals(119.0, result.transaction.amount, 0.001)
        assertEquals(false, result.transaction.isAutoPay)
    }
}

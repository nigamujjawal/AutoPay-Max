package com.autopaymax.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScreenshotOcrParserTest {

    @Test
    fun `known app plus monthly price`() {
        val result = ScreenshotOcrParser.parse("Netflix Premium\nNext billing date 12 Oct\n₹649/month")
        assertEquals("Netflix", result.merchant)
        assertEquals(649.0, result.amount)
        assertEquals("Monthly", result.frequency)
    }

    @Test
    fun `annual billing wording maps to Yearly`() {
        val result = ScreenshotOcrParser.parse("Your Spotify Premium plan\nRs. 1189 billed annually")
        assertEquals("Spotify", result.merchant)
        assertEquals(1189.0, result.amount)
        assertEquals("Yearly", result.frequency)
    }

    @Test
    fun `several prices on screen - smallest wins over a struck-through or yearly total`() {
        val result = ScreenshotOcrParser.parse("Upgrade plan\n$19.99/month\nor save with $199.99/year")
        assertEquals(19.99, result.amount)
    }

    @Test
    fun `unrecognized screenshot yields nulls, never a guess`() {
        val result = ScreenshotOcrParser.parse("Random unrecognized screenshot with no price")
        assertNull(result.merchant)
        assertNull(result.amount)
        assertNull(result.frequency)
    }

    // Real captured OCR output (ML Kit Latin recognizer, 2026-09-17) off a Play Store
    // "Manage subscriptions" screenshot - the ₹ symbol was dropped entirely, and the status bar's
    // "6.36 KB/s" network indicator got OCR'd as a stray number too. The keyword-anchored fallback
    // has to find the real price via "Next payment:" and ignore the decoy.
    private val playStoreSubscriptionsScreenshot = """
        12:34
        ( Subscriptions
        Active
        Google may share subscription data that doesn't
        personally identify you with developers to help
        them offer subscriptions. Learn more about
        subscriptions
        Claude by Anthropic
        Claude Pro Subscription
        Next payment: 1,999.00 on 20 Sept 2026
        Your plan includes
        6.36
        KB/s
        V Higher usage for more conversations
        V Access to the smartest Claude models
        V Create unlimited Projects with Claude
        Expired
        Feenko: Subscription Manager
        Premium 6 month (trial)
        Expired on 4 Sept 2026
        Resubscribe
        Remove
        Vo 1
    """.trimIndent()

    @Test
    fun `Play Store screenshot with the currency symbol dropped by OCR still resolves the real price`() {
        val result = ScreenshotOcrParser.parse(playStoreSubscriptionsScreenshot)
        assertEquals("Claude AI", result.merchant)
        assertEquals(1999.0, result.amount)
        assertEquals("Monthly", result.frequency)
    }
}

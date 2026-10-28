package com.arcticfishtrail.game.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CampaignParserTest {

    @Test
    fun parse_splitsIntoOrderedSubs_withoutDecoding() {
        val subs = CampaignParser.parse("android1_AR_sub3%26_sub4_WBZ_sub6")
        assertEquals(
            listOf(
                "sub1" to "android1", "sub2" to "AR", "sub3" to "sub3%26",
                "sub4" to "sub4", "sub5" to "WBZ", "sub6" to "sub6",
            ),
            subs.toList(),
        )
    }

    @Test
    fun parse_keepsEmptySegments() {
        assertEquals(listOf("sub1" to "a", "sub2" to "", "sub3" to "c"), CampaignParser.parse("a__c").toList())
    }

    @Test
    fun parse_emptyCampaign_givesNoSubs() {
        assertTrue(CampaignParser.parse("").isEmpty())
    }

    @Test
    fun toQuery_doesNotDoubleEncode() {
        val query = CampaignParser.toQuery(CampaignParser.parse("android1_sub3%26"))
        assertEquals("sub1=android1&sub2=sub3%26", query)
    }
}

class UrlSchemesTest {

    @Test
    fun allowedSchemes_stayInside() {
        listOf(
            "http://a", "https://a", "about:blank", "blob:https://a/1", "data:text/html,x",
            "javascript:void(0)", "file:///x", "srcdoc:x", "HTTPS://A",
        ).forEach { assertFalse(it, UrlSchemes.isExternal(it)) }
    }

    @Test
    fun otherSchemes_areExternal() {
        listOf("intent://x#Intent;end", "market://details?id=a", "tg://resolve", "mailto:a@b.c", "tel:123")
            .forEach { assertTrue(it, UrlSchemes.isExternal(it)) }
    }

    @Test
    fun relativeUrls_stayInside() {
        listOf("/path", "/path:with:colons", "page.html", "?q=1", "#x", "2go:x", "")
            .forEach { assertFalse(it, UrlSchemes.isExternal(it)) }
    }
}

class DeepLinkRouterTest {

    @Test
    fun queuedLinks_areDeliveredOnSetHandler() {
        DeepLinkRouter.clearHandler()
        DeepLinkRouter.handle("https://a")
        DeepLinkRouter.handle("https://b")
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received += it }
        assertEquals(listOf("https://a", "https://b"), received)
        DeepLinkRouter.clearHandler()
    }

    @Test
    fun liveHandler_receivesImmediately_andBlankIsIgnored() {
        val received = mutableListOf<String>()
        DeepLinkRouter.setHandler { received += it }
        DeepLinkRouter.handle("https://c")
        DeepLinkRouter.handle("")
        DeepLinkRouter.handle(null)
        assertEquals(listOf("https://c"), received)
        DeepLinkRouter.clearHandler()
    }

    @Test
    fun linksAreDeliveredOnlyOnce() {
        DeepLinkRouter.clearHandler()
        DeepLinkRouter.handle("https://d")
        val first = mutableListOf<String>()
        DeepLinkRouter.setHandler { first += it }
        val second = mutableListOf<String>()
        DeepLinkRouter.setHandler { second += it }
        assertEquals(listOf("https://d"), first)
        assertTrue(second.isEmpty())
        DeepLinkRouter.clearHandler()
    }
}

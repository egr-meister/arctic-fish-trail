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

class OfferUrlBuilderTest {

    private val params = linkedMapOf(
        "af_status" to "Non-organic",
        "media_source" to "facebook",
        "campaign" to "android1_AR_sub3%26_sub4_WBZ_sub6",
    )

    private fun build(
        base: String = "https://secret-joker.cfd/",
        p: Map<String, String> = params,
        id: String? = "test-af-id",
    ) = OfferUrlBuilder.build(base, p, id)

    @Test
    fun build_matchesExpectedUrl() {
        assertEquals(
            "https://secret-joker.cfd/android1?af_status=Non-organic&media_source=facebook" +
                "&campaign=android1_AR_sub3%26_sub4_WBZ_sub6&sub1=android1&sub2=AR&sub3=sub3%26" +
                "&sub4=sub4&sub5=WBZ&sub6=sub6&appsflyer_id=test-af-id",
            build(),
        )
    }

    @Test
    fun build_keepsEveryAppsFlyerParam_andCampaign() {
        val url = build()
        assertTrue(url.startsWith("https://secret-joker.cfd/android1?"))
        params.forEach { (k, v) -> assertTrue("missing $k", url.contains("$k=$v")) }
        (1..6).forEach { assertTrue("missing sub$it", url.contains("sub$it=")) }
        assertTrue(url.contains("&sub1=android1"))
        assertTrue(url.contains("appsflyer_id=test-af-id"))
        assertFalse(url.contains("%2526"))
    }

    @Test
    fun build_keepsUnknownKeys() {
        val url = build(p = params + ("brand_new_key" to "x y"))
        assertTrue(url.contains("brand_new_key=x%20y"))
    }

    @Test
    fun build_noCampaign_noPathSegment() {
        val url = build(p = linkedMapOf("af_status" to "Organic"))
        assertEquals("https://secret-joker.cfd/?af_status=Organic&appsflyer_id=test-af-id", url)
    }

    @Test
    fun build_emptySub1_noPathSegment() {
        val url = build(p = linkedMapOf("campaign" to "_AR"))
        assertEquals("https://secret-joker.cfd/?campaign=_AR&sub1=&sub2=AR&appsflyer_id=test-af-id", url)
    }

    @Test
    fun build_appendsAfterExistingPath() {
        val url = build(base = "https://domain.com/offer/", p = linkedMapOf("campaign" to "test"), id = null)
        assertEquals("https://domain.com/offer/test?campaign=test&sub1=test", url)
    }

    @Test
    fun build_isIdempotentOnPath() {
        val first = build()
        val second = OfferUrlBuilder.build(first, params, "test-af-id")
        assertEquals(first, second)
    }

    @Test
    fun build_keepsFragment_andSkipsBlankId() {
        val url = build(base = "https://d.com/#top", p = linkedMapOf("campaign" to "a"), id = " ")
        assertEquals("https://d.com/a?campaign=a&sub1=a#top", url)
    }
}

class RoutingRulesTest {

    @Test
    fun notFound_isWhite() {
        assertEquals(RouteDecision.White, RoutingRules.decide(ProbeResult(404, "https://x"), "https://req"))
    }

    @Test
    fun anyOtherStatus_isBlack_withFinalUrl() {
        listOf(200, 204, 302, 403, 500).forEach { code ->
            assertEquals(
                RouteDecision.Black("https://final"),
                RoutingRules.decide(ProbeResult(code, "https://final"), "https://req"),
            )
        }
    }

    @Test
    fun missingFinalUrl_fallsBackToRequested() {
        assertEquals(RouteDecision.Black("https://req"), RoutingRules.decide(ProbeResult(200, null), "https://req"))
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

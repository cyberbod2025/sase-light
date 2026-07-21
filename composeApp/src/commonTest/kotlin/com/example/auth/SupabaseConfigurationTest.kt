package com.example.auth

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupabaseConfigurationTest {

    @Test
    fun absentValuesProduceNotConfigured() {
        assertIs<SupabaseConfigurationState.NotConfigured>(
            SupabaseConfigurationState.from(null, null)
        )
        assertIs<SupabaseConfigurationState.NotConfigured>(
            SupabaseConfigurationState.from("  ", "")
        )
    }

    @Test
    fun missingUrlOrKeyIsInvalidNotConfigured() {
        assertProblem(
            SupabaseConfigurationState.from(null, "sb_publishable_demo_key"),
            SupabaseConfigurationProblem.MISSING_URL
        )
        assertProblem(
            SupabaseConfigurationState.from("https://demo.supabase.co", null),
            SupabaseConfigurationProblem.MISSING_KEY
        )
    }

    @Test
    fun insecureOrMalformedUrlIsRejected() {
        assertProblem(
            SupabaseConfigurationState.from("http://demo.supabase.co", "sb_publishable_demo_key"),
            SupabaseConfigurationProblem.INSECURE_URL
        )
        assertProblem(
            SupabaseConfigurationState.from("https://", "sb_publishable_demo_key"),
            SupabaseConfigurationProblem.MALFORMED_URL
        )
        assertProblem(
            SupabaseConfigurationState.from("https://sin-punto", "sb_publishable_demo_key"),
            SupabaseConfigurationProblem.MALFORMED_URL
        )
    }

    @Test
    fun validValuesProduceConfigured() {
        val state = SupabaseConfigurationState.from(
            " https://demo.supabase.co ",
            " sb_publishable_demo_key "
        )
        val configured = assertIs<SupabaseConfigurationState.Configured>(state)
        assertEquals("https://demo.supabase.co", configured.configuration.url)
        assertEquals("sb_publishable_demo_key", configured.configuration.anonKey)
    }

    @OptIn(ExperimentalEncodingApi::class)
    @Test
    fun serviceRoleJwtIsRejected() {
        val header = Base64.UrlSafe.encode("{\"alg\":\"HS256\"}".encodeToByteArray())
        val payload = Base64.UrlSafe.encode(
            "{\"role\":\"service_role\",\"iss\":\"supabase-demo\"}".encodeToByteArray()
        )
        val syntheticServiceKey = "$header.$payload.firma-sintetica"
        assertProblem(
            SupabaseConfigurationState.from("https://demo.supabase.co", syntheticServiceKey),
            SupabaseConfigurationProblem.SERVICE_ROLE_KEY_REJECTED
        )
    }

    @Test
    fun safeRepresentationNeverShowsTheKey() {
        val secretishValue = "sb_publishable_demo_key_no_debe_verse"
        val configuration = SupabaseConfiguration("https://demo.supabase.co", secretishValue)
        assertFalse(configuration.toString().contains(secretishValue))
        assertTrue(configuration.toString().contains("***"))
        assertTrue(configuration.toString().contains("https://demo.supabase.co"))
    }

    private fun assertProblem(
        state: SupabaseConfigurationState,
        expected: SupabaseConfigurationProblem
    ) {
        val invalid = assertIs<SupabaseConfigurationState.Invalid>(state)
        assertEquals(expected, invalid.problem)
    }
}

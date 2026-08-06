package dev.gaphunter.asyncapicompanion.reference

import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * [AsyncApiJsonSchemaGtdSuppressor] has one real branch worth testing
 * without a live IDE sandbox: it must suppress on a recognized AsyncAPI
 * file (so this plugin's own references get a chance to resolve
 * instead of losing to the bundled handler), and never on an
 * unrecognized one.
 */
class AsyncApiJsonSchemaGtdSuppressorTest : BasePlatformTestCase() {

    fun testSuppressesOnARecognizedAsyncApiFile() {
        val file = myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            components:
              messages:
                UserSignedUp:
                  payload:
                    type: object
            """.trimIndent(),
        )
        assertTrue(AsyncApiJsonSchemaGtdSuppressor().shouldSuppressGtd(file))
    }

    fun testDoesNotSuppressOutsideARecognizedAsyncApiFile() {
        val file = myFixture.configureByText(
            "plain.yaml",
            """
            some: value
            """.trimIndent(),
        )
        assertFalse(AsyncApiJsonSchemaGtdSuppressor().shouldSuppressGtd(file))
    }
}

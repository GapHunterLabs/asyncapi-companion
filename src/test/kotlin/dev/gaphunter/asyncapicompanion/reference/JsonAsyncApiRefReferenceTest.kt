package dev.gaphunter.asyncapicompanion.reference

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase

/**
 * Real PSI resolution across 2+ fixture files -- confirms `$ref`
 * resolves locally (same-file and cross-file) in a JSON-formatted
 * AsyncAPI document, that a genuinely missing target resolves to null
 * instead of throwing, and that an http(s) `$ref` never resolves (this
 * plugin has no HTTP client anywhere).
 */
class JsonAsyncApiRefReferenceTest : BasePlatformTestCase() {

    private fun findRefStringLiteral(file: PsiFile): JsonStringLiteral {
        val jsonFile = file as JsonFile
        return PsiTreeUtil.findChildrenOfType(jsonFile, JsonStringLiteral::class.java)
            .first { literal ->
                val property = literal.parent as? JsonProperty
                property?.name == "\$ref" && property.value === literal
            }
    }

    fun testResolvesSameFilePointerToAComponentMessage() {
        myFixture.configureByText(
            "asyncapi.json",
            """
            {
              "asyncapi": "2.6.0",
              "channels": {
                "user/signedup": { "subscribe": { "message": {
                  "${'$'}ref": "#/components/messages/UserSignedUp"
                } } }
              },
              "components": {
                "messages": {
                  "UserSignedUp": { "payload": { "type": "object" } }
                }
              }
            }
            """.trimIndent(),
        )
        val refLiteral = findRefStringLiteral(myFixture.file)
        val resolved = JsonAsyncApiRefReference(refLiteral).resolve()
        assertNotNull("expected the \$ref to resolve", resolved)
        val property = resolved!!.parent as JsonProperty
        assertEquals("UserSignedUp", property.name)
    }

    fun testResolvesRefAcrossTwoFiles() {
        myFixture.addFileToProject(
            "messages/user-signed-up.json",
            """
            { "UserSignedUp": { "payload": { "type": "object" } } }
            """.trimIndent(),
        )
        myFixture.configureByText(
            "asyncapi.json",
            """
            {
              "asyncapi": "2.6.0",
              "components": {
                "messages": {
                  "SignupRef": { "${'$'}ref": "messages/user-signed-up.json#/UserSignedUp" }
                }
              }
            }
            """.trimIndent(),
        )
        val refLiteral = findRefStringLiteral(myFixture.file)
        val resolved = JsonAsyncApiRefReference(refLiteral).resolve()
        assertNotNull("expected the cross-file \$ref to resolve", resolved)
        val property = resolved!!.parent as JsonProperty
        assertEquals("UserSignedUp", property.name)
        assertEquals("user-signed-up.json", property.containingFile.name)
    }

    fun testDoesNotResolveWhenTargetIsMissing() {
        myFixture.configureByText(
            "asyncapi.json",
            """
            {
              "asyncapi": "2.6.0",
              "components": {
                "messages": {
                  "SignupRef": { "${'$'}ref": "#/components/messages/DoesNotExist" }
                }
              }
            }
            """.trimIndent(),
        )
        val refLiteral = findRefStringLiteral(myFixture.file)
        assertNull(JsonAsyncApiRefReference(refLiteral).resolve())
    }

    fun testNeverResolvesAnHttpRef() {
        myFixture.configureByText(
            "asyncapi.json",
            """
            {
              "asyncapi": "2.6.0",
              "components": {
                "messages": {
                  "SignupRef": { "${'$'}ref": "https://example.com/schemas/user.json" }
                }
              }
            }
            """.trimIndent(),
        )
        val refLiteral = findRefStringLiteral(myFixture.file)
        assertNull(
            "an http(s) \$ref must never resolve via network access",
            JsonAsyncApiRefReference(refLiteral).resolve(),
        )
    }

    fun testDoesNotAttachOurReferenceOutsideARecognizedAsyncApiFile() {
        myFixture.configureByText(
            "plain.json",
            """
            {
              "${'$'}ref": "#/definitions/User",
              "definitions": { "User": {} }
            }
            """.trimIndent(),
        )
        val refLiteral = findRefStringLiteral(myFixture.file)
        assertNull(JsonAsyncApiRefUtil.asRefProperty(refLiteral))
    }
}

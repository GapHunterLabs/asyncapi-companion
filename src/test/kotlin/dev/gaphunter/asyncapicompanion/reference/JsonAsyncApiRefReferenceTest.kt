package dev.gaphunter.asyncapicompanion.reference

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
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

    /**
     * Real end-to-end Ctrl+B/Ctrl+Click simulation -- [myFixture.gotoDeclaration]
     * runs the platform's actual `GotoDeclarationAction` pipeline (every
     * registered `GotoDeclarationHandler`, THEN generic `PsiReference`
     * resolution, with [AsyncApiJsonSchemaGtdSuppressor] consulted in
     * between), unlike every other test in this file/class which calls
     * `resolve()` on our own reference class directly and so can never
     * catch a suppressor/registration problem. Added 2026-08-12 after a
     * real runIde sandbox showed "No usages found" on this exact caret
     * position -- see [[openapi_companion_ctrlclick_broken_with_trial]]
     * memory entry for the full investigation.
     */
    fun testGotoDeclarationNavigatesThroughTheRealPlatformPipeline() {
        myFixture.configureByText(
            "asyncapi.json",
            """
            {
              "asyncapi": "2.6.0",
              "channels": {
                "user/signedup": { "subscribe": { "message": {
                  "${'$'}ref": "#/components/messages/UserSignedUp<caret>"
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
        val editor = myFixture.editor
        val offset = editor.caretModel.offset
        val targets = GotoDeclarationAction.findAllTargetElements(myFixture.project, editor, offset)
        assertTrue(
            "expected Ctrl+B at the \$ref caret to navigate to the UserSignedUp " +
                "definition via the real platform pipeline, but got no targets " +
                "(this is the exact 'No usages found' symptom seen live)",
            targets.isNotEmpty(),
        )
    }
}

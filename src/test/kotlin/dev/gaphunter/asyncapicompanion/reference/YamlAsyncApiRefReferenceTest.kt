package dev.gaphunter.asyncapicompanion.reference

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML counterpart of [JsonAsyncApiRefReferenceTest] -- AsyncAPI
 * documents are written in YAML at least as often as JSON in real-world
 * use, and this is a separate PSI backend, not just a different syntax
 * for the same resolver.
 */
class YamlAsyncApiRefReferenceTest : BasePlatformTestCase() {

    private fun findRefScalar(file: PsiFile): YAMLScalar {
        val yamlFile = file as YAMLFile
        return PsiTreeUtil.findChildrenOfType(yamlFile, YAMLScalar::class.java)
            .first { scalar ->
                val keyValue = scalar.parent as? YAMLKeyValue
                keyValue?.keyText == "\$ref" && keyValue.value === scalar
            }
    }

    fun testResolvesSameFilePointerToAComponentMessage() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            channels:
              user/signedup:
                subscribe:
                  message:
                    ${'$'}ref: '#/components/messages/UserSignedUp'
            components:
              messages:
                UserSignedUp:
                  payload:
                    type: object
            """.trimIndent(),
        )
        val refScalar = findRefScalar(myFixture.file)
        val resolved = YamlAsyncApiRefReference(refScalar).resolve()
        assertNotNull("expected the \$ref to resolve", resolved)
        val keyValue = resolved!!.parent as YAMLKeyValue
        assertEquals("UserSignedUp", keyValue.keyText)
    }

    fun testResolvesRefAcrossTwoFiles() {
        myFixture.addFileToProject(
            "messages/user-signed-up.yaml",
            """
            UserSignedUp:
              payload:
                type: object
            """.trimIndent(),
        )
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            components:
              messages:
                SignupRef:
                  ${'$'}ref: 'messages/user-signed-up.yaml#/UserSignedUp'
            """.trimIndent(),
        )
        val refScalar = findRefScalar(myFixture.file)
        val resolved = YamlAsyncApiRefReference(refScalar).resolve()
        assertNotNull("expected the cross-file \$ref to resolve", resolved)
        val keyValue = resolved!!.parent as YAMLKeyValue
        assertEquals("UserSignedUp", keyValue.keyText)
        assertEquals("user-signed-up.yaml", keyValue.containingFile.name)
    }

    fun testDoesNotResolveWhenTargetIsMissingInAMultiKeyCrossFileTarget() {
        // Reproduces a real demo-data bug: the target file has TWO
        // top-level keys (not just one, like testResolvesRefAcrossTwoFiles
        // above), and the $ref points at a THIRD key that doesn't exist.
        myFixture.addFileToProject(
            "components.yaml",
            """
            OrderPlaced:
              type: object
            ShipmentDispatched:
              type: object
            """.trimIndent(),
        )
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            channels:
              order/refunded:
                subscribe:
                  message:
                    ${'$'}ref: 'components.yaml#/RefundIssued'
            """.trimIndent(),
        )
        val refScalar = findRefScalar(myFixture.file)
        val resolved = YamlAsyncApiRefReference(refScalar).resolve()
        assertNull("expected RefundIssued to NOT resolve -- it's not a key in components.yaml", resolved)
    }

    fun testDoesNotResolveWhenTargetIsMissing() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            components:
              messages:
                SignupRef:
                  ${'$'}ref: '#/components/messages/DoesNotExist'
            """.trimIndent(),
        )
        val refScalar = findRefScalar(myFixture.file)
        assertNull(YamlAsyncApiRefReference(refScalar).resolve())
    }

    fun testNeverResolvesAnHttpRef() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            components:
              messages:
                SignupRef:
                  ${'$'}ref: 'https://example.com/schemas/user.yaml'
            """.trimIndent(),
        )
        val refScalar = findRefScalar(myFixture.file)
        assertNull(
            "an http(s) \$ref must never resolve via network access",
            YamlAsyncApiRefReference(refScalar).resolve(),
        )
    }

    /**
     * Goes through the real extension pipeline via `getReferenceAtCaretPosition`,
     * the same path Ctrl+Click/Ctrl+B use in the IDE -- catches wiring bugs
     * (wrong `language=` in plugin.xml, a contributor that never gets
     * registered) that constructing the reference class directly would miss.
     */
    fun testContributedReferenceIsAttachedThroughTheRealExtensionPipeline() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            channels:
              order/shipped:
                subscribe:
                  message:
                    ${'$'}ref: '#/components/messages/OrderShip<caret>ped'
            components:
              messages:
                OrderShipped:
                  payload:
                    type: object
            """.trimIndent(),
        )
        val reference = myFixture.getReferenceAtCaretPosition()
        assertNotNull("expected a reference to be contributed at the caret", reference)
        assertTrue(
            "expected our own contributor's reference type, not some other plugin's",
            reference is YamlAsyncApiRefReference,
        )
        val resolved = reference!!.resolve()
        assertNotNull("expected the contributed reference to resolve", resolved)
        val keyValue = resolved!!.parent as YAMLKeyValue
        assertEquals("OrderShipped", keyValue.keyText)
    }

    fun testDoesNotAttachOurReferenceOutsideARecognizedAsyncApiFile() {
        myFixture.configureByText(
            "plain.yaml",
            """
            ${'$'}ref: '#/definitions/User'
            definitions:
              User: {}
            """.trimIndent(),
        )
        val refScalar = findRefScalar(myFixture.file)
        assertNull(YamlAsyncApiRefUtil.asRefKeyValue(refScalar))
    }
}

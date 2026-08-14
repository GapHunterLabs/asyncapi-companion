package dev.gaphunter.asyncapicompanion.reference

import com.intellij.codeInsight.navigation.actions.GotoDeclarationAction
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

    /**
     * Real end-to-end Ctrl+B/Ctrl+Click simulation on a YAML AsyncAPI
     * file -- same real-pipeline check as
     * [JsonAsyncApiRefReferenceTest.testGotoDeclarationNavigatesThroughTheRealPlatformPipeline],
     * but reproducing the exact format (YAML, not JSON) used in the
     * live `runIde` sandbox session where Ctrl+Click showed "No usages
     * found" -- see [[openapi_companion_ctrlclick_broken_with_trial]]
     * memory entry. [testContributedReferenceIsAttachedThroughTheRealExtensionPipeline]
     * above only proves the generic PsiReference is attached; it does
     * NOT prove GotoDeclarationAction picks it, because
     * GotoDeclarationHandlers (the suppressor's territory) are
     * consulted first and could still win even with a valid reference
     * sitting right there.
     */
    fun testGotoDeclarationNavigatesThroughTheRealPlatformPipeline() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            channels:
              user/signedup:
                subscribe:
                  message:
                    ${'$'}ref: '#/components/messages/UserSignedUp<caret>'
            components:
              messages:
                UserSignedUp:
                  payload:
                    type: object
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

    /**
     * Direct unit test of [AsyncApiGotoDeclarationHandler] -- added
     * 2026-08-13 as the actual fix for the confirmed platform bug (see
     * `openapi_companion_ctrlclick_broken_with_trial` memory entry):
     * logging in a real `runIde` sandbox proved the suppressor +
     * generic-PsiReference-fallback hand-off silently breaks after
     * Ctrl+B, even though both halves work correctly in isolation. A
     * real `GotoDeclarationHandler` sidesteps that hand-off -- the
     * platform calls it directly, same extension point the bundled
     * handler itself uses.
     */
    fun testGotoDeclarationHandlerResolvesTheSameRefDirectly() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            channels:
              user/signedup:
                subscribe:
                  message:
                    ${'$'}ref: '#/components/messages/UserSignedUp<caret>'
            components:
              messages:
                UserSignedUp:
                  payload:
                    type: object
            """.trimIndent(),
        )
        val sourceElement = myFixture.file.findElementAt(myFixture.caretOffset)
        val targets = AsyncApiGotoDeclarationHandler()
            .getGotoDeclarationTargets(sourceElement, myFixture.caretOffset, myFixture.editor)
        assertNotNull("expected the handler to return a non-null target array", targets)
        assertTrue("expected at least one target", targets!!.isNotEmpty())
        val keyValue = targets[0].parent as YAMLKeyValue
        assertEquals("UserSignedUp", keyValue.keyText)
    }

    /**
     * Same as [testGotoDeclarationHandlerResolvesTheSameRefDirectly] but
     * with the caret at the very START of the `$ref` value (right after
     * the opening quote), not mid-text -- this is the exact caret
     * position from the live sandbox reproduction where Ctrl+Click still
     * failed even after the handler was first added, catching a real
     * PSI-depth bug (`sourceElement.parent as? YAMLScalar` assumed a
     * fixed single-parent hop that only held for a caret placed inside
     * the value's own leaf token, not at the scalar's own boundary).
     */
    fun testGotoDeclarationHandlerResolvesWithCaretAtStartOfRefValue() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            channels:
              user/signedup:
                subscribe:
                  message:
                    ${'$'}ref: '<caret>#/components/messages/UserSignedUp'
            components:
              messages:
                UserSignedUp:
                  payload:
                    type: object
            """.trimIndent(),
        )
        val sourceElement = myFixture.file.findElementAt(myFixture.caretOffset)
        val targets = AsyncApiGotoDeclarationHandler()
            .getGotoDeclarationTargets(sourceElement, myFixture.caretOffset, myFixture.editor)
        assertNotNull("expected the handler to return a non-null target array", targets)
        assertTrue("expected at least one target", targets!!.isNotEmpty())
        val keyValue = targets[0].parent as YAMLKeyValue
        assertEquals("UserSignedUp", keyValue.keyText)
    }

    /**
     * Reproduces the ACTUAL live failure, root-caused 2026-08-13 via
     * `runIde` logging: the user clicked on the literal `$ref` KEY text
     * (not the value after it), and `getGotoDeclarationTargets` received
     * a `sourceElement` whose PSI ancestors never include the value
     * `YAMLScalar` at all -- `PsiTreeUtil.getParentOfType(sourceElement,
     * YAMLScalar::class.java, false)` (the first fix attempt) still
     * returned null for this exact case, because walking up from the KEY
     * token never reaches the VALUE scalar; they're siblings under the
     * same `YAMLKeyValue`, not ancestor/descendant. The real fix walks up
     * to the enclosing `YAMLKeyValue` first, then reads `.value`
     * explicitly, so it doesn't matter which side of the `:` the caret
     * lands on.
     */
    fun testGotoDeclarationHandlerResolvesWithCaretOnTheDollarRefKeyItself() {
        myFixture.configureByText(
            "asyncapi.yaml",
            """
            asyncapi: 2.6.0
            channels:
              user/signedup:
                subscribe:
                  message:
                    ${'$'}re<caret>f: '#/components/messages/UserSignedUp'
            components:
              messages:
                UserSignedUp:
                  payload:
                    type: object
            """.trimIndent(),
        )
        val sourceElement = myFixture.file.findElementAt(myFixture.caretOffset)
        val targets = AsyncApiGotoDeclarationHandler()
            .getGotoDeclarationTargets(sourceElement, myFixture.caretOffset, myFixture.editor)
        assertNotNull("expected the handler to return a non-null target array", targets)
        assertTrue("expected at least one target", targets!!.isNotEmpty())
        val keyValue = targets[0].parent as YAMLKeyValue
        assertEquals("UserSignedUp", keyValue.keyText)
    }

    fun testGotoDeclarationHandlerReturnsNullOutsideARecognizedAsyncApiFile() {
        myFixture.configureByText(
            "plain.yaml",
            """
            ${'$'}ref: '#/definitions/User<caret>'
            definitions:
              User: {}
            """.trimIndent(),
        )
        val sourceElement = myFixture.file.findElementAt(myFixture.caretOffset)
        val targets = AsyncApiGotoDeclarationHandler()
            .getGotoDeclarationTargets(sourceElement, myFixture.caretOffset, myFixture.editor)
        assertTrue(
            "expected no targets outside a recognized AsyncAPI file",
            targets == null || targets.isEmpty(),
        )
    }
}

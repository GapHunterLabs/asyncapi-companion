package dev.gaphunter.asyncapicompanion.reference

import com.intellij.psi.PsiElement
import com.jetbrains.jsonSchema.extension.JsonSchemaGotoDeclarationSuppressor
import dev.gaphunter.asyncapicompanion.detection.AsyncApiDetector

/**
 * Suppresses the platform's own bundled JSON Schema go-to-declaration
 * (`com.jetbrains.jsonSchema.impl.JsonSchemaGotoDeclarationHandler`,
 * part of `com.intellij.modules.json`) on recognized AsyncAPI files.
 * Kept even though [AsyncApiGotoDeclarationHandler] is now the real
 * Ctrl+B mechanism (see that class's doc for why): still useful for
 * any other bundled-handler-driven UI (e.g. quick documentation
 * previews) that might consult this same extension point directly.
 * Unconditional here -- no license gate, this plugin is free.
 *
 * Investigation note (2026-08-13, see
 * `openapi_companion_ctrlclick_broken_with_trial` memory entry for the
 * full logged findings): live `runIde` logging proved this class works
 * correctly on its own -- `shouldSuppressGtd` is called on every real
 * Ctrl+B attempt and always returns `true` for a recognized AsyncAPI
 * file. The actual bug was a broken suppress-then-fallback hand-off
 * elsewhere in the platform, not here -- fixed by adding
 * [AsyncApiGotoDeclarationHandler] as the real navigation path.
 */
class AsyncApiJsonSchemaGtdSuppressor : JsonSchemaGotoDeclarationSuppressor {
    override fun shouldSuppressGtd(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        return AsyncApiDetector.isAsyncApiSpec(file.text)
    }
}

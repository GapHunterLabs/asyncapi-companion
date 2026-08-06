package dev.gaphunter.asyncapicompanion.reference

import com.intellij.psi.PsiElement
import com.jetbrains.jsonSchema.extension.JsonSchemaGotoDeclarationSuppressor
import dev.gaphunter.asyncapicompanion.detection.AsyncApiDetector

/**
 * Suppresses the platform's own bundled JSON Schema go-to-declaration
 * (`com.jetbrains.jsonSchema.impl.JsonSchemaGotoDeclarationHandler`,
 * part of `com.intellij.modules.json`) on recognized AsyncAPI files, so
 * this plugin's own reference classes get a chance to resolve instead
 * -- `GotoDeclarationHandler`s are consulted by the platform before
 * generic `PsiReference`-based resolution, so without this the bundled
 * handler wins first regardless of what this plugin contributes. Same
 * real, confirmed extension point openapi-companion already uses for
 * the identical problem (see that plugin's `KNOWN_ISSUES.md` Round 1).
 * Unconditional here -- no license gate, this plugin is free.
 */
class AsyncApiJsonSchemaGtdSuppressor : JsonSchemaGotoDeclarationSuppressor {
    override fun shouldSuppressGtd(element: PsiElement): Boolean {
        val file = element.containingFile ?: return false
        return AsyncApiDetector.isAsyncApiSpec(file.text)
    }
}

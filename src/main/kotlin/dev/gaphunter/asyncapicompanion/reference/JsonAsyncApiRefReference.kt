package dev.gaphunter.asyncapicompanion.reference

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import dev.gaphunter.asyncapicompanion.pointer.JsonPointer
import java.net.URLDecoder

/**
 * Resolves a `$ref` value inside a JSON-formatted AsyncAPI document
 * (`"#/components/messages/UserSignedUp"`, `"common.json"`,
 * `"./messages/user.json#/components/messages/UserSignedUp"`) purely
 * against local files -- [resolveLocalFile] only ever walks
 * [VfsUtilCore.findRelativeFile] relative to the referencing file's own
 * directory. There is no HTTP client anywhere in this plugin.
 *
 * Free, no license gate -- unlike openapi-companion's 100%-Paid model,
 * this catalog's evidence for AsyncAPI points at a FREEMIUM incumbent
 * whose pain is reliability (breaks across IDE version bumps, blank
 * preview bug), not missing premium features. The fix here is "just
 * works", not a paywalled capability.
 */
class JsonAsyncApiRefReference(element: JsonStringLiteral) :
    PsiReferenceBase<JsonStringLiteral>(element, ElementManipulators.getValueTextRange(element)) {

    override fun resolve(): PsiElement? {
        val refText = element.value
        val hashIndex = refText.indexOf('#')
        val filePart = if (hashIndex >= 0) refText.substring(0, hashIndex) else refText
        val pointerPart = if (hashIndex >= 0) refText.substring(hashIndex + 1) else ""

        val decodedPointer = try {
            URLDecoder.decode(pointerPart, "UTF-8")
        } catch (e: Exception) {
            pointerPart
        }

        if (filePart.isBlank()) {
            val targetFile = element.containingFile as? JsonFile ?: return null
            if (decodedPointer.isEmpty()) return targetFile.topLevelValue ?: targetFile
            val root = targetFile.topLevelValue ?: return null
            return JsonPointer.resolve(root, decodedPointer)
        }

        // The target of a $ref is resolved by its OWN real format, not
        // assumed to match the format of the file containing the $ref --
        // an AsyncAPI document written in JSON commonly references a
        // YAML-formatted shared component file, and vice versa.
        val targetFile = resolveLocalFile(filePart) ?: return null
        return CrossFormatRefTarget.resolve(targetFile, decodedPointer)
    }

    private fun resolveLocalFile(relativePath: String): PsiFile? {
        val currentVirtualFile = element.containingFile?.originalFile?.virtualFile ?: return null
        val baseDir = currentVirtualFile.parent ?: return null
        val targetVirtualFile = VfsUtilCore.findRelativeFile(relativePath, baseDir) ?: return null
        return PsiManager.getInstance(element.project).findFile(targetVirtualFile)
    }

    override fun getVariants(): Array<Any> = emptyArray()
}

package dev.gaphunter.asyncapicompanion.reference

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiReferenceBase
import dev.gaphunter.asyncapicompanion.pointer.YamlPointer
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLScalar
import java.net.URLDecoder

/**
 * YAML counterpart of [JsonAsyncApiRefReference] -- same resolution
 * rules (local files only, RFC 6901 pointer via [YamlPointer]).
 * `getTextValue()` already gives the decoded scalar content regardless
 * of quoting style.
 */
class YamlAsyncApiRefReference(element: YAMLScalar) :
    PsiReferenceBase<YAMLScalar>(element, ElementManipulators.getValueTextRange(element)) {

    override fun resolve(): PsiElement? {
        val refText = element.textValue
        val hashIndex = refText.indexOf('#')
        val filePart = if (hashIndex >= 0) refText.substring(0, hashIndex) else refText
        val pointerPart = if (hashIndex >= 0) refText.substring(hashIndex + 1) else ""

        val decodedPointer = try {
            URLDecoder.decode(pointerPart, "UTF-8")
        } catch (e: Exception) {
            pointerPart
        }

        if (filePart.isBlank()) {
            val targetFile = element.containingFile as? YAMLFile ?: return null
            val root = targetFile.documents.firstOrNull()?.topLevelValue
            if (decodedPointer.isEmpty()) return root ?: targetFile
            if (root == null) return null
            return YamlPointer.resolve(root, decodedPointer)
        }

        // Same cross-format resolution as JsonAsyncApiRefReference: the
        // target of a $ref is resolved by its OWN real format, since a
        // YAML AsyncAPI document commonly references a JSON-formatted
        // shared component file.
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

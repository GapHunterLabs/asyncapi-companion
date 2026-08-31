package dev.gaphunter.asyncapicompanion.highlighting

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import dev.gaphunter.asyncapicompanion.detection.UnescapedChannelSlashHint
import dev.gaphunter.asyncapicompanion.reference.JsonAsyncApiRefReference
import dev.gaphunter.asyncapicompanion.reference.JsonAsyncApiRefUtil
import dev.gaphunter.asyncapicompanion.reference.YamlAsyncApiRefReference
import dev.gaphunter.asyncapicompanion.reference.YamlAsyncApiRefUtil
import dev.gaphunter.asyncapicompanion.review.ReviewPrompt
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * Flags a `$ref` value that fails to resolve, in both JSON- and
 * YAML-formatted AsyncAPI documents -- real, visible feedback for a
 * broken reference, on top of the go-to-definition navigation the two
 * reference contributors already provide for valid ones. Handles both
 * languages in one class (registered twice in plugin.xml, once per
 * language) rather than duplicating this dispatch logic.
 */
class AsyncApiRefAnnotator : Annotator {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val refText: String
        val resolves: Boolean
        when (element) {
            is JsonStringLiteral -> {
                JsonAsyncApiRefUtil.asRefProperty(element) ?: return
                refText = element.value
                resolves = JsonAsyncApiRefReference(element).resolve() != null
            }
            is YAMLScalar -> {
                YamlAsyncApiRefUtil.asRefKeyValue(element) ?: return
                refText = element.textValue
                resolves = YamlAsyncApiRefReference(element).resolve() != null
            }
            else -> return
        }

        if (!resolves) {
            val hint = if (refText.startsWith("#")) {
                UnescapedChannelSlashHint.describe(refText.removePrefix("#"), channelNamesOf(element.containingFile))
            } else {
                null
            }
            val message = if (hint != null) "Cannot resolve reference '$refText' -- $hint" else "Cannot resolve reference '$refText'"
            holder.newAnnotation(HighlightSeverity.WARNING, message)
                .range(element.textRange)
                .create()
            val file = element.containingFile
            val lineNumber = file.viewProvider.document?.getLineNumber(element.textRange.startOffset)?.plus(1) ?: 0
            ReviewPrompt.recordHit(file.project, "${file.virtualFile?.path}:$lineNumber:$refText")
        }
    }

    /** The literal key names declared directly under this document's own top-level `channels:` object, or empty if there is none. */
    private fun channelNamesOf(file: com.intellij.psi.PsiFile): Set<String> = when (file) {
        is JsonFile -> (file.topLevelValue as? JsonObject)
            ?.findProperty("channels")?.value
            ?.let { it as? JsonObject }
            ?.propertyList?.mapNotNull { it.name }?.toSet()
            ?: emptySet()
        is YAMLFile -> (file.documents.firstOrNull()?.topLevelValue as? YAMLMapping)
            ?.getKeyValueByKey("channels")?.value
            ?.let { it as? YAMLMapping }
            ?.keyValues?.mapNotNull { it.keyText }?.toSet()
            ?: emptySet()
        else -> emptySet()
    }
}

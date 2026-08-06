package dev.gaphunter.asyncapicompanion.highlighting

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import dev.gaphunter.asyncapicompanion.reference.JsonAsyncApiRefReference
import dev.gaphunter.asyncapicompanion.reference.JsonAsyncApiRefUtil
import dev.gaphunter.asyncapicompanion.reference.YamlAsyncApiRefReference
import dev.gaphunter.asyncapicompanion.reference.YamlAsyncApiRefUtil
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
            holder.newAnnotation(HighlightSeverity.WARNING, "Cannot resolve reference '$refText'")
                .range(element.textRange)
                .create()
        }
    }
}

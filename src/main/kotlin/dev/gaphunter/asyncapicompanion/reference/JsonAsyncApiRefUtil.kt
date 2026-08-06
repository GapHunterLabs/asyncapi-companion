package dev.gaphunter.asyncapicompanion.reference

import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import dev.gaphunter.asyncapicompanion.detection.AsyncApiDetector

private const val REF_KEYWORD = "\$ref"

/** Shared "is this string literal a `$ref` VALUE inside a recognized
 * AsyncAPI JSON file" check -- used by both
 * [JsonAsyncApiRefReferenceContributor] (to decide whether to offer
 * navigation) and the annotator (to decide whether to check
 * resolution), so the two never drift out of sync. */
object JsonAsyncApiRefUtil {
    fun asRefProperty(literal: JsonStringLiteral): JsonProperty? {
        if (literal.isPropertyName) return null
        val property = literal.parent as? JsonProperty ?: return null
        if (property.name != REF_KEYWORD) return null
        if (property.value !== literal) return null
        if (!AsyncApiDetector.isAsyncApiSpec(literal.containingFile.text)) return null
        return property
    }
}

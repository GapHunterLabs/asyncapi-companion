package dev.gaphunter.asyncapicompanion.reference

import dev.gaphunter.asyncapicompanion.detection.AsyncApiDetector
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar

private const val REF_KEYWORD = "\$ref"

/** YAML counterpart of [JsonAsyncApiRefUtil]. */
object YamlAsyncApiRefUtil {
    fun asRefKeyValue(scalar: YAMLScalar): YAMLKeyValue? {
        val keyValue = scalar.parent as? YAMLKeyValue ?: return null
        if (keyValue.keyText != REF_KEYWORD) return null
        if (keyValue.value !== scalar) return null
        if (!AsyncApiDetector.isAsyncApiSpec(scalar.containingFile.text)) return null
        return keyValue
    }
}

package dev.gaphunter.asyncapicompanion.reference

import com.intellij.json.psi.JsonFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import dev.gaphunter.asyncapicompanion.pointer.JsonPointer
import dev.gaphunter.asyncapicompanion.pointer.YamlPointer
import org.jetbrains.yaml.psi.YAMLFile

/**
 * Resolves an RFC 6901 pointer fragment against a target file of EITHER
 * format, regardless of which format the referencing `$ref` itself lives
 * in. A real AsyncAPI project commonly keeps a JSON-formatted root spec
 * with YAML-formatted shared component files (or vice versa) -- 0.2.x
 * only ever resolved a same-format target file, a documented scope cut.
 * [JsonPointer] and [YamlPointer] already do the real per-format PSI
 * walk; this only decides which one applies to the resolved [PsiFile].
 */
object CrossFormatRefTarget {
    fun resolve(targetFile: PsiFile, pointer: String): PsiElement? = when (targetFile) {
        is JsonFile -> targetFile.topLevelValue?.let { JsonPointer.resolve(it, pointer) }
            ?: if (pointer.isEmpty()) targetFile else null
        is YAMLFile -> targetFile.documents.firstOrNull()?.topLevelValue?.let { YamlPointer.resolve(it, pointer) }
            ?: if (pointer.isEmpty()) targetFile else null
        else -> null
    }
}

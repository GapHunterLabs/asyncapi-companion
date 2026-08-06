package dev.gaphunter.asyncapicompanion.reference

import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.util.ProcessingContext
import org.jetbrains.yaml.psi.YAMLScalar

/**
 * YAML counterpart of [JsonAsyncApiRefReferenceContributor] -- wires
 * [YamlAsyncApiRefReference] up as go-to-definition on `$ref` scalar
 * values inside YAML-formatted AsyncAPI documents.
 */
class YamlAsyncApiRefReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(YAMLScalar::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val scalar = element as? YAMLScalar ?: return PsiReference.EMPTY_ARRAY
                    YamlAsyncApiRefUtil.asRefKeyValue(scalar) ?: return PsiReference.EMPTY_ARRAY
                    return arrayOf(YamlAsyncApiRefReference(scalar))
                }
            },
        )
    }
}

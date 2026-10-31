package io.hamlook.aetheria.idea

import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext

/** Offers project compatibility types when the user is completing a mapped raw class name. */
class CompatClassCompletionContributor : CompletionContributor() {
    init {
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            object : CompletionProvider<CompletionParameters>() {
                override fun addCompletions(
                    parameters: CompletionParameters,
                    context: ProcessingContext,
                    result: CompletionResultSet
                ) {
                    val language = parameters.position.containingFile.language.id.lowercase()
                    if (language != "java" && language != "kotlin") return

                    val prefix = result.prefixMatcher.prefix
                    if (prefix.isEmpty() || !prefix.first().isJavaIdentifierStart()) return
                    if (!prefix.all { it.isJavaIdentifierPart() }) return

                    CompatClassMappings.entries
                        .filter { language in it.languages && result.prefixMatcher.prefixMatches(it.rawClass) }
                        .forEach { mapping ->
                            result.addElement(
                                LookupElementBuilder.create(mapping.compatClass)
                                    .withPresentableText(mapping.compatClass.substringAfterLast('.'))
                                    .withLookupString(mapping.rawClass)
                                    .withTailText("  replaces ${mapping.rawClass}", true)
                                    .withTypeText("Aetheria compatibility class", true)
                            )
                        }
                }
            }
        )
    }
}

private data class CompatClassMapping(val rawClass: String, val compatClass: String, val languages: Set<String>)

private object CompatClassMappings {
    val entries: List<CompatClassMapping> by lazy {
        val json = CompatClassCompletionContributor::class.java.classLoader
            .getResourceAsStream("compat-class-mappings.json")
            ?.bufferedReader()
            ?.use { it.readText() }
            ?: return@lazy emptyList()

        Regex("""\{([^{}]*)}""")
            .findAll(json)
            .mapNotNull { block ->
                val fields = block.groupValues[1]
                val raw = Regex(""""rawClass"\s*:\s*"([^"]+)"""").find(fields)?.groupValues?.get(1) ?: return@mapNotNull null
                val compat = Regex(""""compatClass"\s*:\s*"([^"]+)"""").find(fields)?.groupValues?.get(1) ?: return@mapNotNull null
                val languages = Regex(""""languages"\s*:\s*\[([^]]*)]""")
                    .find(fields)?.groupValues?.get(1)?.let { languageList ->
                        Regex(""""([^"]+)"""").findAll(languageList).map { it.groupValues[1] }.toSet()
                    }.orEmpty()
                CompatClassMapping(raw, compat, languages)
            }
            .toList()
    }
}

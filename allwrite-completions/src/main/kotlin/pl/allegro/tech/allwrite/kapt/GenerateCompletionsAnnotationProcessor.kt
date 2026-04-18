package pl.allegro.tech.allwrite.kapt

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.FileSpec
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.ksp.generated.module
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.kapt.generators.CompletionGenerator
import pl.allegro.tech.allwrite.kapt.util.injectAll
import java.io.File
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.lang.model.util.Elements
import javax.tools.Diagnostic.Kind.ERROR

@AutoService(Processor::class)
@SupportedAnnotationTypes("pl.allegro.tech.allwrite.kapt.GenerateCompletions")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class GenerateCompletionsAnnotationProcessor :
    AbstractProcessor(),
    KoinComponent {

    private val recipeSource: RecipeSource by inject()
    private val completionGenerators: List<CompletionGenerator> by injectAll()

    private val elements: Elements by lazy { processingEnv.elementUtils }
    private val kaptGeneratedDir: String by lazy { processingEnv.options["kapt.kotlin.generated"]!! }

    init {
        startKoin { modules(CompletionsModule().module) }
    }

    override fun process(annotations: MutableSet<out TypeElement>?, roundEnv: RoundEnvironment?): Boolean {
        try {
            val annotatedFunction = roundEnv?.getElementsAnnotatedWith(GenerateCompletions::class.java)?.firstOrNull()

            if (annotatedFunction != null) {
                processAnnotatedFunction(annotatedFunction)
            }
        } catch (e: Exception) {
            processingEnv.messager.printMessage(ERROR, "failed: ${e.message}")
            throw e
        }
        return true
    }

    private fun processAnnotatedFunction(annotatedFunction: Element) {
        val packageName = elements.getPackageOf(annotatedFunction).qualifiedName.toString()

        FileSpec.builder(packageName, "Completions")
            .apply(::generateFileContent)
            .build()
            .writeTo(File(kaptGeneratedDir))
    }

    private fun generateFileContent(fileSpec: FileSpec.Builder) {
        val recipeDescriptors = recipeSource.findAll()

        completionGenerators
            .map { generator -> generator.generate(recipeDescriptors) }
            .forEach(fileSpec::addProperty)
    }
}

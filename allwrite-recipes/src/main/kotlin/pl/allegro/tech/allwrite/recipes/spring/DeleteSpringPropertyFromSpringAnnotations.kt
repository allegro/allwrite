package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.TreeVisitor
import org.openrewrite.internal.NameCaseConvention
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType.FullyQualified
import pl.allegro.tech.allwrite.recipes.AllwriteRecipe
import pl.allegro.tech.allwrite.recipes.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.java.AnnotationArgument
import pl.allegro.tech.allwrite.recipes.java.MultiValueAnnotationArgument
import pl.allegro.tech.allwrite.recipes.java.PrimitiveAnnotationArgument
import pl.allegro.tech.allwrite.recipes.java.getArgument
import pl.allegro.tech.allwrite.recipes.java.getValueArgument

public class DeleteSpringPropertyFromSpringAnnotations(
    @Option(displayName = "Property key", description = "The property key to remove. Supports glob", example = "management.metrics.binders.*.enabled")
    private val propertyName: String,
) : AllwriteRecipe(
    displayName = "Remove Spring property from @SpringBootTest and @TestPropertySource annotations",
    visibility = INTERNAL
) {
    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        return Visitor(propertyName)
    }

    internal class Visitor(private val propertyNameGlob: String) : JavaIsoVisitor<ExecutionContext>() {

        override fun visitAnnotation(annotation: J.Annotation, p: ExecutionContext): J.Annotation {
            var annotation = annotation
            val annotationType = annotation.annotationType.type
            if (annotationType is FullyQualified) {
                annotation = when (annotationType.fullyQualifiedName) {
                    "org.springframework.boot.test.context.SpringBootTest" ->
                        SpringBootTestVisitor(propertyNameGlob, springBootTestArgumentSelector).visitAnnotation(annotation, p)
                    "org.springframework.test.context.TestPropertySource" ->
                        SpringBootTestVisitor(propertyNameGlob, testPropertySourceArgumentSelector).visitAnnotation(annotation, p)
                    else -> annotation
                }
            }
            return super.visitAnnotation(annotation, p)
        }
    }

    internal class SpringBootTestVisitor(
        private val propertyNameGlob: String,
        private val annotationArgumentSelector: (J.Annotation) -> AnnotationArgument?
    ) : JavaIsoVisitor<ExecutionContext>() {

        override fun visitAnnotation(a: J.Annotation, p: ExecutionContext): J.Annotation {
            val argument = annotationArgumentSelector(a) ?: return a
            return when (argument) {
                is PrimitiveAnnotationArgument -> {
                    if (argument.literal.matchesRequestedProperty()) argument.replaceWith(null) else a
                }
                is MultiValueAnnotationArgument -> {
                    val properties = argument.elements
                    val newProperties = processElements(properties)
                    if (properties != newProperties) argument.replaceElements(newProperties) else a
                }
                else -> a
            }
        }

        private fun processElements(exprs: List<Expression>?): List<Expression>? = exprs
            ?.toMutableList()
            ?.also { elements ->
                elements.withIndex()
                    .filter { (_, init) -> (init as? J.Literal).matchesRequestedProperty() }
                    .reversed()
                    .forEach { (idx, _) ->
                        val prefixToTransfer = (elements[idx] as? J.Literal)?.prefix
                        elements.removeAt(idx)
                        if (idx == 0 && elements.isNotEmpty() && prefixToTransfer != null) {
                            if (elements[0] is J.Literal) {
                                elements[0] = elements[0].withPrefix(prefixToTransfer)
                            }
                        }
                    }
            }
            ?.ifEmpty { null }

        private fun J.Literal?.matchesRequestedProperty(): Boolean {
            val propertyName = (this?.value as? String)?.split('=', ':')?.first()
            return propertyName != null && NameCaseConvention.matchesGlobRelaxedBinding(propertyName, propertyNameGlob)
        }
    }

    private companion object {
        val springBootTestArgumentSelector: (J.Annotation) -> AnnotationArgument? = { a -> a.getValueArgument() ?: a.getArgument("properties") }
        val testPropertySourceArgumentSelector: (J.Annotation) -> AnnotationArgument? = { a -> a.getArgument("properties") }
    }
}

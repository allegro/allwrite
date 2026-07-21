package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.Option
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.TypeUtils
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility

public open class RemoveAnnotatedMethod(
    @Option(
        displayName = "Return Type",
        description = "Fully qualified class name of the method return type to match (e.g. com.example.MyBean).",
        required = true,
    )
    public val returnType: String? = null,

    @Option(
        displayName = "Annotation Name",
        description = "Fully qualified class name of the annotation the method must have (e.g. org.springframework.context.annotation.Bean).",
        required = true,
    )
    public val annotationName: String? = null,

    @Option(
        displayName = "Allowed Body Calls",
        description = """Simple method names that may appear in the method body without blocking removal.
            Any call not in this set causes the method to be treated as complex and skipped""",
        required = false,
    )
    public val allowedBodyCalls: Set<String> = emptySet(),
) : AllwriteRecipe(
    description = """
        Removes methods annotated with a specific annotation. Example:
        BEFORE:
        public class JacksonConfig {
            @Bean
            public BlackbirdModule blackbirdModule() {
                return new BlackbirdModule();
            }
        }

        AFTER:
        public class JacksonConfig {
        }
        .
    """.trimIndent(),
    visibility = RecipeVisibility.INTERNAL,
),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = emptyList()

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> {
        return object : JavaVisitor<ExecutionContext>() {

            override fun visit(tree: Tree?, ctx: ExecutionContext): J? {
                val targetAnnotationName = annotationName ?: return super.visit(tree, ctx)
                val targetReturnType = returnType ?: return super.visit(tree, ctx)

                if (tree !is J.MethodDeclaration) {
                    return super.visit(tree, ctx)
                }

                if (hasParameters(tree)) {
                    return super.visit(tree, ctx)
                }

                if (!hasOnlyGivenAnnotation(tree, cursor, targetAnnotationName)) {
                    return super.visit(tree, ctx)
                }

                val resolvedReturnType = tree.methodType?.returnType ?: return super.visit(tree, ctx)

                if (!TypeUtils.isOfClassType(resolvedReturnType, targetReturnType)) {
                    return super.visit(tree, ctx)
                }

                if (isComplexMethod(tree, ctx)) {
                    return super.visit(tree, ctx)
                }

                maybeRemoveImport(targetReturnType)

                val annotationCount = countInvocation(this.cursor, ctx, targetAnnotationName)
                if (annotationCount <= 1) {
                    maybeRemoveImport(targetAnnotationName)
                }

                // null => remove method
                return null
            }
        }
    }

    private fun hasOnlyGivenAnnotation(tree: J.MethodDeclaration, cursor: Cursor, targetAnnotationName: String): Boolean {
        val annotations = tree.leadingAnnotations
        return annotations.size == 1 &&
            annotations.all { ann ->
                val annType = ann.annotationType.printTrimmed(cursor)
                annType == targetAnnotationName || targetAnnotationName.endsWith(".$annType")
            }
    }

    private fun hasParameters(method: J.MethodDeclaration): Boolean = method.parameters.any { it !is J.Empty }

    private fun isComplexMethod(tree: J.MethodDeclaration, ctx: ExecutionContext): Boolean {
        var isComplex = false
        if (tree.body != null) {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitMethodInvocation(method: J.MethodInvocation, ctx: ExecutionContext): J {
                    if (!allowedBodyCalls.contains(method.simpleName)) {
                        isComplex = true
                    }
                    return super.visitMethodInvocation(method, ctx)
                }
            }.visit(tree.body, ctx)
        }
        return isComplex
    }

    private fun countInvocation(originalCursor: Cursor, executionContext: ExecutionContext, targetAnnotationName: String): Int {
        val shortAnnotationName = targetAnnotationName.substringAfterLast('.')

        val compilationUnit = originalCursor.firstEnclosing(SourceFile::class.java)
        var annotationCount = 0

        if (compilationUnit != null) {
            object : JavaVisitor<ExecutionContext>() {
                override fun visitAnnotation(annotation: J.Annotation, ctx: ExecutionContext): J {
                    val annType = annotation.annotationType.printTrimmed(cursor)
                    if (annType == targetAnnotationName || annType == shortAnnotationName) {
                        annotationCount++
                    }
                    return super.visitAnnotation(annotation, ctx)
                }
            }.visit(compilationUnit, executionContext)
        }

        return annotationCount
    }
}

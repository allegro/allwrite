package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.JavaVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space.SINGLE_SPACE
import org.openrewrite.kotlin.format.MinimumViableSpacingVisitor
import pl.allegro.tech.allwrite.recipes.java.annotation
import pl.allegro.tech.allwrite.recipes.spring.util.ANNOTATION_NAMED
import pl.allegro.tech.allwrite.recipes.spring.util.ANNOTATION_QUALIFIER
import pl.allegro.tech.allwrite.recipes.spring.util.ANNOTATION_RESOURCE
import pl.allegro.tech.allwrite.recipes.spring.util.Variable
import pl.allegro.tech.allwrite.recipes.spring.util.hasQualifyingAnnotation
import pl.allegro.tech.allwrite.recipes.util.isKotlin
import pl.allegro.tech.allwrite.recipes.util.mapFirst

/**
 * Add a qualified name to the variable via spring's `@Qualifier` annotation. If another qualifying annotation is
 * already present (either `@Named` or `@Resource`), change its argument to use the `newName` input option.
 */
internal class QualifyVariable(
    val variable: Variable,
    val newName: String
) : JavaIsoVisitor<ExecutionContext>() {

    override fun visitVariableDeclarations(tree: J.VariableDeclarations, p: ExecutionContext): J.VariableDeclarations {
        if (variable.variable !in tree.variables || !tree.isValidTargetToQualify()) return super.visitVariableDeclarations(tree, p)

        val qualifiedName = variable.qualifiedName
        if (variable.declaration.hasQualifyingAnnotation()) {
            // if there is a qualifier annotation, change its value
            val originalPrefixes = tree.leadingAnnotations.associate { it.id to it.prefix }
            var jtree = AddOrUpdateAnnotationAttribute(annotationType = ANNOTATION_QUALIFIER, oldAttributeValue = qualifiedName, attributeValue = newName)
                .visitor.visit(tree, p, cursor.parent!!) as J
            jtree = AddOrUpdateAnnotationAttribute(annotationType = ANNOTATION_RESOURCE, oldAttributeValue = qualifiedName, attributeName = "name", attributeValue = newName)
                .visitor.visit(jtree, p, cursor.parent!!) as J
            jtree = AddOrUpdateAnnotationAttribute(annotationType = ANNOTATION_NAMED, oldAttributeValue = qualifiedName, attributeValue = newName)
                .visitor.visit(jtree, p, cursor.parent!!) as J
            val result = jtree as J.VariableDeclarations
            return result.withLeadingAnnotations(result.leadingAnnotations.map { ann ->
                originalPrefixes[ann.id]?.let { prefix -> ann.withPrefix(prefix) } ?: ann
            })
        } else {
            // otherwise add a qualifier annotation
            return AddQualifierAnnotation(variable.variable, newName).visit(tree, p, cursor.parent!!) as J.VariableDeclarations
        }
    }

    /**
     * Checks if the variable is either a constructor/method argument or a class field. Other variable declaration options
     * (e.g. local variables) are not suitable targets for qualification
     */
    private fun J.VariableDeclarations.isValidTargetToQualify(): Boolean {
        val parent = cursor.dropParentUntil { it is J.MethodDeclaration || it is J.ClassDeclaration }.getValue<Any>()
        return when (parent) {
            is J.MethodDeclaration -> this in parent.parameters
            is J.ClassDeclaration -> this in parent.body.statements
            else -> false
        }
    }
}

// TODO: this recipe does not add an annotation import for now, it should be handled by the caller having access to J.CompilationUnit
private class AddQualifierAnnotation(val variable: J.VariableDeclarations.NamedVariable, val qualifier: String) : JavaVisitor<ExecutionContext>() {

    override fun visitVariableDeclarations(variableDeclaration: J.VariableDeclarations, p: ExecutionContext): J.VariableDeclarations {
        if (variable !in variableDeclaration.variables) return variableDeclaration

        val newAnnotations = variableDeclaration.leadingAnnotations + annotation(JavaType.ShallowClass.build(ANNOTATION_QUALIFIER), qualifier)

        var result = variableDeclaration.withLeadingAnnotations(newAnnotations)
        val formattedAnnotations = autoFormat(result, p).leadingAnnotations
        result = result.withLeadingAnnotations(formattedAnnotations)
        result = MinimumViableSpacingVisitor<ExecutionContext>().visit(result, p) as J.VariableDeclarations
        result = adjustSpaces(result)

        return result
    }

    // As we are avoiding `autoFormat` in JavaTemplate, we need to do some cleanup ourselves:
    // e.g. adding a space between variable and annotation
    private fun adjustSpaces(v: J.VariableDeclarations): J.VariableDeclarations {
        if (cursor.isKotlin()) {
           return adjustKotlinSpaces(v)
        } else if (v.typeExpression != null) {
            val typePrefix = v.typeExpression!!.prefix
            if (typePrefix == null || typePrefix.isEmpty) {
                return v.withTypeExpression(v.typeExpression!!.withPrefix(SINGLE_SPACE))
            }
        }
        return v
    }

    private fun adjustKotlinSpaces(v: J.VariableDeclarations): J.VariableDeclarations {
        // in Kotlin we may first want to add an annotation in front of modifiers
        if (v.modifiers.isNotEmpty()) {
            val firstModifier = v.modifiers[0]
            if (firstModifier.prefix == null || firstModifier.prefix.isEmpty) {
                return v.withModifiers(v.modifiers.mapFirst { it.withPrefix(SINGLE_SPACE) })
            }
        } else if (v.variables.isNotEmpty()) {
            // if there are no modifiers, we are adding a space before the variable name
            val variable = v.variables.first()
            if (variable.prefix == null || variable.prefix.isEmpty) {
                return v.withVariables(v.variables.mapFirst { it.withPrefix(SINGLE_SPACE) })
            }
        }
        return v
    }
}

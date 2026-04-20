package pl.allegro.tech.allwrite.recipes.java

import org.openrewrite.Cursor
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.tree.Comment
import org.openrewrite.java.tree.Expression
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.kotlin.tree.K
import pl.allegro.tech.allwrite.recipes.util.KotlinListLiteral
import pl.allegro.tech.allwrite.recipes.util.isKotlin
import pl.allegro.tech.allwrite.recipes.util.mapFirst
import pl.allegro.tech.allwrite.recipes.util.mapLast
import pl.allegro.tech.allwrite.recipes.util.replace

internal const val DEFAULT_ANNOTATION_ARGUMENT_NAME: String = "value"

internal fun J.Annotation.arguments() = arguments ?: emptyList<Expression>()

/**
 * Represents an argument of the specific [annotation] instance. Mutating operations ([replaceWith], [remove])
 * return a modified copy of the original annotation.
 */
internal sealed class AnnotationArgument {

    abstract val name: String

    /**
     * Original LST element representing the argument - usually [J.Assignment], but default argument may be different.
     * [source] is used to identify the original argument in [J.Annotation.arguments] to perform [replaceWith] and [remove]
     */
    abstract val source: Expression
    abstract val annotation: J.Annotation

    /**
     * Replaces the original argument with [replacement] and returns a modified copy of the annotation
     */
    open fun replaceWith(replacement: Expression?): J.Annotation {
        if (replacement == null) return remove()

        var replacement = replacement
        if (source is J.Assignment &&
            replacement !is J.Assignment &&
            (annotation.arguments().size > 1 || name != DEFAULT_ANNOTATION_ARGUMENT_NAME)
        ) {
            replacement = assignment(name, replacement.withPrefix(Space.SINGLE_SPACE))
        }

        return annotation.withArguments(annotation.arguments?.replace(source, replacement))
    }

    /**
     * Removes the argument and returns a modified copy of the annotation
     */
    open fun remove(): J.Annotation {
        val newArguments = annotation.arguments?.minus(source)?.mapFirst {
            if (it.prefix == Space.SINGLE_SPACE) it.withPrefix(Space.EMPTY) else it
        }
        val removingLastArgument = annotation.arguments?.last() == source
        return if (removingLastArgument) {
            annotation.withArguments(newArguments).preserveLastArgumentFormatting()
        } else {
            annotation.withArguments(newArguments)
        }
    }

    private fun J.Annotation.preserveLastArgumentFormatting(): J.Annotation {
        val lastArgumentSuffix = annotation.padding.arguments!!.padding.elements.last().after
        val lastCommentEndedWithNewLine = lastArgumentSuffix.comments.lastOrNull()?.suffix?.endsWith("\n") == true
        val lastArgumentEndedWithNewLine = lastCommentEndedWithNewLine || lastArgumentSuffix.whitespace.endsWith("\n")
        if (lastArgumentEndedWithNewLine) {
            val arguments = this.padding.arguments!!.padding.elements
            val newArguments = arguments.mapLast { newLastArgument ->
                val isFirstCommentOfLastArgumentSameLineAsNewLastArgument = !source.prefix.whitespace.startsWith('\n')
                val newLastArgumentComment = if (isFirstCommentOfLastArgumentSameLineAsNewLastArgument) {
                    source.prefix.comments.firstOrNull()?.withSuffix<Comment>("\n")
                } else {
                    null
                }
                val suffix = if (newLastArgumentComment == null) {
                    newLastArgument.after.withWhitespace("\n")
                } else {
                    val newLastArgumentSuffixWhitespace = source.prefix.whitespace
                    newLastArgument.after.withWhitespace(newLastArgumentSuffixWhitespace)
                        .withComments(listOf(newLastArgumentComment))
                }
                newLastArgument.withAfter(suffix)
            }
            return this.padding.withArguments(this.padding.arguments!!.padding.withElements(newArguments))
        }
        return this
    }

    open fun unwrapString(): String? = null

    fun isDefault(): Boolean = name == DEFAULT_ANNOTATION_ARGUMENT_NAME
}

/**
 * Represents an argument with an array value. Implementations may cover language-specific cases,
 * but this common class to be used in generic code.
 */
internal sealed class MultiValueAnnotationArgument : AnnotationArgument() {

    abstract val elements: List<Expression>
    abstract val elementType: JavaType

    /**
     * Replaces the elements, but keeps the original container with its formatting
     */
    open fun replaceElements(replacement: List<Expression>?): J.Annotation {
        if (replacement.isNullOrEmpty()) return remove()
        val source = this.source
        val sourceValue = if (source is J.Assignment) source.assignment else source

        val newElements = when (sourceValue) {
            is J.NewArray -> sourceValue.withInitializer(replacement)
            is K.ListLiteral -> sourceValue.withElements(replacement)
            is G.ListLiteral -> sourceValue.withElements(replacement)
            else -> annotation
        }

        val newArgument = if (source is J.Assignment) source.withAssignment(newElements) else newElements
        return replaceWith(newArgument)
    }
}

internal data class PrimitiveAnnotationArgument(
    override val name: String,
    val literal: J.Literal,
    override val source: Expression,
    override val annotation: J.Annotation,
) : AnnotationArgument() {

    override fun unwrapString(): String? = literal.value?.toString()
}

internal data class ClassAnnotationArgument(
    override val name: String,
    val type: JavaType,
    override val source: Expression,
    override val annotation: J.Annotation,
) : AnnotationArgument()

internal data class EnumAnnotationArgument(
    override val name: String,
    val type: JavaType,
    override val source: Expression,
    override val annotation: J.Annotation,
) : AnnotationArgument()

internal data class AnnotationAnnotationArgument(
    override val name: String,
    override val source: Expression,
    override val annotation: J.Annotation,
) : AnnotationArgument()

internal data class ListAnnotationArgument(
    override val name: String,
    override val elements: List<Expression>,
    override val elementType: JavaType,
    override val source: Expression,
    override val annotation: J.Annotation,
) : MultiValueAnnotationArgument()

/**
 * Represents Kotlin's varargs for the unnamed default argument
 */
internal data class VarArgAnnotationArgument(
    override val name: String,
    override val elements: List<Expression>,
    override val elementType: JavaType,
    override val source: Expression,
    override val annotation: J.Annotation,
) : MultiValueAnnotationArgument() {

    override fun replaceWith(replacement: Expression?): J.Annotation {
        if (replacement == null) return annotation.withArguments(null)
        val assignment = replacement as? J.Assignment ?: assignment(name, replacement)
        return annotation.withArguments(listOf(assignment))
    }

    override fun replaceElements(replacement: List<Expression>?): J.Annotation = annotation.withArguments(replacement)
}

internal data class ReferenceAnnotationArgument(
    override val name: String,
    val type: JavaType?,
    val sourceType: JavaType?,
    override val source: Expression,
    override val annotation: J.Annotation,
) : AnnotationArgument()

internal data class CalculatedAnnotationArgument(
    override val name: String,
    override val source: Expression,
    override val annotation: J.Annotation,
) : AnnotationArgument()

internal fun J.Annotation.getArgument(name: String): AnnotationArgument? {
    val arguments = this.arguments ?: return null

    val isVararg = arguments.size > 1 && arguments.none { it is J.Assignment }
    if (isVararg) {
        return VarArgAnnotationArgument(name, arguments, arguments.firstOrNull()?.type ?: JavaType.Unknown.getInstance(), this, this)
    }

    val argument = arguments().firstOrNull {
        (it is J.Assignment && it.name() == name) || (name == DEFAULT_ANNOTATION_ARGUMENT_NAME && it !is J.Assignment)
    } ?: return null

    val argumentValue: Expression = if (argument is J.Assignment) argument.assignment else argument

    return when (argumentValue) {
        is J.Literal -> PrimitiveAnnotationArgument(name, argumentValue, argument, this)
        is J.NewArray -> ListAnnotationArgument(name, argumentValue.initializer ?: emptyList(), argumentValue.type!!, argument, this)
        is K.ListLiteral -> ListAnnotationArgument(name, argumentValue.elements, argumentValue.type!!, argument, this)
        is G.ListLiteral -> ListAnnotationArgument(name, argumentValue.elements, argumentValue.type!!, argument, this)
        is K.StringTemplate -> CalculatedAnnotationArgument(name, argument, this)
        is G.GString -> CalculatedAnnotationArgument(name, argument, this)
        is J.Identifier -> ReferenceAnnotationArgument(name, argumentValue.type, null, argument, this)
        is J.Annotation -> AnnotationAnnotationArgument(name, argumentValue, this)
        is J.FieldAccess -> {
            val type = argumentValue.type
            when {
                type == null -> null
                TypeUtils.asFullyQualified(type)?.kind == JavaType.FullyQualified.Kind.Enum -> EnumAnnotationArgument(name, type, argument, this)
                else -> ReferenceAnnotationArgument(name, type, argumentValue.target.type, argument, this)
            }
        }

        // kotlin's ::class
        is J.MemberReference -> {
            val type = argumentValue.type
            type
                ?.takeIf { TypeUtils.asFullyQualified(type)?.fullyQualifiedName == "kotlin.reflect.KClass" }
                ?.let { ClassAnnotationArgument(name, type, argument, this) }
        }

        // kotlin allows arrayOf method invocation in annotation values
        // in this case it does not have methodType info
        is J.MethodInvocation -> {
            val type = argumentValue.arguments.firstOrNull()?.type ?: JavaType.Unknown.getInstance()
            argumentValue
                .takeIf { it.name.simpleName == "arrayOf" && it.methodType == null }
                ?.let { ListAnnotationArgument(name, it.arguments, type, argument, this) }
        }
        else -> null
    }
}

internal fun J.Annotation.getValueArgument(): AnnotationArgument? = getArgument(DEFAULT_ANNOTATION_ARGUMENT_NAME)

/**
 * Adds an argument to the annotation. [value] will be wrapped in the [J.Assignment] instance
 * with [name] being the left hand side of the assignment.
 *
 * [Cursor] is needed to cover language-specific cases
 */
internal fun J.Annotation.addArgument(name: String, value: Expression, cursor: Cursor): J.Annotation {
    val existing = getArgument(name)
    if (existing != null) return existing.replaceWith(value)

    val newPrefix = if (arguments.isNullOrEmpty()) value.prefix else value.prefix.withWhitespace(value.prefix.whitespace + " ")
    val newArg = assignment(name, value.withPrefix(Space.SINGLE_SPACE)).withPrefix(newPrefix)
    val args = arguments ?: emptyList<Expression>()

    val valueArgument = getValueArgument()
    if (valueArgument == null || valueArgument.source is J.Assignment) return withArguments(args + newArg)

    return withArguments(args + newArg).fixValueArgument(cursor)
}

// After change in the arguments, specific changes may be needed for the default `value` argument
// If it was the only argument and had no name (no J.Assignment), the name should be added -
// in this case for Kotlin, we should also wrap array arguments in array literals
private fun J.Annotation.fixValueArgument(cursor: Cursor): J.Annotation {
    val valueArgument = getValueArgument()
    if (valueArgument == null || valueArgument.source is J.Assignment) return this

    val args = arguments ?: emptyList<Expression>()
    var newValueArgument = valueArgument.source

    val isArray =
        (type as? JavaType.Class)?.methods?.filter { m -> m.name == DEFAULT_ANNOTATION_ARGUMENT_NAME }?.map { it.returnType }?.any { it is JavaType.Array }
            ?: return this
    if (isArray && valueArgument !is MultiValueAnnotationArgument && cursor.isKotlin()) {
        newValueArgument = KotlinListLiteral(type = valueArgument.source.type, elements = listOf(valueArgument.source))
    }

    val needsAssignment = args.size > 1 && valueArgument.source !is J.Assignment
    if (needsAssignment) {
        val newPrefix = newValueArgument.prefix.withWhitespace(newValueArgument.prefix.whitespace + " ")
        newValueArgument = assignment(DEFAULT_ANNOTATION_ARGUMENT_NAME, newValueArgument.withPrefix(newPrefix))
    }

    return valueArgument.replaceWith(newValueArgument)
}

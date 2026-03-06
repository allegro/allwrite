package pl.allegro.tech.allwrite.recipes.kotlin

import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType

internal class KotlinPropertyMatcher(
    methodSignature: String
) {
    private val ownerClassName: String
    private val fieldName: String

    init {
        val matchResult = """(\S+) get(\S+)\(\)""".toRegex().matchEntire(methodSignature)
        ownerClassName = matchResult?.groupValues?.get(1) ?: ""
        fieldName = matchResult?.groupValues?.get(2)?.replaceFirstChar(Char::lowercase) ?: ""
    }

    fun matches(fieldAccess: J.FieldAccess): Boolean {
        val fieldOwnerClassName = (fieldAccess.name.fieldType?.owner as? JavaType.Class)?.fullyQualifiedName
        val fieldSimpleName = fieldAccess.simpleName

        return ownerClassName == fieldOwnerClassName && fieldName == fieldSimpleName
    }
}

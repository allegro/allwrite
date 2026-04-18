package pl.allegro.tech.allwrite.recipes.gradle

import org.openrewrite.Cursor
import org.openrewrite.ExecutionContext
import org.openrewrite.Parser
import org.openrewrite.gradle.GradleParser
import org.openrewrite.groovy.GroovyIsoVisitor
import org.openrewrite.groovy.tree.G
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.Space
import org.openrewrite.java.tree.Statement
import org.openrewrite.kotlin.KotlinIsoVisitor
import org.openrewrite.kotlin.tree.K
import pl.allegro.tech.allwrite.recipes.java.valueAsString
import pl.allegro.tech.allwrite.recipes.util.DelegatingJVisitor
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Paths
import kotlin.jvm.optionals.getOrNull

private const val DEPENDENCIES = "dependencies"
private val MESSAGE_TARGET = "${AddVersionCatalogDependencyReference::class.java.name}_target"
private val GRADLE_PARSER: GradleParser = GradleParser.builder().build()

/**
 * Adds a reference to the version catalog (e.g. `implementation(libs.com.example.test)` to gradle dependencies).
 * If there is a dependency with such [configuration] and [library] coordinates already, it will not be added.
 */
internal class AddVersionCatalogDependencyReference(
    val configuration: String,
    val library: Library,
    val versionCatalogAlias: String? = null,
) : DelegatingJVisitor(
    javaVisitor = JavaIsoVisitor(),
    kotlinVisitor = KotlinAddVersionCatalogDependencyReference(configuration, library, versionCatalogAlias),
    groovyVisitor = GroovyAddVersionCatalogDependencyReference(configuration, library, versionCatalogAlias),
)

private class KotlinAddVersionCatalogDependencyReference(
    val configuration: String,
    val library: Library,
    val versionCatalogAlias: String? = null,
) : KotlinIsoVisitor<ExecutionContext>() {

    override fun visitCompilationUnit(cu: K.CompilationUnit, p: ExecutionContext): K.CompilationUnit {
        if (!cu.sourcePath.toString().endsWith(".gradle.kts")) return cu
        val result = super.visitCompilationUnit(withDependencies(cu, p), p)
        return result
    }

    // adds `dependencies` block when it's missing
    private fun withDependencies(cu: K.CompilationUnit, ctx: ExecutionContext): K.CompilationUnit {
        val block = cu.statements.firstOrNull() as? J.Block ?: return cu
        val target = block.statements.filterIsInstance<J.MethodInvocation>().firstOrNull { it.simpleName == DEPENDENCIES }
        if (target != null) {
            cursor.putMessage(MESSAGE_TARGET, target)
            return cu
        }

        var dependencies = GRADLE_PARSER.parseInputs(
            listOf(Parser.Input(Paths.get("build.gradle.kts")) { ByteArrayInputStream("$DEPENDENCIES {}".toByteArray(StandardCharsets.UTF_8)) }),
            null,
            ctx,
        )
            .findFirst().getOrNull()
            ?.let { it as? K.CompilationUnit }
            ?.statements?.firstOrNull()
            ?.let { it as? J.Block }
            ?.statements?.firstOrNull()
            ?.let { it as? J.MethodInvocation }
            ?: return cu

        if (block.statements.isNotEmpty()) {
            dependencies = dependencies.withPrefix(dependencies.prefix.let { it.withWhitespace("\n" + it.whitespace) })
        }

        cursor.putMessage(MESSAGE_TARGET, dependencies)
        val newValues = block.statements + dependencies
        return cu.withStatements(mutableListOf<Statement>(block.withStatements(newValues)))
    }

    override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation =
        if (cursor.getNearestMessage<Any>(MESSAGE_TARGET) == method) {
            DependenciesVisitor(configuration, library, versionCatalogAlias).visit(method, p, cursor.parent!!) as J.MethodInvocation
        } else {
            super.visitMethodInvocation(method, p)
        }
}

private class GroovyAddVersionCatalogDependencyReference(
    val configuration: String,
    val library: Library,
    val versionCatalogAlias: String? = null,
) : GroovyIsoVisitor<ExecutionContext>() {

    override fun visitCompilationUnit(cu: G.CompilationUnit, p: ExecutionContext): G.CompilationUnit {
        if (!cu.sourcePath.toString().endsWith(".gradle")) return cu
        return super.visitCompilationUnit(withDependencies(cu), p)
    }

    private fun withDependencies(cu: G.CompilationUnit): G.CompilationUnit {
        val existing = cu.statements.filterIsInstance<J.MethodInvocation>().firstOrNull { it.simpleName == DEPENDENCIES }
        if (existing != null) {
            cursor.putMessage(MESSAGE_TARGET, existing)
            return cu
        }

        var dependencies = GRADLE_PARSER.parse("$DEPENDENCIES {\n}\n").findFirst().getOrNull()
            ?.let { it as? G.CompilationUnit }
            ?.statements?.firstOrNull()
            ?: return cu

        if (cu.statements.isNotEmpty()) {
            dependencies = dependencies.withPrefix(dependencies.prefix.let { it.withWhitespace("\n" + it.whitespace) })
        }

        cursor.putMessage(MESSAGE_TARGET, dependencies)
        return cu.withStatements(cu.statements + dependencies)
    }

    override fun visitStatement(statement: Statement, p: ExecutionContext) =
        if (cursor.getNearestMessage<Any>(MESSAGE_TARGET) == statement) {
            DependenciesVisitor(configuration, library, versionCatalogAlias).visit(statement, p, cursor.parent!!) as Statement
        } else {
            super.visitStatement(statement, p)
        }
}

private class DependenciesVisitor(
    val configuration: String,
    val library: Library,
    val versionCatalogAlias: String? = null,
) : JavaIsoVisitor<ExecutionContext>() {

    override fun visitBlock(block: J.Block, p: ExecutionContext): J.Block {
        var result = super.visitBlock(block, p)

        val methodInvocation = cursor.firstEnclosing(J.MethodInvocation::class.java)
        val lambda = cursor.firstEnclosing(J.Lambda::class.java)
        if (methodInvocation?.simpleName == DEPENDENCIES && lambda?.body == block && methodInvocation.arguments.any { it == lambda }) {
            val existing = block.statements.mapNotNull {
                when (it) {
                    is J.MethodInvocation -> it
                    is J.Return -> it.expression as? J.MethodInvocation
                    else -> null
                }
            }
                .firstOrNull { it.declaresTargetLibrary() }

            if (existing != null) return result

            val dependencyCoordinates = getDependencyCoordinates()
            var dependency = GRADLE_PARSER.parse(p, "$DEPENDENCIES {\n$configuration($dependencyCoordinates)\n}").findFirst().getOrNull()
                ?.let { it as? G.CompilationUnit }
                ?.let { autoFormat(it, p, Cursor(cursor, result)) }
                ?.statements?.firstOrNull()
                ?.let { it as? J.MethodInvocation }
                ?.arguments?.firstOrNull()
                ?.let { it as? J.Lambda }
                ?.body
                ?.let { it as? J.Block }
                ?.statements?.firstOrNull()
                as? J.Return ?: return result

            if (result.statements.isNotEmpty()) {
                val previousStatement = result.statements.last()
                val indent = previousStatement.prefix.whitespace.substringAfterLast('\n')
                dependency = dependency.withPrefix(dependency.prefix.withWhitespace("\n$indent"))
            } else if (result.prefix.whitespace.contains('\n')) {
                dependency = dependency.withPrefix(dependency.prefix.let { it.withWhitespace(it.whitespace.substringAfter('\n')) })
            }

            result = result.withStatements(result.statements.toMutableList() + dependency).withEnd(Space.format("\n"))
        }

        return result
    }

    private fun getDependencyCoordinates(): String {
        if (versionCatalogAlias == null) {
            return buildString {
                append('"')
                append(library.group)
                append(":").append(library.name)
                if (library.version != null) append(":").append(library.version)
                append('"')
            }
        }

        val targetName = versionCatalogAlias.toVersionCatalogReference()
        return "libs.$targetName"
    }

    private fun J.MethodInvocation.declaresTargetLibrary(): Boolean =
        declaresDependencyAsVersionCatalogReference() ||
            declaresDependencyAsGroovyMap() ||
            declaresDependencyAsKotlinNamedArgs() ||
            declaredDependencyInStringNotation()

    private fun J.MethodInvocation.declaresDependencyAsVersionCatalogReference(): Boolean =
        versionCatalogAlias != null &&
            (arguments.firstOrNull() as? J.FieldAccess)?.toString()?.contains(versionCatalogAlias.toVersionCatalogReference()) ?: false

    private fun J.MethodInvocation.declaredDependencyInStringNotation(): Boolean {
        val str = (arguments.firstOrNull() as? J.Literal)?.value?.toString() ?: return false
        return str.contains("${library.group}:${library.name}")
    }

    private fun J.MethodInvocation.declaresDependencyAsGroovyMap(): Boolean {
        val entriesByName = arguments.filterIsInstance<G.MapEntry>()
            .filter { it.key is J.Literal }
            .associateBy { (it.key as J.Literal).value?.toString() }

        val group = entriesByName[VERSION_CATALOG_PARAM_GROUP]?.valueAsString()
        val name = entriesByName[VERSION_CATALOG_PARAM_NAME]?.valueAsString()

        return group == library.group && name == library.name
    }

    private fun J.MethodInvocation.declaresDependencyAsKotlinNamedArgs(): Boolean {
        val assignmentsByName = arguments
            .filterIsInstance<J.Assignment>()
            .filter { it.variable is J.Identifier }
            .associateBy { (it.variable as J.Identifier).simpleName }

        val group = assignmentsByName[VERSION_CATALOG_PARAM_GROUP]?.valueAsString()
        val name = assignmentsByName[VERSION_CATALOG_PARAM_NAME]?.valueAsString()

        return group == library.group && name == library.name
    }

    private fun G.MapEntry.valueAsString(): String? = (value as? J.Literal)?.value.toString()
}

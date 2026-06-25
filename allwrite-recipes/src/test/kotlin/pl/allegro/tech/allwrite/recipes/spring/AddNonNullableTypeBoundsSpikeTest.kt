package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.java.tree.TypeTree
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.marker.TypeReferencePrefix
import org.openrewrite.kotlin.tree.K
import org.openrewrite.marker.Markers
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation
import pl.allegro.tech.allwrite.recipes.kotlin
import java.util.UUID

class AddNonNullableTypeBoundsSpikeTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(SpikeAddAnyBoundRecipe())
            .typeValidationOptions(TypeValidation.none())
    }

    @Test
    fun `spike - inspect AST of type parameter without bound`() {
        val parser = KotlinParser.builder().build()
        val ctx = InMemoryExecutionContext { t -> t.printStackTrace() }
        val sources = parser.parse(
            ctx,
            """
            interface Repo<T, ID> : org.springframework.data.repository.CrudRepository<T, ID>
            """.trimIndent()
        )
        val cu = sources.toList().first() as K.CompilationUnit
        println("=== CompilationUnit statements ===")
        cu.statements.forEach { stmt ->
            println("  statement class: ${stmt::class.java.name}")
        }
        val classDecl = cu.statements.filterIsInstance<J.ClassDeclaration>().firstOrNull()
        if (classDecl == null) {
            println("  No J.ClassDeclaration found! Trying K.ClassDeclaration...")
            cu.statements.forEach { stmt ->
                println("  stmt: $stmt")
                println("  stmt interfaces: ${stmt::class.java.interfaces.map { it.name }}")
            }
            return
        }
        val typeParams = classDecl.typeParameters ?: emptyList()

        println("=== Type parameters for 'interface Repo<T, ID>' ===")
        typeParams.forEach { tp ->
            println("  TypeParameter: name=${tp.name}, bounds=${tp.bounds}, class=${tp::class.simpleName}")
            println("    name type: ${tp.name::class.simpleName}")
            println("    bounds: ${tp.bounds}")
            tp.bounds?.forEach { bound ->
                println("      bound: ${bound}, class=${bound::class.simpleName}")
                if (bound is J.Identifier) {
                    println("        identifier: simpleName=${bound.simpleName}, type=${bound.type}")
                }
            }
        }

        println("\n=== Implements ===")
        println("  implements: ${classDecl.implements}")
        println("  extends: ${classDecl.extends}")
        classDecl.implements?.forEach { impl ->
            println("  impl class: ${impl::class.java.name}")
            if (impl is J.ParameterizedType) {
                val clazz = impl.clazz
                println("    clazz class: ${clazz::class.java.name}")
                println("    clazz: ${clazz}")
                if (clazz is J.FieldAccess) {
                    println("    fieldAccess name: ${clazz.name}")
                    println("    fieldAccess target: ${clazz.target}")
                    println("    fieldAccess simpleName: ${clazz.simpleName}")
                }
                if (clazz is J.Identifier) {
                    println("    identifier simpleName: ${clazz.simpleName}")
                }
            }
        }
    }

    @Test
    fun `spike - inspect AST of type parameter with Any bound`() {
        val parser = KotlinParser.builder().build()
        val ctx = InMemoryExecutionContext { t -> t.printStackTrace() }
        val sources = parser.parse(
            ctx,
            """
            interface Repo<T : Any, ID : Any> : org.springframework.data.repository.CrudRepository<T, ID>
            """.trimIndent()
        )
        val cu = sources.toList().first() as K.CompilationUnit
        val classDecl = cu.statements.first() as J.ClassDeclaration
        val typeParams = classDecl.typeParameters ?: emptyList()

        println("=== Type parameters for 'interface Repo<T : Any, ID : Any>' ===")
        typeParams.forEach { tp ->
            println("  TypeParameter: name=${tp.name}, bounds=${tp.bounds}, class=${tp::class.simpleName}")
            println("    name type: ${tp.name::class.simpleName}")
            println("    bounds size: ${tp.bounds?.size}")
            println("    padding.bounds: ${tp.padding.bounds}")
            println("    padding.bounds.before: ${tp.padding.bounds?.before}")
            println("    padding.bounds.markers: ${tp.padding.bounds?.markers}")
            tp.bounds?.forEach { bound ->
                println("      bound: ${bound}, class=${bound::class.simpleName}")
                if (bound is J.Identifier) {
                    println("        identifier: simpleName=${bound.simpleName}, type=${bound.type}, prefix='${bound.prefix}'")
                }
            }
        }
    }

    @Test
    fun `spike - attempt to add Any bound and print result`() {
        rewriteRun(
            kotlin(
                before = """
                interface Repo<T, ID> : org.springframework.data.repository.CrudRepository<T, ID>
                """.trimIndent(),
                after = """
                interface Repo<T : Any, ID : Any> : org.springframework.data.repository.CrudRepository<T, ID>
                """.trimIndent(),
            )
        )
    }

    @Test
    fun `spike - no change when bounds already present`() {
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                interface Repo<T : Any, ID : Any> : org.springframework.data.repository.CrudRepository<T, ID>
                """.trimIndent(),
            )
        )
    }
}

class SpikeAddAnyBoundRecipe : Recipe() {
    override fun getDisplayName(): String = "Spike: Add Any bound"
    override fun getDescription(): String = "Spike test for adding : Any bounds to type parameters."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : JavaIsoVisitor<ExecutionContext>() {

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                var cd = super.visitClassDeclaration(classDecl, p)

                val supertypes = (cd.implements ?: emptyList()) + listOfNotNull(cd.extends)
                val isRepository = supertypes.any { supertype ->
                    when (supertype) {
                        is J.ParameterizedType -> {
                            val clazz = supertype.clazz
                            when (clazz) {
                                is J.FieldAccess -> clazz.simpleName == "CrudRepository"
                                is J.Identifier -> clazz.simpleName == "CrudRepository"
                                else -> false
                            }
                        }

                        is J.Identifier -> supertype.simpleName == "CrudRepository"
                        else -> false
                    }
                }

                if (!isRepository) return cd

                val typeParams = cd.typeParameters ?: return cd
                var changed = false
                val newTypeParams = typeParams.map { tp ->
                    if (tp.bounds.isNullOrEmpty()) {
                        changed = true
                        addAnyBound(tp)
                    } else {
                        tp
                    }
                }

                if (changed) {
                    cd = cd.withTypeParameters(newTypeParams)
                }

                return cd
            }

            private fun addAnyBound(tp: J.TypeParameter): J.TypeParameter {
                val anyIdentifier = J.Identifier(
                    UUID.randomUUID(),
                    Space.format(" "),
                    Markers.EMPTY,
                    emptyList(),
                    "Any",
                    JavaType.buildType("kotlin.Any"),
                    null
                )
                var newTp = tp.withBounds(listOf<TypeTree>(anyIdentifier))
                val boundsContainer = newTp.padding.bounds!!.withBefore(Space.format(" "))
                newTp = newTp.padding.withBounds(boundsContainer)
                newTp = newTp.withMarkers(
                    newTp.markers.add(TypeReferencePrefix(UUID.randomUUID(), Space.EMPTY))
                )
                return newTp
            }
        }
}

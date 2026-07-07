package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.Space
import org.openrewrite.java.tree.TypeTree
import org.openrewrite.java.tree.TypeUtils
import org.openrewrite.kotlin.marker.TypeReferencePrefix
import org.openrewrite.marker.Markers
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility
import pl.allegro.tech.allwrite.recipes.util.isKotlin
import java.util.UUID

public class AddNonNullableTypeBoundsToSpringRepositories :
    AllwriteRecipe(
        displayName = "Add non-nullable type bounds to Spring Data repository type parameters (Kotlin)",
        description = "Adds `: Any` upper bounds to type parameters of Kotlin classes/interfaces extending Spring Data " +
            "repository interfaces, as required by Spring Framework 7 / Spring Boot 4 JSpecify nullability annotations. " +
            "This recipe only applies to Kotlin source files.\n\n" +
            "Example:\n" +
            "  Before: `interface UserRepository<T, ID> : CrudRepository<T, ID>`\n" +
            "  After:  `interface UserRepository<T : Any, ID : Any> : CrudRepository<T, ID>`.",
        visibility = RecipeVisibility.INTERNAL,
    ),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = listOf("spring-data-commons-3", "spring-data-jpa-3", "spring-data-mongodb-4")

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : JavaIsoVisitor<ExecutionContext>() {

            override fun visitCompilationUnit(cu: J.CompilationUnit, p: ExecutionContext): J.CompilationUnit {
                if (!cursor.isKotlin()) return cu
                return super.visitCompilationUnit(cu, p)
            }

            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                var cd = super.visitClassDeclaration(classDecl, p)
                val typeParams = cd.typeParameters ?: return cd

                val repoTypeParamNames = findTypeParamsPassedToRepository(cd)
                if (repoTypeParamNames.isEmpty()) return cd

                var changed = false
                val newTypeParams = typeParams.map {
                    val simpleName = (it.name as? J.Identifier)?.simpleName
                    if (it.bounds.isNullOrEmpty() && simpleName in repoTypeParamNames) {
                        changed = true
                        addAnyBound(it)
                    } else {
                        it
                    }
                }

                if (changed) {
                    cd = cd.withTypeParameters(newTypeParams)
                }

                return cd
            }

            private fun findTypeParamsPassedToRepository(classDecl: J.ClassDeclaration): Set<String> {
                val result = mutableSetOf<String>()
                val supertypes = (classDecl.implements ?: emptyList()) + listOfNotNull(classDecl.extends)

                for (supertype in supertypes) {
                    if (supertype !is J.ParameterizedType) continue
                    if (!isSpringDataRepository(supertype)) continue

                    supertype.typeParameters?.forEach {
                        val name = when (it) {
                            is J.Identifier -> it.simpleName
                            else -> null
                        }
                        if (name != null) {
                            result.add(name)
                        }
                    }
                }

                return result
            }

            private fun isSpringDataRepository(parameterizedType: J.ParameterizedType): Boolean {
                val type = resolveType(parameterizedType.clazz)
                return SPRING_DATA_REPOSITORY_FQNS.any { TypeUtils.isAssignableTo(it, type) }
            }

            private fun resolveType(clazz: J): JavaType? =
                when (clazz) {
                    is J.Identifier -> clazz.type
                    is J.FieldAccess -> clazz.type
                    else -> null
                }

            private fun addAnyBound(tp: J.TypeParameter): J.TypeParameter {
                val anyIdentifier = J.Identifier(
                    UUID.randomUUID(),
                    Space.format(" "),
                    Markers.EMPTY,
                    emptyList(),
                    "Any",
                    JavaType.buildType("kotlin.Any"),
                    null,
                )
                var newTp = tp.withBounds(listOf<TypeTree>(anyIdentifier))
                newTp = newTp.padding.withBounds(newTp.padding.bounds!!.withBefore(Space.format(" ")))
                newTp = newTp.withMarkers(
                    newTp.markers.add(TypeReferencePrefix(UUID.randomUUID(), Space.EMPTY)),
                )
                return newTp
            }
        }

    private companion object {

        val SPRING_DATA_REPOSITORY_FQNS = listOf(
            "org.springframework.data.repository.Repository",
            "org.springframework.data.repository.CrudRepository",
            "org.springframework.data.repository.ListCrudRepository",
            "org.springframework.data.repository.PagingAndSortingRepository",
            "org.springframework.data.repository.ListPagingAndSortingRepository",
            "org.springframework.data.jpa.repository.JpaRepository",
            "org.springframework.data.mongodb.repository.MongoRepository",
            "org.springframework.data.mongodb.repository.ReactiveMongoRepository",
            "org.springframework.data.repository.reactive.ReactiveCrudRepository",
            "org.springframework.data.repository.reactive.ReactiveSortingRepository",
        )
    }
}

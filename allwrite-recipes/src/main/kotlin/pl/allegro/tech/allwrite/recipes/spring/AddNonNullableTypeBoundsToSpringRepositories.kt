package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.TreeVisitor
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility

public class AddNonNullableTypeBoundsToSpringRepositories :
    AllwriteRecipe(
        displayName = "Add non-nullable type bounds to Spring Data repository type parameters (Kotlin)",
        description = "Adds `: Any` upper bounds to type parameters of Kotlin classes/interfaces extending Spring Data " +
                "repository interfaces, as required by Spring Framework 7 / Spring Boot 4 JSpecify nullability annotations. " +
                "This recipe only applies to Kotlin source files.",
        visibility = RecipeVisibility.INTERNAL,
    ),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> =
        listOf("spring-data-commons-3")

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : JavaIsoVisitor<ExecutionContext>() {
            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J.ClassDeclaration {
                return super.visitClassDeclaration(classDecl, p)
            }
        }
}

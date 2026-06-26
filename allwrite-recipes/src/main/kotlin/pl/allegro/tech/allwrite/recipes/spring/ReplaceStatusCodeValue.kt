package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.ExecutionContext
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility

public class ReplaceStatusCodeValue :
    AllwriteRecipe(
        displayName = "Replace ResponseEntity.getStatusCodeValue() with getStatusCode().value()",
        description = "Replaces deprecated `ResponseEntity.getStatusCodeValue()` / `.statusCodeValue` " +
            "with `getStatusCode().value()` / `.statusCode.value()` as required by Spring Framework 7 / Spring Boot 4.",
        visibility = RecipeVisibility.INTERNAL,
    ),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = listOf("spring-web-6")

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> = object : TreeVisitor<Tree, ExecutionContext>() {}
}

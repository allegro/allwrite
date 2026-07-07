package pl.allegro.tech.allwrite.recipes.spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource

class SpringBoot4_0Test : RewriteTest {

    private val upstreamEnvironment = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.spring")
        .build()

    private lateinit var recipe: Recipe

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<RecipeSource> {
                        FakeRecipeSource(
                            listOf(upstreamEnvironment.activateRecipes("org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0")),
                        )
                    }
                },
            )
        }
        recipe = object : Recipe() {
            override fun getDisplayName() = "SpringBoot4_0"
            override fun getDescription() = "Runs all recipes from SpringBoot4_0."
            override fun getRecipeList(): List<Recipe> = SpringBoot4_0().recipeList
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    override fun defaults(spec: RecipeSpec) {
        val ctx = InMemoryExecutionContext()
        spec
            .recipe(recipe)
            .parser(
                KotlinParser.builder().classpathFromResources(ctx, "spring-data-commons-3", "spring-data-jpa-3", "spring-data-mongodb-4"),
            )
    }

    @Test
    fun `SpringBoot4_0 recipe list contains custom recipes`() {
        val recipeNames = recipe.recipeList.map { it::class.simpleName }
        assertThat(recipeNames).contains("AddNonNullableTypeBoundsToSpringRepositories")
    }

    @Test
    fun `should add Any bounds to Spring Data repository type parameters`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.data.repository.CrudRepository

                interface UserRepository<T, ID> : CrudRepository<T, ID>
                """.trimIndent(),
                after = """
                import org.springframework.data.repository.CrudRepository

                interface UserRepository<T : Any, ID : Any> : CrudRepository<T, ID>
                """.trimIndent(),
            ),
        )
    }
}

package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe
import org.openrewrite.text.FindAndReplace
import pl.allegro.tech.allwrite.recipes.gradle.ChangeGradleDependency
import pl.allegro.tech.allwrite.recipes.java.ChangeType

public class SpringBoot4_0 : IsolatedSpringRecipe(from = "3.5", to = "4.0") {

    override fun getRecipeList(): List<Recipe> =
        super.getRecipeList() +
            AddNonNullableTypeBoundsToSpringRepositories() +
            ReplaceStatusCodeValue() +
            changeSpringBoot4MongoProperties() +
            relocateTestClientTypes() +
            addSpringHttpTestClientDependencies() +
            upgradeGroovyToV5() +
            upgradeTestcontainersToV2()

    private fun changeSpringBoot4MongoProperties(): List<Recipe> =
        listOf(
            FindAndReplace("spring.data.mongodb.uri", "spring.mongodb.uri", false, true, false, false, null, false),
            FindAndReplace("spring.data.mongodb.database", "spring.mongodb.database", false, true, false, false, null, false),
        )

    private fun upgradeGroovyToV5(): List<ChangeGradleDependency> =
        listOf(
            ChangeGradleDependency(
                oldGroupId = "org.apache.groovy",
                oldArtifactId = "*",
                newGroupId = "org.apache.groovy",
                newArtifactId = "",
                newVersion = "5.0.7",
            ),
            ChangeGradleDependency(
                oldGroupId = "org.spockframework",
                oldArtifactId = "*",
                newGroupId = "org.spockframework",
                newArtifactId = "",
                newVersion = "2.4-groovy-5.0",
            ),
        )

    private fun relocateTestClientTypes(): List<Recipe> =
        listOf(
            ChangeType(
                "org.springframework.boot.test.web.reactive.server.WebTestClientBuilderCustomizer",
                "org.springframework.boot.webtestclient.autoconfigure.WebTestClientBuilderCustomizer",
                false,
            ),
            ChangeType(
                "org.springframework.boot.test.web.client.TestRestTemplate",
                "org.springframework.boot.resttestclient.TestRestTemplate",
                false,
            ),
        )

    private fun addSpringHttpTestClientDependencies(): List<Recipe> =
        listOf(
            PreconditionsAwareAddDependency(
                displayName = "Add WebTestClient dependency",
                description = "Adds the Spring Boot WebTestClient test dependency when it is used.",
                requiredClasspath = listOf("spring-test-6"),
                requiredTypes = listOf(
                    "org.springframework.test.web.reactive.server.WebTestClient",
                    "org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient",
                    "org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient",
                ),
                configuration = "testImplementation",
                groupId = "org.springframework.boot",
                artifactId = "spring-boot-webtestclient",
            ),
            PreconditionsAwareAddDependency(
                displayName = "Add REST test client dependency",
                description = "Adds the Spring Boot REST test client dependency when TestRestTemplate is used.",
                requiredClasspath = listOf("spring-boot-test-3"),
                requiredTypes = listOf("org.springframework.boot.test.web.client.TestRestTemplate"),
                configuration = "testImplementation",
                groupId = "org.springframework.boot",
                artifactId = "spring-boot-resttestclient",
            ),
            PreconditionsAwareAddDependency(
                displayName = "Add Rest Assured Spring Web Test Client dependency",
                description = "Adds io.rest-assured:spring-web-test-client when io.rest-assured:rest-assured is present.",
                requiredDependencies = listOf("io.rest-assured:rest-assured"),
                configuration = "testImplementation",
                groupId = "io.rest-assured",
                artifactId = "spring-web-test-client",
            ),
        )
}

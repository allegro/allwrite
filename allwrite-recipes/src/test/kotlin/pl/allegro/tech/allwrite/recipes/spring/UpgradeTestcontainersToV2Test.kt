package pl.allegro.tech.allwrite.recipes.spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import pl.allegro.tech.allwrite.recipes.gradle.ChangeGradleDependency

class UpgradeTestcontainersToV2Test {

    @Test
    fun `should construct Testcontainers dependency recipes`() {
        // given
        val recipes = upgradeTestcontainersToV2()

        // expect
        assertThat(recipes)
            .isNotEmpty
            .allSatisfy { recipe ->
                assertThat(recipe).isInstanceOf(ChangeGradleDependency::class.java)

                val options = recipe.descriptor.options.associate { option -> option.name to option.value }
                assertThat(options["oldGroupId"]).isEqualTo("org.testcontainers")
                assertThat(options["newArtifactId"]).isEqualTo("testcontainers-${options["oldArtifactId"]}")
            }
    }
}

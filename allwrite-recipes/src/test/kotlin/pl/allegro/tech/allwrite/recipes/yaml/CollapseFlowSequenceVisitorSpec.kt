package pl.allegro.tech.allwrite.recipes.yaml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.test.RewriteTest
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.visitor.CollapseFlowSequenceVisitor

class CollapseFlowSequenceVisitorSpec : RewriteTest, YamlTest {

    @Test
    fun `should collapse flow sequence when original is collapsed`() {
        val (original, beforeVisit) = parse("seq: [ 1, 2 ]", "seq: [1,\n2]")

        val visitor = CollapseFlowSequenceVisitor(original)

        // when
        val result = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents

        // then
        assertThat(result.toYamlString()).isEqualTo(original.toYamlString())
    }

    @Test
    fun `should not collapse when original is expanded`() {
        val (original, beforeVisit) = parse("seq: [ 1,\n  2 ]", "seq: [ 1,\n  2 ]")

        val visitor = CollapseFlowSequenceVisitor(original)

        // when
        val result = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents

        // then
        assertThat(result.toYamlString()).isEqualTo(original.toYamlString())
    }

    @Test
    fun `should not expand when original is expanded`() {
        val (original, beforeVisit) = parse("seq: [ 1,\n  2 ]", "seq: [ 1, 2 ]")

        val visitor = CollapseFlowSequenceVisitor(original)

        // when
        val result = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents

        // then
        assertThat(result.toYamlString()).isEqualTo(beforeVisit.toYamlString())
    }

    @Test
    fun `should support multiple documents`() {
        val (original, beforeVisit) = parse(
            """
            seq: [1, 2]
            ---
            seq: [
              3,
              4
            ]
            """.trimIndent(),
            """
            seq: [
             1,
             2
            ]
            ---
            seq: [3, 4]
            """.trimIndent())

        val visitor = CollapseFlowSequenceVisitor(original)

        // when
        val result = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents

        // then
        assertThat(result.toYamlString()).isEqualTo(
            """
            seq: [1, 2]
            ---
            seq: [3, 4]
            """.trimIndent()
        )
    }
}

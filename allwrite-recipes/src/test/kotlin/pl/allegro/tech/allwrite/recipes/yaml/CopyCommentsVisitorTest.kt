package pl.allegro.tech.allwrite.recipes.yaml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.yaml.tree.Yaml
import pl.allegro.tech.allwrite.recipes.yaml.Mutators.mapBlock
import pl.allegro.tech.allwrite.recipes.yaml.Mutators.mapDocuments
import pl.allegro.tech.allwrite.recipes.yaml.Mutators.mapEntries
import pl.allegro.tech.allwrite.recipes.yaml.visitor.CopyCommentsVisitor

// In these tests we are only testing second property (counting from top), as the comment
// at the top of the document is attached to the Document in rewrite-yaml, not to the entry.
// Also, we are reusing the same Yaml.Documents object, as the implementation is based
// on the ID-equality of the documents, which is OK in scope of a single recipe when
// running OpenRewrite for real
class CopyCommentsVisitorTest : YamlTest {

    @Test
    fun `should copy comments from the original document`() {
        val yaml = """
            prop1: 1
            # comment
            prop2: 2
            """.trimIndent()
        val docs = parse(yaml)
        val visitor = CopyCommentsVisitor(docs)
        val docBeforeVisitor = docs.removeTopLevelEntriesPrefix()

        // when
        val resultYaml = visitor.visit(docBeforeVisitor, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(yaml)
    }

    @Test
    fun `should copy comments in the end of the line`() {
        val originalYaml = "prop1: 1 # single line"
        val beforeVisitYaml = "prop1: 1"
        val (original, beforeVisit) = parse(originalYaml, beforeVisitYaml)
        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(originalYaml)
    }

    @Test
    fun `should copy comments in the end of the line for reordered properties`() {
        val originalYaml =
            """
            prop1: 1
            prop2: 2 # 2
            """.trimIndent()
        val beforeVisitYaml =
            """
            prop2: 2
            prop1: 1
            """.trimIndent()
        val (original, beforeVisit) = parse(originalYaml, beforeVisitYaml)
        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(
            """
            prop2: 2 # 2
            prop1: 1
            """.trimIndent()
        )
    }

    @Test
    fun `should copy commented sequence entries`() {
        val yaml =
            """
            seq:
             - 1
             # - 2
             - 3
            """.trimIndent()
        val docs = parse(yaml)
        val visitor = CopyCommentsVisitor(docs)
        val docBeforeVisitor = docs.removeTopLevelEntriesPrefix()

        // when
        val resultYaml = visitor.visit(docBeforeVisitor, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(yaml)
    }

    @Test
    fun `should copy comments in the end of the line for block sequence`() {
        val originalYaml = """
            prop1:
              sequence:
                - attr: 1 # comm
            prop2:
              nested: 1
            prop3:
              - attr: 2 # comm2
        """.trimIndent()
        val beforeVisitYaml = """
            prop1:
              sequence:
                - attr: 1
            prop2:
              nested: 1
            prop3:
              - attr: 2
        """.trimIndent()
        val (original, beforeVisit) = parse(originalYaml, beforeVisitYaml)
        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(originalYaml)
    }

    @Test
    fun `should copy comments in the end of the line for flow sequence`() {
        val originalYaml = """
            prop1:
              sequence: [1] # comm
            prop2:
              nested: 1
            prop3: [2] # comm2
        """.trimIndent()
        val beforeVisitYaml = """
            prop1:
              sequence: [1]
            prop2:
              nested: 1
            prop3: [2]
        """.trimIndent()
        val (original, beforeVisit) = parse(originalYaml, beforeVisitYaml)
        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(originalYaml)
    }

    @Test
    fun `should copy comments in the end of the line for flow sequence when reordered`() {
        val originalYaml = """
            prop1:
              sequence: [1] # comm
            prop2:
              nested: 1
            prop3: [2] # comm2
        """.trimIndent()
        val beforeVisitYaml = """
            prop2:
              nested: 1
            prop3: [2]
            prop1:
              sequence: [1]
        """.trimIndent()
        val (original, beforeVisit) = parse(originalYaml, beforeVisitYaml)
        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(
            """
            prop2:
              nested: 1
            prop3: [2] # comm2
            prop1:
              sequence: [1] # comm
            """.trimIndent()
        )
    }

    @Test
    fun `should copy multiline comment from the original document`() {
        val yaml = """
            prop1: 1
            # comment line 1
            # comment line 2
            prop2: 2
            """.trimIndent()
        val docs = parse(yaml)
        val visitor = CopyCommentsVisitor(docs)
        val docBeforeVisitor = docs.removeTopLevelEntriesPrefix()

        // when
        val resultYaml = visitor.visit(docBeforeVisitor, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(yaml)
    }

    @Test
    fun `should copy comments for reordered properties`() {
        val (original, beforeVisit) = parse(
            """
            prop1: 1
            # comment 1
            prop2: 2
            # comment 2
            prop3: 3
            """.trimIndent(),
            """
            prop1: 1
            prop3: 3
            prop2: 2
            """.trimIndent()
        )
        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(
            """
            prop1: 1
            # comment 2
            prop3: 3
            # comment 1
            prop2: 2
            """.trimIndent(),
        )
    }

    @Test
    fun `should copy comments for the nested entries`() {
        val yamlWithComments = """
            prop1: 1
            prop:
              long:
                # pretty nested already
                path:
                  value: 1
            """.trimIndent()
        val yamlWithoutComments = """
            prop1: 1
            prop:
              long:
                path:
                  value: 1
            """.trimIndent()
        val original = parse(yamlWithComments)
        var beforeVisit = parse(yamlWithoutComments)

        // copy doc id
        beforeVisit = beforeVisit.withDocuments(listOf(beforeVisit.documents[0].withId(original.documents[0].id)))

        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(yamlWithComments)
    }

    @Test
    fun `should copy comments for the reordered nested entries`() {
        val yamlWithComments = """
            prop1: 1
            prop:
              nested:
                # should be one
                one: 1 # one
                # should be two
                two: 3 # two does not work, fix later
                three: 3 # three
            """.trimIndent()
        val yamlWithoutComments = """
            prop1: 1
            prop:
              nested:
                two: 3
                three: 3
                one: 1
            """.trimIndent()
        val original = parse(yamlWithComments)
        var beforeVisit = parse(yamlWithoutComments)

        // copy doc id
        beforeVisit = beforeVisit.withDocuments(listOf(beforeVisit.documents[0].withId(original.documents[0].id)))

        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(
            """
            prop1: 1
            prop:
              nested:
                # should be two
                two: 3 # two does not work, fix later
                three: 3 # three
                # should be one
                one: 1 # one
            """.trimIndent()
        )
    }

    @Test
    fun `should copy comments for scalar entries of block sequence`() {
        val yamlWithComments = """
            prop1: 1
            prop:
             - 1 # this is one
             - 2 # this is two
            """.trimIndent()
        val yamlWithoutComments = """
            prop1: 1
            prop:
             - 1
             - 2
            """.trimIndent()
        val original = parse(yamlWithComments)
        var beforeVisit = parse(yamlWithoutComments)

        // copy doc id
        beforeVisit = beforeVisit
            .withDocuments(listOf(beforeVisit.documents[0].withId(original.documents[0].id)))

        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(yamlWithComments)
    }

    @Test
    fun `should copy comments from the according documents inside a single file`() {
        val yaml = """
            prop1: 0
            # this is first doc
            prop2: 1
            ---
            prop1: 0
            # this is second doc
            prop2: 2
            ---
            prop1: 0
            prop2: 3
            """.trimIndent()
        val docs = parse(yaml)
        val visitor = CopyCommentsVisitor(docs)
        val docBeforeVisitor = docs.removeTopLevelEntriesPrefix()

        // when
        val resultYaml = visitor.visit(docBeforeVisitor, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(yaml)
    }

    @Test
    fun `should use keyMapper to match property keys`() {
        val (original, beforeVisit) = parse(
            """
            prop1.old: 0
            # comment
            prop2.old: 1
            """.trimIndent(),
            """
            prop1.new: 0
            prop2.new: 1
            """.trimIndent()
        )
        val visitor = CopyCommentsVisitor(original) { YamlPath(it.path.substringBeforeLast(".")) }

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(
            """
            prop1.new: 0
            # comment
            prop2.new: 1
            """.trimIndent()
        )
    }

    @Test
    fun `should support anchors`() {
        val (original, beforeVisit) = parse(
            """
            prop1: &ANCHOR # this is anchor
              val1: 1 # this is val 1
              # this is val 2
              val2: 2
            prop2:
              # just duplicate
              <<: *ANCHOR # reference to the prop1
            prop3: 3
            """.trimIndent(),
            """
            prop1: &ANCHOR
              val1: 1
              val2: 2
            prop2:
              <<: *ANCHOR
            prop3: 3
            """.trimIndent()
        )
        val visitor = CopyCommentsVisitor(original)

        // when
        val resultYaml = visitor.visit(beforeVisit, InMemoryExecutionContext()) as Yaml.Documents
        val result = resultYaml.toYamlString()

        // then
        assertThat(result).isEqualTo(
            """
            prop1: &ANCHOR # this is anchor
              val1: 1 # this is val 1
              # this is val 2
              val2: 2
            prop2:
              # just duplicate
              <<: *ANCHOR # reference to the prop1
            prop3: 3
            """.trimIndent()
        )
    }

    private fun Yaml.Documents.removeTopLevelEntriesPrefix() = this.mapDocuments {
        it.mapBlock<Yaml.Mapping> {
            it.mapEntries {
                it.withPrefix("\n")
            }
        }
    }
}

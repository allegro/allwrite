package pl.allegro.tech.allwrite.recipes.yaml

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.yaml.tree.Yaml

class PrefixPartsTest : YamlTest {

    @Nested
    inner class CapturesTest {

        @Test
        fun `should capture parts correctly for the end of line comment`() {
            val doc = parse(
                """
                prop1: 1 # this is comment for prop1
                prop2: 2
                """.trimIndent()
            )

            // when
            val prop2 = (doc.documents[0].block as Yaml.Mapping).entries[1] as Yaml.Mapping.Entry
            val result = prop2.prefixParts()

            // then
            assertThat(result.remainder).isEqualTo(" # this is comment for prop1")
            assertThat(result.main).isNull()
            assertThat(result.indent).isEqualTo("")
        }

        @Test
        fun `should capture parts correctly for the prefix comment`() {
            val doc = parse(
                """
                prop1: 1
                # this is comment for prop2
                prop2: 2
                """.trimIndent()
            )

            // when
            val prop2 = (doc.documents[0].block as Yaml.Mapping).entries[1] as Yaml.Mapping.Entry
            val result = prop2.prefixParts()

            // then
            assertThat(result.remainder).isEqualTo("")
            assertThat(result.main).isEqualTo("# this is comment for prop2")
            assertThat(result.indent).isEqualTo("")
        }

        @Test
        fun `should capture parts correctly for the mixed case`() {
            val doc = parse(
                """
                prop1: 1 # this is comment for prop1
                # this is comment for prop2
                prop2: 2
                """.trimIndent()
            )

            // when
            val prop2 = (doc.documents[0].block as Yaml.Mapping).entries[1] as Yaml.Mapping.Entry
            val result = prop2.prefixParts()

            // then
            assertThat(result.remainder).isEqualTo(" # this is comment for prop1")
            assertThat(result.main).isEqualTo("# this is comment for prop2")
            assertThat(result.indent).isEqualTo("")
        }

        @Test
        fun `should capture parts correctly for the mixed case with indent`() {
            val doc = parse(
                """
                object:   # this is comment for parent
                  # this is comment for prop
                  prop: 1
                """.trimIndent()
            )

            // when
            val prop2 = (((doc.documents[0].block as Yaml.Mapping).entries[0] as Yaml.Mapping.Entry).value as Yaml.Mapping).entries[0] as Yaml.Mapping.Entry
            val result = prop2.prefixParts()

            // then
            assertThat(result.remainder).isEqualTo("   # this is comment for parent")
            assertThat(result.main).isEqualTo("  # this is comment for prop")
            assertThat(result.indent).isEqualTo("  ")
        }
    }

    @Nested
    inner class AsStringTests {

        @Test
        fun `should remove leading and trailing newlines from main`() {
            val parts = PrefixParts("", "\n\n1\n2\n3\n\n", "")
            val result = parts.asString()
            assertThat(result).isEqualTo(
                """
                
                1
                2
                3
                
                """.trimIndent()
            )
        }

        @Test
        fun `should add the correct number of lines breaks`() {
            val parts = PrefixParts("remainder", "main", "indent")
            val result = parts.asString(lineBreaksBefore = 2, lineBreaksAfter = 3)
            assertThat(result).isEqualTo("remainder\n\nmain\n\n\nindent")
        }

        @Test
        fun `should not add line breaks after main if main is empty`() {
            val parts = PrefixParts("remainder", "   ", "indent")
            val result = parts.asString(lineBreaksBefore = 2, lineBreaksAfter = 3)
            assertThat(result).isEqualTo("remainder\n\nindent")
        }

        @Test
        fun `should not add line breaks after main if main is null`() {
            val parts = PrefixParts("remainder", null, "indent")
            val result = parts.asString(lineBreaksBefore = 2, lineBreaksAfter = 3)
            assertThat(result).isEqualTo("remainder\n\nindent")
        }

        @Test
        fun `should not add line breaks if there is just indent`() {
            val parts = PrefixParts(null, null, "indent")
            val result = parts.asString(lineBreaksBefore = 2, lineBreaksAfter = 3)
            assertThat(result).isEqualTo("indent")
        }
    }

}

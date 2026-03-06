package pl.allegro.tech.allwrite.recipes.util

import io.kotest.matchers.equals.shouldBeEqual
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import pl.allegro.tech.allwrite.recipes.util.JavaStringLiteral

class CollectionUtilsTest {
    @Nested
    inner class MapFirst {
        @Test
        fun `should apply the mapper to the first element and return a new list`() {
            // given
            val list = listOf(1, 2, 3)

            // when
            val result = list.mapFirst { it * 2 }

            // then
            assertEquals(listOf(2, 2, 3), result)
            assertEquals(listOf(1, 2, 3), list)
        }

        @Test
        fun `should be noop if the list is empty`() {
            // given
            val list = emptyList<Int>()

            // when
            val result = list.mapFirst { it * 2 }

            // then
            assertSame(list, result)
        }
    }

    @Nested
    inner class ReplaceByInstance {

        @Test
        fun `should replace the element of the list`() {
            val first = JavaStringLiteral("a")
            val second = JavaStringLiteral("b")
            val replacement = JavaStringLiteral("replacement")

            val list = listOf(first, second)
            val result = list.replace(first, replacement)

            result shouldBeEqual listOf(replacement, second)
        }

        @Test
        fun `should not replace if element is equal, but the references are different`() {
            val first = JavaStringLiteral("a")
            val second = JavaStringLiteral("b")
            val replacement = JavaStringLiteral("replacement")

            val list = listOf(first, second)
            val result = list.replace(JavaStringLiteral("a"), replacement)

            result shouldBeEqual list
        }

        @Test
        fun `returned list should be equal to original list when no replacement happened`() {
            val first = JavaStringLiteral("a")
            val second = JavaStringLiteral("b")
            val third = JavaStringLiteral("c")

            val list = listOf(first, second)
            val result = list.replace(third, JavaStringLiteral("replacement"))

            result shouldBeEqual list
        }
    }
}

package pl.allegro.tech.allwrite.recipes

import org.junit.jupiter.api.Test
import org.openrewrite.java.JavaParser.runtimeClasspath
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.java.ChangeType

class ChangeTypeKotlinTest : RewriteTest {

    @Test
    fun changeJavaType() {
        rewriteRun(
            { spec ->
                spec
                    .parser(KotlinParser.builder().classpath(runtimeClasspath()))
                    .recipe(
                        ChangeType(
                            "com.example.otherpackage.OldSampleClass",
                            "com.example.somepackage.NewSampleClass",
                            false
                        )
                    )
            },
            kotlin(
                before = """
                    import com.example.otherpackage.OldSampleClass

                    class SomeService {
                        private val oldSampleClass = OldSampleClass()
                    }
                    """.trimIndent(),
                after = """
                    import com.example.somepackage.NewSampleClass

                    class SomeService {
                        private val newSampleClass = NewSampleClass()
                    }
                    """.trimIndent()
            ),
            kotlin(
                before = """
                    import com.example.otherpackage.OldSampleClass

                    class SomeService(
                        private val oldSampleClass: OldSampleClass
                    ) {
                        fun foo() {
                            val someField = oldSampleClass.someField
                        }
                    }
                    """.trimIndent(),
                after = """
                    import com.example.somepackage.NewSampleClass

                    class SomeService(
                        private val newSampleClass: NewSampleClass
                    ) {
                        fun foo() {
                            val someField = newSampleClass.someField
                        }
                    }
                    """.trimIndent()
            ),
        )
    }
}

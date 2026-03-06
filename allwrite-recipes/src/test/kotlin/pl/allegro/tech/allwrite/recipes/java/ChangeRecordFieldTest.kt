package pl.allegro.tech.allwrite.recipes.java

import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin

class ChangeRecordFieldTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        val recipe = ChangeRecordField(
            declaringTypeFqn = "com.example.SomeRecord",
            oldFieldName = "oldField",
            newFieldName = "newField",
        )
        spec.recipe(recipe)
            .validateRecipeSerialization(false)
    }

    @Test
    fun `should rename given field`() {
        rewriteRun(
            kotlin(
                before = """
                    import com.example.SomeRecord

                    class Foo(private val someRecord: SomeRecord) {
                       fun bar() {
                           println(someRecord.oldField)
                       }
                    }
                    """.trimIndent(),
                after = """
                    import com.example.SomeRecord

                    class Foo(private val someRecord: SomeRecord) {
                       fun bar() {
                           println(someRecord.newField)
                       }
                    }
                    """.trimIndent()
            ),
            kotlin(
                before = """
                    import com.example.SomeRecord

                    class Foo(private val someRecord: SomeRecord) {
                       fun bar() {
                           println(someRecord.oldField())
                       }
                    }
                    """.trimIndent(),
                after = """
                    import com.example.SomeRecord

                    class Foo(private val someRecord: SomeRecord) {
                       fun bar() {
                           println(someRecord.newField())
                       }
                    }
                    """.trimIndent()
            ),
            java(
                before = """
                    import com.example.SomeRecord;

                    class Foo {
                        private final SomeRecord someRecord;

                        void bar() {
                            System.out.println(someRecord.oldField());
                        }
                    }
                """.trimIndent(),
                after = """
                    import com.example.SomeRecord;

                    class Foo {
                        private final SomeRecord someRecord;

                        void bar() {
                            System.out.println(someRecord.newField());
                        }
                    }
                """.trimIndent()
            )
        )
    }

    @Test
    fun `should not rename other field`() {
        rewriteRun(
            kotlin(
                beforeAndAfter = """
                    import com.example.SomeRecord

                    class Foo(private val someRecord: SomeRecord) {
                       fun bar() {
                           println(someRecord.anotherField)
                       }
                    }
                    """.trimIndent(),
            ),
            java(
                beforeAndAfter = """
                    import com.example.SomeRecord;

                    class Foo {
                        private final SomeRecord someRecord;

                        void bar() {
                            System.out.println(someRecord.anotherField());
                        }
                    }
                """.trimIndent(),
            )
        )
    }
}

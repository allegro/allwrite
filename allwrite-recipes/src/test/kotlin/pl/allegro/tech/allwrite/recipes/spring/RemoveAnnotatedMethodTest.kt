package pl.allegro.tech.allwrite.recipes.spring

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.TypeValidation
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.runtime.util.withRecipeClasspath

class RemoveAnnotatedMethodTest {

    class RemoveSomeClassUsedAsABeanMethod :
        RemoveAnnotatedMethod(
            returnType = "pl.allegro.example.SomeClassUsedAsABean",
            annotationName = "pl.allegro.example.SomeAnnotation",
        )

    class RemoveSomeClassUsedAsABeanMethodWithAllowedBodyMethods :
        RemoveAnnotatedMethod(
            returnType = "pl.allegro.example.SomeClassUsedAsABean",
            annotationName = "pl.allegro.example.SomeAnnotation",
            allowedBodyCalls = setOf("someAllowedMethod", "println"),
        )

    @Nested
    inner class SimpleRemoveTest : RewriteTest {

        override fun defaults(spec: RecipeSpec) {
            spec.recipe(RemoveSomeClassUsedAsABeanMethod())
                .withRecipeClasspath()
                .typeValidationOptions(TypeValidation.none())
        }

        @Nested
        inner class Kotlin {

            @Test
            fun `should remove method annotated with @SomeAnnotation`() {
                rewriteRun(
                    kotlin(
                        before = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean

                            class SomeConfig {

                                @SomeAnnotation
                                fun someClassUsedAsABean(): SomeClassUsedAsABean = SomeClassUsedAsABean()
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean

                            class SomeConfig {
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method annotated with @SomeAnnotation when method has some parameters`() {
                rewriteRun(
                    kotlin(
                        beforeAndAfter = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean

                            class SomeConfig {

                                @SomeAnnotation
                                fun someClassUsedAsABean(someParam: String): SomeClassUsedAsABean = SomeClassUsedAsABean()
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method not returning the target type`() {
                rewriteRun(
                    kotlin(
                        beforeAndAfter = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeConfig {

                                @SomeAnnotation
                                fun someClassUsedAsABean(): String = "someDummyString"
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should not remove @SomeAnnotation import when other method is using it`() {
                rewriteRun(
                    kotlin(
                        before = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean
                            class SomeOtherBean

                            class SomeConfig {

                                @SomeAnnotation
                                fun someClassUsedAsABean(): SomeClassUsedAsABean = SomeClassUsedAsABean()

                                @SomeAnnotation
                                fun someOtherBean() = SomeOtherBean()
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean
                            class SomeOtherBean

                            class SomeConfig {

                                @SomeAnnotation
                                fun someOtherBean() = SomeOtherBean()
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method when there is also another annotation`() {
                rewriteRun(
                    kotlin(
                        beforeAndAfter = """
                            package pl.allegro.example

                            annotation class SomeAnnotation
                            annotation class SomeOtherAnnotation

                            class SomeClassUsedAsABean

                            class SomeConfig {

                                @SomeAnnotation
                                @SomeOtherAnnotation
                                fun someClassUsedAsABean(): SomeClassUsedAsABean = SomeClassUsedAsABean()
                            }
                        """.trimIndent(),
                    ),
                )
            }
        }

        @Nested
        inner class Java {

            @Test
            fun `should remove method annotated with @SomeAnnotation`() {
                rewriteRun(
                    java(
                        before = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}

                            public class SomeConfig {

                                @SomeAnnotation
                                public SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean();
                                }
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}

                            public class SomeConfig {
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method annotated with @SomeAnnotation when method has some parameters`() {
                rewriteRun(
                    java(
                        beforeAndAfter = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}

                            public class SomeConfig {

                                @SomeAnnotation
                                public SomeClassUsedAsABean someClassUsedAsABean(String someParam) {
                                    return new SomeClassUsedAsABean();
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method not returning the target type`() {
                rewriteRun(
                    java(
                        beforeAndAfter = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            public class SomeConfig {

                                @SomeAnnotation
                                public String someClassUsedAsABean() {
                                    return "someDummyString";
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should not remove @SomeAnnotation when other method is using it`() {
                rewriteRun(
                    java(
                        before = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}
                            class SomeOtherBean {}

                            public class SomeConfig {

                                @SomeAnnotation
                                public SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean();
                                }

                                @SomeAnnotation
                                public SomeOtherBean someOtherBean() {
                                    return new SomeOtherBean();
                                }
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}
                            class SomeOtherBean {}

                            public class SomeConfig {

                                @SomeAnnotation
                                public SomeOtherBean someOtherBean() {
                                    return new SomeOtherBean();
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method when there is also another annotation`() {
                rewriteRun(
                    java(
                        beforeAndAfter = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}
                            @interface SomeOtherAnnotation {}

                            class SomeClassUsedAsABean {}

                            public class SomeConfig {

                                @SomeAnnotation
                                @SomeOtherAnnotation
                                public SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean();
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }
        }

        @Nested
        inner class Groovy {

            @Test
            fun `should remove method annotated with @SomeAnnotation`() {
                rewriteRun(
                    groovy(
                        before = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}

                            class SomeConfig {

                                @SomeAnnotation
                                SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean()
                                }
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}

                            class SomeConfig {
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method annotated with @SomeAnnotation when method has some parameters`() {
                rewriteRun(
                    groovy(
                        beforeAndAfter = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}

                            class SomeConfig {

                                @SomeAnnotation
                                SomeClassUsedAsABean someClassUsedAsABean(String someParam) {
                                    return new SomeClassUsedAsABean()
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method not returning the target type`() {
                rewriteRun(
                    groovy(
                        beforeAndAfter = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeConfig {

                                @SomeAnnotation
                                String someClassUsedAsABean() {
                                    return "someDummyString"
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should not remove @SomeAnnotation when other method is using it`() {
                rewriteRun(
                    groovy(
                        before = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}
                            class SomeOtherBean {}

                            class SomeConfig {

                                @SomeAnnotation
                                SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean()
                                }

                                @SomeAnnotation
                                SomeOtherBean someOtherBean() {
                                    return new SomeOtherBean()
                                }
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {}
                            class SomeOtherBean {}

                            class SomeConfig {

                                @SomeAnnotation
                                SomeOtherBean someOtherBean() {
                                    return new SomeOtherBean()
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method when there is also another annotation`() {
                rewriteRun(
                    groovy(
                        beforeAndAfter = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}
                            @interface SomeOtherAnnotation {}

                            class SomeClassUsedAsABean {}

                            class SomeConfig {

                                @SomeAnnotation
                                @SomeOtherAnnotation
                                SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean()
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }
        }
    }

    @Nested
    inner class WithAllowedBodyMethodsTest : RewriteTest {

        override fun defaults(spec: RecipeSpec) {
            spec.recipe(RemoveSomeClassUsedAsABeanMethodWithAllowedBodyMethods())
                .withRecipeClasspath()
                .typeValidationOptions(TypeValidation.none())
        }

        @Nested
        inner class Kotlin {

            @Test
            fun `should remove method when body uses only allowed methods`() {
                rewriteRun(
                    kotlin(
                        before = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean {
                                fun someAllowedMethod(): SomeClassUsedAsABean = SomeClassUsedAsABean()
                            }

                            class SomeConfig {

                                @SomeAnnotation
                                fun someClassUsedAsABean(): SomeClassUsedAsABean {
                                    println("this println call is allowed")
                                    return SomeClassUsedAsABean().someAllowedMethod()
                                }
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean {
                                fun someAllowedMethod(): SomeClassUsedAsABean = SomeClassUsedAsABean()
                            }

                            class SomeConfig {
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method when body uses disallowed methods`() {
                rewriteRun(
                    kotlin(
                        beforeAndAfter = """
                            package pl.allegro.example

                            annotation class SomeAnnotation

                            class SomeClassUsedAsABean {
                                fun someAllowedMethod(): SomeClassUsedAsABean = SomeClassUsedAsABean()
                                fun someNotAllowedMethod(value: String): SomeClassUsedAsABean = this
                            }

                            class SomeConfig {

                                @SomeAnnotation
                                fun someClassUsedAsABean(): SomeClassUsedAsABean =
                                    SomeClassUsedAsABean().someAllowedMethod().someNotAllowedMethod("value")
                            }
                        """.trimIndent(),
                    ),
                )
            }
        }

        @Nested
        inner class Java {

            @Test
            fun `should remove method when body uses only allowed methods`() {
                rewriteRun(
                    java(
                        before = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {
                                SomeClassUsedAsABean someAllowedMethod() { return new SomeClassUsedAsABean(); }
                            }

                            class SomeConfig {

                                @SomeAnnotation
                                public SomeClassUsedAsABean someClassUsedAsABean() {
                                    System.out.println("this println call is allowed");
                                    return new SomeClassUsedAsABean().someAllowedMethod();
                                }
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {
                                SomeClassUsedAsABean someAllowedMethod() { return new SomeClassUsedAsABean(); }
                            }

                            class SomeConfig {
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method when body uses disallowed methods`() {
                rewriteRun(
                    java(
                        beforeAndAfter = """
                            package pl.allegro.example;

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {
                                SomeClassUsedAsABean someAllowedMethod() { return new SomeClassUsedAsABean(); }
                                SomeClassUsedAsABean someNotAllowedMethod(String value) { return this; }
                            }

                            class SomeConfig {

                                @SomeAnnotation
                                public SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean().someAllowedMethod().someNotAllowedMethod("value");
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }
        }

        @Nested
        inner class Groovy {

            @Test
            fun `should remove method when body uses only allowed methods`() {
                rewriteRun(
                    groovy(
                        before = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {
                                SomeClassUsedAsABean someAllowedMethod() { return new SomeClassUsedAsABean() }
                            }

                            class SomeConfig {

                                @SomeAnnotation
                                SomeClassUsedAsABean someClassUsedAsABean() {
§                                    println("this println call is allowed")
                                    return new SomeClassUsedAsABean().someAllowedMethod()
                                }
                            }
                        """.trimIndent(),
                        after = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {
                                SomeClassUsedAsABean someAllowedMethod() { return new SomeClassUsedAsABean() }
                            }

                            class SomeConfig {
                            }
                        """.trimIndent(),
                    ),
                )
            }

            @Test
            fun `should NOT remove method when body uses disallowed methods`() {
                rewriteRun(
                    groovy(
                        beforeAndAfter = """
                            package pl.allegro.example

                            @interface SomeAnnotation {}

                            class SomeClassUsedAsABean {
                                SomeClassUsedAsABean someAllowedMethod() { return new SomeClassUsedAsABean() }
                                SomeClassUsedAsABean someNotAllowedMethod(String value) { return this }
                            }

                            class SomeConfig {

                                @SomeAnnotation
                                SomeClassUsedAsABean someClassUsedAsABean() {
                                    return new SomeClassUsedAsABean().someAllowedMethod().someNotAllowedMethod("value")
                                }
                            }
                        """.trimIndent(),
                    ),
                )
            }
        }
    }
}

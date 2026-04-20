package pl.allegro.tech.allwrite.recipes.spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.openrewrite.Cursor
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import pl.allegro.tech.allwrite.recipes.spring.util.Variable
import kotlin.jvm.optionals.getOrNull

class QualifyVariableTest {

    @BeforeEach
    fun setUp() {
        parser.reset()
    }

    @Test
    fun `should update qualifier for class field value when already present`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              @Qualifier("old") int a = 1;
            }
            """.trimIndent(),
        )
        val variable = j.classes[0].body.statements[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test").visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              @Qualifier("test") int a = 1;
            }
            """.trimIndent(),
        )
    }

    // we want to minimize diff
    @Test
    fun `should preserve formatting (even if its weird)`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              @Qualifier("old")        int a=1;
            }
            """.trimIndent(),
        )
        val variable = j.classes[0].body.statements[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              @Qualifier("test")        int a=1;
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `should update qualifier for constructor argument value when already present`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              public A(@Qualifier("old") int a) {}
            }
            """.trimIndent(),
        )
        val variable = (j.classes[0].body.statements[0] as J.MethodDeclaration).parameters[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              public A(@Qualifier("test") int a) {}
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `should update qualifier for method argument value when already present`() {
        val j = parse(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              public void test(@Qualifier("old") int a) {}
            }
            """.trimIndent(),
        )
        val variable = (j.classes[0].body.statements[0] as J.MethodDeclaration).parameters[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            import org.springframework.beans.factory.annotation.Qualifier;
            public class A {
              public void test(@Qualifier("test") int a) {}
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `should insert qualifier for class field value`() {
        val j = parse(
            """
            public class A {
              int a = 1;
            }
            """.trimIndent(),
        )
        val variable = j.classes[0].body.statements[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            public class A {
              @Qualifier("test") int a = 1;
            }
            """.trimIndent(),
        )
    }

    // we want to minimize diff
    @Test
    fun `should insert qualifier and preserve formatting (even if its weird)`() {
        val j = parse(
            """
            public class A {
              int a=1;
            }
            """.trimIndent(),
        )
        val variable = j.classes[0].body.statements[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            public class A {
              @Qualifier("test") int a=1;
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `should insert qualifier for constructor argument`() {
        val j = parse(
            """
            public class A {
              public A(int a) {}
            }
            """.trimIndent(),
        )
        val variable = (j.classes[0].body.statements[0] as J.MethodDeclaration).parameters[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            public class A {
              public A(@Qualifier("test") int a) {}
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `should insert qualifier for kotlin constructor argument`() {
        val k = parseKotlin(
            """
            class A(a: Int) { companion object {} }
            """.trimIndent(),
        )
        val variable = (k.classes[0].body.statements[0] as J.MethodDeclaration).parameters[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(k, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            class A(@Qualifier("test") a: Int) { companion object {} }
            """.trimIndent(),
        )
    }

    @Test
    fun `should insert qualifier for method argument value`() {
        val j = parse(
            """
            public class A {
              public void test(int a) {}
            }
            """.trimIndent(),
        )
        val variable = (j.classes[0].body.statements[0] as J.MethodDeclaration).parameters[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(
            """
            public class A {
              public void test(@Qualifier("test") int a) {}
            }
            """.trimIndent(),
        )
    }

    @Test
    fun `should not qualify local variable`() {
        val source = """
        public class A {
          public void foo() {
             var x = 123;
          }
        }
        """.trimIndent()
        val j = parse(source)
        val variable = (j.classes[0].body.statements[0] as J.MethodDeclaration).body!!.statements[0] as J.VariableDeclarations

        // when
        val result = QualifyVariable(Variable(variable.variables[0], variable), "test")
            .visit(j, InMemoryExecutionContext())!!

        // then
        assertThat(result.print(Cursor(null, result))).isEqualTo(source)
    }

    fun parse(java: String): J.CompilationUnit = parser.parse(java).findFirst().getOrNull() as J.CompilationUnit
    fun parseKotlin(java: String): K.CompilationUnit = kotlinParser.parse(java).findFirst().getOrNull() as K.CompilationUnit

    companion object {
        private val parser = JavaParser.fromJavaVersion()
            .classpathFromResources(InMemoryExecutionContext(), "spring-beans-6")
            .build()
        val kotlinParser: KotlinParser = KotlinParser.builder().build()
    }
}

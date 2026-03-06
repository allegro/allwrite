package pl.allegro.tech.allwrite.recipes.spring.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.tree.K

class ConstructorTest : ParsingTest() {

    @BeforeEach
    fun setup() {
        javaParser.reset()
    }

    @Test
    fun `should return a constructor when it is the only one`() {
        val jclass = parse(
            """
            class Config {
              Config(int x) {}
            }
            """.trimIndent()).classes[0]

        // when
        val result = jclass.getAutowiringConstructor()

        // then
        assertNotNull(result)
        assertEquals(jclass.body.statements[0] as J.MethodDeclaration, result.method)
    }

    @Test
    fun `should return a first constructor with @Autowired when there are more than one`() {
        val jclass = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            class Config {
              Config() {}
              @Autowired Config(String str) {}
              Config(int x) {}
              @Autowired Config(String str, int x) {}
            }
            """.trimIndent()).classes[0]

        // when
        val result = jclass.getAutowiringConstructor()

        // then
        assertNotNull(result)
        assertEquals(jclass.body.statements[1] as J.MethodDeclaration, result.method)
    }

    @Test
    fun `should return default constructor when there is more than one constructor and none of them has @Autowired`() {
        val jclass = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            class Config {
              Config(String str) {}
              Config() {}
              Config(int x) {}
              Config(String str, int x) {}
            }
            """.trimIndent()).classes[0]

        // when
        val result = jclass.getAutowiringConstructor()

        // then
        assertNotNull(result)
        assertEquals(jclass.body.statements[1] as J.MethodDeclaration, result.method)
    }

    @Test
    fun `should return null when there is more than one constructor and none of them is default or has has @Autowired`() {
        val jclass = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            class Config {
              Config(String str) {}
              Config(int x) {}
              Config(String str, int x) {}
            }
            """.trimIndent()).classes[0]

        // when
        val result = jclass.getAutowiringConstructor()

        // then
        assertNull(result)
    }

    @Test
    fun `should support kotlin secondary constructors`() {
        var kclass = parseKotlin(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            class Config @Autowired constructor(str: String, x: Int) {
              constructor() : this("", 0)
            }
            """.trimIndent()).classes[0]

        // when
        var result = kclass.getAutowiringConstructor()

        // then
        assertNotNull(result)
        assertEquals(kclass.body.statements[0] as J.MethodDeclaration, result.method)

        // when
        kclass = parseKotlin(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            class Config(str: String) {
              @Autowired constructor(str: String, x: Int) : this(str + x)
            }
            """.trimIndent()).classes[0]
        result = kclass.getAutowiringConstructor()

        // then
        assertNotNull(result)
        assertEquals((kclass.body.statements[1] as K.Constructor).methodDeclaration, result.method)

        // when
        kclass = parseKotlin(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            
            @Autowired
            class Config(str: String) {
              constructor(str: String, x: Int) : this(str + x)
            }
            """.trimIndent()).classes[0]
        result = kclass.getAutowiringConstructor()

        // then
        assertNull(result)

        // when
        kclass = parseKotlin(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            
            @Autowired
            class Config(str: String) {
              constructor() : this("123")
            }
            """.trimIndent()).classes[0]
        result = kclass.getAutowiringConstructor()

        // then
        assertNotNull(result)
        assertEquals((kclass.body.statements[1] as K.Constructor).methodDeclaration, result.method)
    }
}

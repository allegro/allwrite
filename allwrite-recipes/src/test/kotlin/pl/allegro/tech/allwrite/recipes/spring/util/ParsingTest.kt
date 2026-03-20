package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.java.JavaParser
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.kotlin.tree.K
import kotlin.jvm.optionals.getOrNull

abstract class ParsingTest {
    val javaParser: JavaParser = JavaParser.fromJavaVersion()
        .classpathFromResources(CTX, *SPRING_FRAMEWORK_ARTIFACTS)
        .build()
    val kotlinParser: KotlinParser = KotlinParser.builder()
        .classpathFromResources(CTX, *SPRING_FRAMEWORK_ARTIFACTS)
        .build()

    fun parse(java: String): J.CompilationUnit = javaParser.parse(java).findFirst().getOrNull() as J.CompilationUnit
    fun parseKotlin(java: String): K.CompilationUnit = kotlinParser.parse(java).findFirst().getOrNull() as K.CompilationUnit
}

private val CTX = InMemoryExecutionContext()

private val SPRING_FRAMEWORK_ARTIFACTS = arrayOf(
    "spring-context-6",
    "spring-core-6",
    "spring-beans-6",
    "spring-web-6",
    "jakarta.annotation-api",
    "jakarta.inject-api",
)

package pl.allegro.tech.allwrite.recipes.spring.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class AnnotationTest : ParsingTest() {

    @Test
    fun `should correctly find annotations`() {
        val jclass = parse(
            """
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            import jakarta.annotation.Resource;
            import jakarta.inject.Named;
            
            import org.springframework.stereotype.Component;
            import org.springframework.stereotype.Service;
            
            @Named
            @Service
            @Autowired
            @Qualifier
            @Component
            @Resource
            class A {}
            """.trimIndent(),
        ).classes[0]

        // then
        assertThat(jclass.leadingAnnotations.findNamedAnnotation()).isSameAs(jclass.leadingAnnotations[0])
        assertThat(jclass.leadingAnnotations.findAutowiredAnnotation()).isSameAs(jclass.leadingAnnotations[2])
        assertThat(jclass.leadingAnnotations.findQualifierAnnotation()).isSameAs(jclass.leadingAnnotations[3])
        assertThat(jclass.leadingAnnotations.findResourceAnnotation()).isSameAs(jclass.leadingAnnotations[5])
    }

    @Test
    fun `should correctly find annotations in kotlin`() {
        val kclass = parseKotlin(
            """
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.beans.factory.annotation.Qualifier
            import jakarta.annotation.Resource
            import jakarta.inject.Named
            
            import org.springframework.stereotype.Component
            import org.springframework.stereotype.Service
            
            @Named
            @Service
            @Autowired
            @Qualifier
            @Component
            @Resource
            class A() {}
            """.trimIndent(),
        ).classes[0]

        // then
        assertThat(kclass.leadingAnnotations.findNamedAnnotation()).isSameAs(kclass.leadingAnnotations[0])
        assertThat(kclass.leadingAnnotations.findAutowiredAnnotation()).isSameAs(kclass.leadingAnnotations[2])
        assertThat(kclass.leadingAnnotations.findQualifierAnnotation()).isSameAs(kclass.leadingAnnotations[3])
        assertThat(kclass.leadingAnnotations.findResourceAnnotation()).isSameAs(kclass.leadingAnnotations[5])
    }

    @Test
    fun `should find qualified name according to the priorities`() {
        var kclass = parseKotlin(
            """
            import org.springframework.beans.factory.annotation.Qualifier
            import jakarta.annotation.Resource
            import jakarta.inject.Named
            
            @Named("n")
            @Qualifier("q")
            @Resource(name = "r")
            class A
            """.trimIndent(),
        ).classes[0]

        // then
        assertThat(kclass.leadingAnnotations.findVariableQualifiedName()).isEqualTo("r")

        // when
        kclass = parseKotlin(
            """
            import org.springframework.beans.factory.annotation.Qualifier
            import jakarta.inject.Named
            
            @Named("n")
            @Qualifier("q")
            class A
            """.trimIndent(),
        ).classes[0]

        // then
        assertThat(kclass.leadingAnnotations.findVariableQualifiedName()).isEqualTo("q")

        // when
        kclass = parseKotlin(
            """
            import jakarta.inject.Named
            
            @Named("n")
            class A
            """.trimIndent(),
        ).classes[0]

        // then
        assertThat(kclass.leadingAnnotations.findVariableQualifiedName()).isEqualTo("n")

        // when
        kclass = parseKotlin("class A").classes[0]

        // then
        assertThat(kclass.leadingAnnotations.findVariableQualifiedName()).isNull()
    }
}

package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils
import pl.allegro.tech.allwrite.recipes.java.getArgument
import pl.allegro.tech.allwrite.recipes.java.getValueArgument

private val SPRING_COMPONENT_ANNOTATION_TYPES = setOf(
    ANNOTATION_COMPONENT,
    ANNOTATION_CONTROLLER,
    ANNOTATION_REST_CONTROLLER,
    ANNOTATION_REPOSITORY,
    ANNOTATION_SERVICE,
    ANNOTATION_CONFIGURATION,
)

internal data class SpringComponentAnnotation(val name: String, val annotation: J.Annotation)

/**
 * Returns a non-null [SpringComponentAnnotation] if the class is marked with Spring
 * stereotype annotation, or the jakarta's [com.fasterxml.jackson.databind.util.Named]
 * annotation.
 *
 * Qualified names are resolved in the following order:
 * - explicit name from @Component (or other stereotype annotations)
 * - explicit name from @Named
 * - implicit name
 *
 * If annotation contains no qualifier, returns an implicit bean name retrieved from
 * class name.
 */
internal fun J.ClassDeclaration.getSpringComponentAnnotation(): SpringComponentAnnotation? {
    val stereotypeAnnotation = leadingAnnotations.find {
        SPRING_COMPONENT_ANNOTATION_TYPES.contains((it.type as? JavaType.FullyQualified)?.fullyQualifiedName)
    }
    val stereotypeName = stereotypeAnnotation?.getValueArgument()?.unwrapString()

    val namedAnnotation = leadingAnnotations.find { TypeUtils.isAssignableTo(ANNOTATION_NAMED, it.type) }
    val namedName = namedAnnotation?.getValueArgument()?.unwrapString()

    return when {
        // explicit name at stereotype annotation is present, e.g. @Component("myName")
        stereotypeName != null -> SpringComponentAnnotation(stereotypeName, stereotypeAnnotation)

        // no name at stereotype, but explicit @Named("myName") is present
        namedName != null -> SpringComponentAnnotation(namedName, namedAnnotation)

        // stereotype is present, but without explicit name, no @Named
        stereotypeAnnotation != null -> SpringComponentAnnotation(name.toSpringBeanName(), stereotypeAnnotation)

        else -> null
    }
}

// TODO: can also be @AutoConfiguration or anything "inheriting" @Configuration
internal fun J.ClassDeclaration.hasConfigurationAnnotation(): Boolean = leadingAnnotations.any { TypeUtils.isAssignableTo(ANNOTATION_CONFIGURATION, it.type) }

/**
 * Contains a @Bean annotation and a method, marked with this annotation
 */
internal data class BeanMethodDeclaration(val annotation: J.Annotation, val method: J.MethodDeclaration) {

    /**
     * Returns a qualified name from @Bean annotation if present, or an implicit bean name
     * retrieved from the method name.
     */
    val beanName = annotation.getValueArgument()?.unwrapString()
        ?: annotation.getArgument("name")?.unwrapString()
        ?: method.name.toSpringBeanName()
}

/**
 * Returns list of [BeanMethodDeclaration] retrieved from the class methods marked with @Bean annotation
 */
internal fun J.ClassDeclaration.getBeanMethodDeclarations(): List<BeanMethodDeclaration> = body.statements
    .filterIsInstance<J.MethodDeclaration>()
    .mapNotNull { m ->
        val beanAnnotation = m.leadingAnnotations.find { TypeUtils.isAssignableTo(ANNOTATION_BEAN, it.type) }
        beanAnnotation?.let { BeanMethodDeclaration(beanAnnotation, m) }
    }

/**
 * Returns list of [Variable] retrieved from class fields marked with @Autowired, @Resource or @Inject annotations
 */
internal fun J.ClassDeclaration.getAutowiredFields(): List<Variable> = body.statements.filterIsInstance<J.VariableDeclarations>()
    .filter { it.isAutowired() }
    .flatMap { it.variables() }

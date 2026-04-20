package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import org.openrewrite.java.tree.TypeUtils
import pl.allegro.tech.allwrite.recipes.java.getArgument
import pl.allegro.tech.allwrite.recipes.java.getValueArgument

public const val ANNOTATION_BEAN: String = "org.springframework.context.annotation.Bean"
public const val ANNOTATION_CONFIGURATION: String = "org.springframework.context.annotation.Configuration"

public const val ANNOTATION_COMPONENT: String = "org.springframework.stereotype.Component"
public const val ANNOTATION_SERVICE: String = "org.springframework.stereotype.Service"
public const val ANNOTATION_CONTROLLER: String = "org.springframework.stereotype.Controller"
public const val ANNOTATION_REPOSITORY: String = "org.springframework.stereotype.Repository"
public const val ANNOTATION_REST_CONTROLLER: String = "org.springframework.web.bind.annotation.RestController"

public const val ANNOTATION_AUTOWIRED: String = "org.springframework.beans.factory.annotation.Autowired"
public const val ANNOTATION_QUALIFIER: String = "org.springframework.beans.factory.annotation.Qualifier"
public const val ANNOTATION_RESOURCE: String = "jakarta.annotation.Resource"
public const val ANNOTATION_NAMED: String = "jakarta.inject.Named"

public const val ANNOTATIONS_ACTIVE_PROFILES: String = "org.springframework.test.context.ActiveProfiles"
public const val ANNOTATIONS_SPRING_BOOT_TEST: String = "org.springframework.boot.test.context.SpringBootTest"
public const val ANNOTATIONS_TEST_PROPERTY_SOURCE: String = "org.springframework.test.context.TestPropertySource"

internal fun List<J.Annotation>.findAutowiredAnnotation(): J.Annotation? = firstOrNull { TypeUtils.isAssignableTo(ANNOTATION_AUTOWIRED, it.type) }
internal fun List<J.Annotation>.findQualifierAnnotation(): J.Annotation? = firstOrNull { TypeUtils.isAssignableTo(ANNOTATION_QUALIFIER, it.type) }
internal fun List<J.Annotation>.findResourceAnnotation(): J.Annotation? = firstOrNull { TypeUtils.isAssignableTo(ANNOTATION_RESOURCE, it.type) }
internal fun List<J.Annotation>.findNamedAnnotation(): J.Annotation? = firstOrNull { TypeUtils.isAssignableTo(ANNOTATION_NAMED, it.type) }
internal fun List<J.Annotation>.findActiveProfilesAnnotation(): J.Annotation? = firstOrNull { TypeUtils.isAssignableTo(ANNOTATIONS_ACTIVE_PROFILES, it.type) }

internal fun List<J.Annotation>.findAnnotation(annotationName: String): J.Annotation? = firstOrNull { TypeUtils.isAssignableTo(annotationName, it.type) }

internal fun List<J.Annotation>.findVariableQualifiedName(): String? =
    findResourceAnnotation()?.getArgument("name")?.unwrapString()
        ?: findQualifierAnnotation()?.getValueArgument()?.unwrapString()
        ?: findNamedAnnotation()?.getValueArgument()?.unwrapString()

internal fun J.Annotation.isEndpointMapping(): Boolean {
    val type = this.type
    return type is JavaType.Class &&
        type.fullyQualifiedName.startsWith("org.springframework.web.bind.annotation") &&
        type.fullyQualifiedName.endsWith("Mapping")
}

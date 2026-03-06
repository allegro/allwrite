package pl.allegro.tech.allwrite.recipes.spring.util

import org.openrewrite.java.tree.J
import java.util.Locale

/**
 * Mimic the spring implicit bean naming logic for @Component classes and @Bean methods
 */
internal fun J.Identifier.toSpringBeanName(): String = simpleName.replaceFirstChar { it.lowercase(Locale.getDefault()) }

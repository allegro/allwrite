package pl.allegro.tech.allwrite.recipes.spring

import org.openrewrite.Recipe
import pl.allegro.tech.allwrite.AllwriteRecipe
import pl.allegro.tech.allwrite.ClasspathAwareRecipe
import pl.allegro.tech.allwrite.RecipeVisibility.INTERNAL
import pl.allegro.tech.allwrite.recipes.java.ChangeType

internal class ChangeSpringBoot4WebServerTypes :
    AllwriteRecipe(
        displayName = "Change Spring Boot 4 web server types",
        description = "Move Spring Boot web server types to their Spring Boot 4 packages.",
        visibility = INTERNAL,
    ),
    ClasspathAwareRecipe {

    override fun requireOnClasspath(): List<String> = listOf("spring-boot-3")

    override fun getRecipeList(): List<Recipe> =
        listOf(
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "CompressionConnectorCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "ConfigurableTomcatWebServerFactory",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "ConnectorStartFailedException",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "ConnectorStartFailureAnalyzer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "DisableReferenceClearingContextCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "LazySessionIdGenerator",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "SslConnectorCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "TomcatConnectorCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "TomcatContextCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "TomcatEmbeddedContext",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "TomcatEmbeddedWebappClassLoader",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "TomcatProtocolHandlerCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.tomcat",
                "org.springframework.boot.tomcat",
                "TomcatWebServer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.jetty",
                "org.springframework.boot.jetty",
                "ConfigurableJettyWebServerFactory",
            ),
            relocate(
                "org.springframework.boot.web.embedded.jetty",
                "org.springframework.boot.jetty",
                "ForwardHeadersCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.jetty",
                "org.springframework.boot.jetty",
                "JettyServerCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.jetty",
                "org.springframework.boot.jetty",
                "JettyWebServer",
            ),
            relocate(
                "org.springframework.boot.web.embedded.jetty",
                "org.springframework.boot.jetty",
                "SslServerCustomizer",
            ),
            relocate(
                "org.springframework.boot.web.servlet.context",
                "org.springframework.boot.web.server.servlet.context",
                "AnnotationConfigServletWebServerApplicationContext",
            ),
            relocate(
                "org.springframework.boot.web.servlet.context",
                "org.springframework.boot.web.server.servlet.context",
                "ServletWebServerApplicationContext",
            ),
            relocate(
                "org.springframework.boot.web.servlet.context",
                "org.springframework.boot.web.server.servlet.context",
                "ServletWebServerApplicationContextFactory",
            ),
            relocate(
                "org.springframework.boot.web.servlet.context",
                "org.springframework.boot.web.server.servlet.context",
                "ServletWebServerInitializedEvent",
            ),
            relocate(
                "org.springframework.boot.web.servlet.context",
                "org.springframework.boot.web.server.servlet.context",
                "XmlServletWebServerApplicationContext",
            ),
            relocate(
                "org.springframework.boot.web.reactive.context",
                "org.springframework.boot.web.server.reactive.context",
                "AnnotationConfigReactiveWebServerApplicationContext",
            ),
            relocate(
                "org.springframework.boot.web.reactive.context",
                "org.springframework.boot.web.server.reactive.context",
                "ReactiveWebServerApplicationContext",
            ),
            relocate(
                "org.springframework.boot.web.reactive.context",
                "org.springframework.boot.web.server.reactive.context",
                "ReactiveWebServerApplicationContextFactory",
            ),
            relocate(
                "org.springframework.boot.web.reactive.context",
                "org.springframework.boot.web.server.reactive.context",
                "ReactiveWebServerInitializedEvent",
            ),
        )

    private fun relocate(oldPackage: String, newPackage: String, typeName: String): ChangeType =
        ChangeType("$oldPackage.$typeName", "$newPackage.$typeName", false)
}

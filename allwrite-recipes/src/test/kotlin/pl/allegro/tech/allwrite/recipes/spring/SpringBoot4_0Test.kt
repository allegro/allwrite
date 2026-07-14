package pl.allegro.tech.allwrite.recipes.spring

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.openrewrite.InMemoryExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.config.Environment
import org.openrewrite.groovy.GroovyParser
import org.openrewrite.java.JavaParser
import org.openrewrite.kotlin.KotlinParser
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import pl.allegro.tech.allwrite.api.RecipeSource
import pl.allegro.tech.allwrite.recipes.groovy
import pl.allegro.tech.allwrite.recipes.java
import pl.allegro.tech.allwrite.recipes.kotlin
import pl.allegro.tech.allwrite.recipes.toml
import pl.allegro.tech.allwrite.runtime.fake.FakeRecipeSource

class SpringBoot4_0Test : RewriteTest {

    private val upstreamEnvironment = Environment.builder()
        .scanRuntimeClasspath("org.openrewrite.java.spring")
        .build()

    private lateinit var recipe: Recipe

    @BeforeEach
    fun setUp() {
        startKoin {
            modules(
                module {
                    single<RecipeSource> {
                        FakeRecipeSource(
                            listOf(upstreamEnvironment.activateRecipes("org.openrewrite.java.spring.boot4.UpgradeSpringBoot_4_0")),
                        )
                    }
                },
            )
        }
        recipe = object : Recipe() {
            override fun getDisplayName() = "SpringBoot4_0"
            override fun getDescription() = "Runs all recipes from SpringBoot4_0."
            override fun getRecipeList(): List<Recipe> = SpringBoot4_0().recipeList
        }
    }

    @AfterEach
    fun tearDown() {
        stopKoin()
    }

    override fun defaults(spec: RecipeSpec) {
        val ctx = InMemoryExecutionContext()
        val classpath = arrayOf("spring-data-commons-3", "spring-data-jpa-3", "spring-data-mongodb-4", "spring-web-6", "spring-core-6")
        spec
            .recipe(recipe)
            .parser(JavaParser.fromJavaVersion().classpathFromResources(ctx, *classpath))
            .parser(KotlinParser.builder().classpathFromResources(ctx, *classpath))
            .parser(GroovyParser.builder().classpathFromResource(ctx, *classpath))
    }

    @Test
    fun `SpringBoot4_0 recipe list contains custom recipes`() {
        val recipeNames = recipe.recipeList.map { it::class.simpleName }
        assertThat(recipeNames).contains("AddNonNullableTypeBoundsToSpringRepositories", "ReplaceStatusCodeValue")
    }

    @Test
    fun `should add Any bounds to Spring Data repository type parameters`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.data.repository.CrudRepository

                interface UserRepository<T, ID> : CrudRepository<T, ID>
                """.trimIndent(),
                after = """
                import org.springframework.data.repository.CrudRepository

                interface UserRepository<T : Any, ID : Any> : CrudRepository<T, ID>
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace getStatusCodeValue() with getStatusCode() value() in Java`() {
        rewriteRun(
            java(
                before = """
                import org.springframework.http.ResponseEntity;

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello");
                        int status = response.getStatusCodeValue();
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity;

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello");
                        int status = response.getStatusCode().value();
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace statusCodeValue property access with statusCode value() in Kotlin`() {
        rewriteRun(
            kotlin(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test() {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        val status: Int = response.statusCodeValue
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    fun test() {
                        val response: ResponseEntity<String> = ResponseEntity.ok("hello")
                        val status: Int = response.statusCode.value()
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should replace statusCodeValue property access with statusCode value() in Groovy`() {
        rewriteRun(
            groovy(
                before = """
                import org.springframework.http.ResponseEntity

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        int status = response.statusCodeValue
                    }
                }
                """.trimIndent(),
                after = """
                import org.springframework.http.ResponseEntity

                class Example {
                    void test() {
                        ResponseEntity<String> response = ResponseEntity.ok("hello")
                        int status = response.statusCode.value()
                    }
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun `should update gradle spock dependency`() {
        rewriteRun(
            toml(
                before = """
                    [versions]
                    spock = "2.3-groovy-4.0"

                    [libraries]
                    spock-junit = { module = "org.spockframework:spock-junit4", version.ref = "spock" }
                    spock-core = { module = "org.spockframework:spock-core", version.ref = "spock" }
                    """.trimIndent(),
                after = """
                    [versions]
                    spock = "2.4-groovy-5.0"

                    [libraries]
                    spock-junit = { module = "org.spockframework:spock-junit4", version.ref = "spock" }
                    spock-core = { module = "org.spockframework:spock-core", version.ref = "spock" }
                    """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }

    @Test
    fun `testing example from rebate-store`() {
        rewriteRun(
            toml(
                before = """
[versions]

kotlin-coroutines-version = '1.8.1'
kotlin-logging-version = '3.0.5'
kotlin = '2.3.0'

integration-test = '2.2.1'

spock = "2.3-groovy-4.0"
groovy = "4.0.12"

json2avro-converter = "0.2.15"

andamio = "10.3.1"

discounts-utils = "1.0.4"

sentry-logback = "7.9.0"
guava = "33.2.0-jre"
resilience4j = "2.2.0"
snakeyaml = "2.2"
joda-time = "2.12.7"

rebate-calculator = "0.3.2"
rebate-label-generator = "2.0.3"


wiremock = "3.3.1"
mongo-explainer-sync = "0.1.5"
icu4j = "75.1"
micrometer-core = "1.5.4"
micrometer-context-propagation  = "1.1.1"
testcontainers-bom = "1.19.8"
public-api-version-media-type = "0.1.14"

# JUnit 5 = JUnit Platform ([junit-api]) + JUnit Launcher ([junit-launcher])
# Changelog: https://junit.org/junit5/docs/current/release-notes/index.html
junit5 = "5.10.2"
junit5-platform = "1.10.1"

mockito-kotlin = "5.3.1"
awaility = "4.3.0"
# Only bundle-store
kotest = "5.9.0"
kotest-spring = "1.1.3"

mongo-common-config = "10.0.0"

hermes-mock = "2.6.6"

mockk = "1.13.11"
springmockk = "4.0.2"

kotlin-logging = "6.0.9"
logback = "1.5.6"

shedlock = "5.9.1"  # max 5.9.1 compatible with Spring 6.0.x

jobrunr = "7.3.2"

konsists = "0.15.1"
archunit = "1.4.0"

mongo-driver = "4.11.4"

javamoney-moneta = "1.4.4"
i18nFeatures = "0.6.3"

permission_verifier_starter = "1.0.8"

user-store-client = "0.1.347"

[libraries]

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlin-coroutines-version" }
kotlinx-coroutines-rx2 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2", version.ref = "kotlin-coroutines-version" }
kotlinx-coroutines-reactor = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor", version.ref = "kotlin-coroutines-version" }
kotlin-logging-jvm = { module = "io.github.microutils:kotlin-logging-jvm", version.ref = "kotlin-logging-version" }
kotlinx-coroutines-reactive = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactive", version.ref = "kotlin-coroutines-version" }
spring-boot-mongodb-reactive = { module = "org.springframework.boot:spring-boot-starter-data-mongodb-reactive" }
spock-junit = { module = "org.spockframework:spock-junit4", version.ref = "spock" }
spock-core = { module = "org.spockframework:spock-core", version.ref = "spock" }
groovy = { module = "org.apache.groovy:groovy-all", version.ref = "groovy" }

andamio-starter-webmvc-tomcat = { module = "pl.allegro.tech.common:andamio-starter-webmvc-tomcat" }
andamio-starter-webflux = { module = "pl.allegro.tech.common:andamio-starter-webflux" }
andamio-starter-core = { module = "pl.allegro.tech.common:andamio-starter-core" }
andamio-starter = { module = "pl.allegro.tech.common:andamio-starter-dependencies", version.ref = "andamio" }
public-api-version-media-type = { module = "pl.allegro.tech.api:public-api-version-media-type", version.ref = "public-api-version-media-type" }
i18n-feature-api = { group = "pl.allegro.internationalization.features", name = "i18n-features-client-starter", version.ref = "i18nFeatures" }
i18n-feature-api-test = { group = "pl.allegro.internationalization.features", name = "i18n-features-client-testing-tools", version.ref = "i18nFeatures" }

json2avro-converter = { module = "tech.allegro.schema.json2avro:converter", version.ref = "json2avro-converter" }
guava-testlib = { module = "com.google.guava:guava-testlib", version.ref = "guava" }
byte-buddy = { module = "net.bytebuddy:byte-buddy" }
andamio-hermes = { module = "pl.allegro.tech.hermes:andamio-starter-hermes", version.ref = "andamio" }

spring-boot-mongodb = { module = "org.springframework.boot:spring-boot-starter-data-mongodb" }
spring-boot-aop = { module = "org.springframework.boot:spring-boot-starter-aop" }
spring-retry = { module = "org.springframework.retry:spring-retry" }
spock-spring = { module = "org.spockframework:spock-spring", version.ref = "spock" }
spring-test = { module = "org.springframework:spring-test" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test" }

resilience4j-circuitbreaker = { module = "io.github.resilience4j:resilience4j-circuitbreaker", version.ref = "resilience4j" }
resilience4j-micrometer = { module = "io.github.resilience4j:resilience4j-micrometer", version.ref = "resilience4j" }

rebate-calculator = { module = "pl.allegro.client.rebates:rebate-calculator", version.ref = "rebate-calculator" }
rebate-label-generator = { module = "pl.allegro.client.rebates:rebate-label-generator", version.ref = "rebate-label-generator" }

discounts-utils = { group = "pl.allegro.client.discounts", name = "discounts-utils", version.ref = "discounts-utils" }

okhttp = { module = "com.squareup.okhttp3:okhttp" }
netty-all = { module = "io.netty:netty-all" }
sentry-logback = { module = "io.sentry:sentry-logback", version.ref = "sentry-logback" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
caffeine = { module = "com.github.ben-manes.caffeine:caffeine" }
snakeyaml = { module = "org.yaml:snakeyaml", version.ref = "snakeyaml" }
joda-time = { module = "joda-time:joda-time", version.ref = "joda-time" }
jsonassert = { module = "org.skyscreamer:jsonassert" }
wiremock = { module = "org.wiremock:wiremock-standalone", version.ref = "wiremock" }

testcontainers-mongodb = { module = "org.testcontainers:mongodb" }
mongo-explainer-sync = { module = "pl.allegro.offer.tech:mongo-explainer-sync", version.ref = "mongo-explainer-sync" }
commons-lang3 = { module = "org.apache.commons:commons-lang3" }
icu4j = { module = "com.ibm.icu:icu4j", version.ref = "icu4j" }
micrometer-core = { module = "io.micrometer:micrometer-core", version.ref = "micrometer-core" }
micrometer-observation = { module = 'io.micrometer:micrometer-observation', version.ref = "micrometer-core" }
micrometer-context-propagation = { module = 'io.micrometer:context-propagation', version.ref = "micrometer-context-propagation" }

jackson-datatype-joda = { module = "com.fasterxml.jackson.datatype:jackson-datatype-joda" }
jackson-module-afterburner = { module = "com.fasterxml.jackson.module:jackson-module-afterburner" }
jackson-module-kotlin = { module = "com.fasterxml.jackson.module:jackson-module-kotlin" }
jackson-datatype-jsr310 = { module = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" }

testcontainers-bom = { module = "org.testcontainers:testcontainers-bom", version.ref = "testcontainers-bom" }

mongo-driver = {module = "org.mongodb:mongodb-driver-sync", version.ref="mongo-driver"}

# Junit for testing
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit5" }
junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }

# JUnit Engines: https://junit.org/junit5/docs/current/user-guide/index.html#running-tests-build-gradle-engines-configure
junit-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }

# https://docs.gradle.org/8.4/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
junit-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit5-platform" }

mockito-kotlin = { module = "org.mockito.kotlin:mockito-kotlin", version.ref = "mockito-kotlin" }
awaitility = { module = "org.awaitility:awaitility", version.ref = "awaility"}


# Only bundle-store
kotest-runner = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest"}
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest"}
kotest-datatest = {group = "io.kotest", name = "kotest-framework-datatest", version.ref = "kotest"}
kotest-spring = { module = "io.kotest.extensions:kotest-extensions-spring", version.ref = "kotest-spring"}

mongo-common-config = { module = "pl.allegro.database.mongodb:mongo-common-config", version.ref = "mongo-common-config" }

hermes-mock = { module = "pl.allegro.tech.hermes:hermes-mock", version.ref = "hermes-mock" }


resilience4j-retry = { module = "io.github.resilience4j:resilience4j-retry", version.ref = "resilience4j" }
resilience4j-spring-boot3 = { module = "io.github.resilience4j:resilience4j-spring-boot3", version.ref = "resilience4j" }

account_permission_verifier_starter = { group = "pl.allegro.settings.selleraccounts", name = "account-permissions-verifier-web-starter", version.ref = "permission_verifier_starter" }
account_permission_verifier_test = { group = "pl.allegro.settings.selleraccounts", name = "account-permissions-verifier-test-starter", version.ref = "permission_verifier_starter" }

mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
springmockk = { module = "com.ninja-squad:springmockk", version.ref = "springmockk" }

kotlin-logging = { module = "io.github.oshai:kotlin-logging-jvm", version.ref = "kotlin-logging" }
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }

shedlock-mongo = { module = "net.javacrumbs.shedlock:shedlock-provider-mongo", version.ref = "shedlock" }
shedlock-spring = { module = "net.javacrumbs.shedlock:shedlock-spring", version.ref = "shedlock" }

jobrunr-spring = { module = "org.jobrunr:jobrunr-spring-boot-3-starter", version.ref = "jobrunr" }
jobrunr-kotlin = { module = "org.jobrunr:jobrunr-kotlin-2.0-support", version.ref = "jobrunr" }

konsists = { module = "com.lemonappdev:konsist", version.ref = "konsists" }
archunit = {module = "com.tngtech.archunit:archunit", version.ref = "archunit"}
javamoney-moneta = { module = "org.javamoney:moneta", version.ref = "javamoney-moneta"}

user-store-client = { group = "pl.allegro.user.usersdb", name = "user-store-client", version.ref = "user-store-client" }

[bundles]

spock = ['spock-junit', 'spock-core']
resilience4j = ['resilience4j-circuitbreaker', 'resilience4j-micrometer']
kotest = ['kotest-runner', 'kotest-assertions', 'kotest-datatest']
shedlock = ['shedlock-mongo', 'shedlock-spring']

junit-tests = ["junit-api", "junit-params"]
junit-runtime = ["junit-engine", "junit-launcher"]

[plugins]

kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-allopen = { id = "org.jetbrains.kotlin.plugin.allopen", version.ref = "kotlin" }
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
integration-test = { id = "com.coditory.integration-test", version.ref = "integration-test"}
                                        """.trimIndent(),
                after = """
[versions]

kotlin-coroutines-version = '1.8.1'
kotlin-logging-version = '3.0.5'
kotlin = '2.3.0'

integration-test = '2.2.1'

spock = "2.4-groovy-5.0"
groovy = "4.0.12"

json2avro-converter = "0.2.15"

andamio = "10.3.1"

discounts-utils = "1.0.4"

sentry-logback = "7.9.0"
guava = "33.2.0-jre"
resilience4j = "2.2.0"
snakeyaml = "2.2"
joda-time = "2.12.7"

rebate-calculator = "0.3.2"
rebate-label-generator = "2.0.3"


wiremock = "3.3.1"
mongo-explainer-sync = "0.1.5"
icu4j = "75.1"
micrometer-core = "1.5.4"
micrometer-context-propagation  = "1.1.1"
testcontainers-bom = "1.19.8"
public-api-version-media-type = "0.1.14"

# JUnit 5 = JUnit Platform ([junit-api]) + JUnit Launcher ([junit-launcher])
# Changelog: https://junit.org/junit5/docs/current/release-notes/index.html
junit5 = "5.10.2"
junit5-platform = "1.10.1"

mockito-kotlin = "5.3.1"
awaility = "4.3.0"
# Only bundle-store
kotest = "5.9.0"
kotest-spring = "1.1.3"

mongo-common-config = "10.0.0"

hermes-mock = "2.6.6"

mockk = "1.13.11"
springmockk = "4.0.2"

kotlin-logging = "6.0.9"
logback = "1.5.6"

shedlock = "5.9.1"  # max 5.9.1 compatible with Spring 6.0.x

jobrunr = "7.3.2"

konsists = "0.15.1"
archunit = "1.4.0"

mongo-driver = "4.11.4"

javamoney-moneta = "1.4.4"
i18nFeatures = "0.6.3"

permission_verifier_starter = "1.0.8"

user-store-client = "0.1.347"

[libraries]

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlin-coroutines-version" }
kotlinx-coroutines-rx2 = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2", version.ref = "kotlin-coroutines-version" }
kotlinx-coroutines-reactor = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor", version.ref = "kotlin-coroutines-version" }
kotlin-logging-jvm = { module = "io.github.microutils:kotlin-logging-jvm", version.ref = "kotlin-logging-version" }
kotlinx-coroutines-reactive = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-reactive", version.ref = "kotlin-coroutines-version" }
spring-boot-mongodb-reactive = { module = "org.springframework.boot:spring-boot-starter-data-mongodb-reactive" }
spock-junit = { module = "org.spockframework:spock-junit4", version.ref = "spock" }
spock-core = { module = "org.spockframework:spock-core", version.ref = "spock" }
groovy = { module = "org.apache.groovy:groovy-all", version.ref = "groovy" }

andamio-starter-webmvc-tomcat = { module = "pl.allegro.tech.common:andamio-starter-webmvc-tomcat" }
andamio-starter-webflux = { module = "pl.allegro.tech.common:andamio-starter-webflux" }
andamio-starter-core = { module = "pl.allegro.tech.common:andamio-starter-core" }
andamio-starter = { module = "pl.allegro.tech.common:andamio-starter-dependencies", version.ref = "andamio" }
public-api-version-media-type = { module = "pl.allegro.tech.api:public-api-version-media-type", version.ref = "public-api-version-media-type" }
i18n-feature-api = { group = "pl.allegro.internationalization.features", name = "i18n-features-client-starter", version.ref = "i18nFeatures" }
i18n-feature-api-test = { group = "pl.allegro.internationalization.features", name = "i18n-features-client-testing-tools", version.ref = "i18nFeatures" }

json2avro-converter = { module = "tech.allegro.schema.json2avro:converter", version.ref = "json2avro-converter" }
guava-testlib = { module = "com.google.guava:guava-testlib", version.ref = "guava" }
byte-buddy = { module = "net.bytebuddy:byte-buddy" }
andamio-hermes = { module = "pl.allegro.tech.hermes:andamio-starter-hermes", version.ref = "andamio" }

spring-boot-mongodb = { module = "org.springframework.boot:spring-boot-starter-data-mongodb" }
spring-boot-aop = { module = "org.springframework.boot:spring-boot-starter-aop" }
spring-retry = { module = "org.springframework.retry:spring-retry" }
spock-spring = { module = "org.spockframework:spock-spring", version.ref = "spock" }
spring-test = { module = "org.springframework:spring-test" }
spring-boot-starter-test = { module = "org.springframework.boot:spring-boot-starter-test" }

resilience4j-circuitbreaker = { module = "io.github.resilience4j:resilience4j-circuitbreaker", version.ref = "resilience4j" }
resilience4j-micrometer = { module = "io.github.resilience4j:resilience4j-micrometer", version.ref = "resilience4j" }

rebate-calculator = { module = "pl.allegro.client.rebates:rebate-calculator", version.ref = "rebate-calculator" }
rebate-label-generator = { module = "pl.allegro.client.rebates:rebate-label-generator", version.ref = "rebate-label-generator" }

discounts-utils = { group = "pl.allegro.client.discounts", name = "discounts-utils", version.ref = "discounts-utils" }

okhttp = { module = "com.squareup.okhttp3:okhttp" }
netty-all = { module = "io.netty:netty-all" }
sentry-logback = { module = "io.sentry:sentry-logback", version.ref = "sentry-logback" }
guava = { module = "com.google.guava:guava", version.ref = "guava" }
caffeine = { module = "com.github.ben-manes.caffeine:caffeine" }
snakeyaml = { module = "org.yaml:snakeyaml", version.ref = "snakeyaml" }
joda-time = { module = "joda-time:joda-time", version.ref = "joda-time" }
jsonassert = { module = "org.skyscreamer:jsonassert" }
wiremock = { module = "org.wiremock:wiremock-standalone", version.ref = "wiremock" }

testcontainers-mongodb = { module = "org.testcontainers:mongodb" }
mongo-explainer-sync = { module = "pl.allegro.offer.tech:mongo-explainer-sync", version.ref = "mongo-explainer-sync" }
commons-lang3 = { module = "org.apache.commons:commons-lang3" }
icu4j = { module = "com.ibm.icu:icu4j", version.ref = "icu4j" }
micrometer-core = { module = "io.micrometer:micrometer-core", version.ref = "micrometer-core" }
micrometer-observation = { module = 'io.micrometer:micrometer-observation', version.ref = "micrometer-core" }
micrometer-context-propagation = { module = 'io.micrometer:context-propagation', version.ref = "micrometer-context-propagation" }

jackson-datatype-joda = { module = "com.fasterxml.jackson.datatype:jackson-datatype-joda" }
jackson-module-afterburner = { module = "com.fasterxml.jackson.module:jackson-module-afterburner" }
jackson-module-kotlin = { module = "com.fasterxml.jackson.module:jackson-module-kotlin" }
jackson-datatype-jsr310 = { module = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" }

testcontainers-bom = { module = "org.testcontainers:testcontainers-bom", version.ref = "testcontainers-bom" }

mongo-driver = {module = "org.mongodb:mongodb-driver-sync", version.ref="mongo-driver"}

# Junit for testing
junit-bom = { module = "org.junit:junit-bom", version.ref = "junit5" }
junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }

# JUnit Engines: https://junit.org/junit5/docs/current/user-guide/index.html#running-tests-build-gradle-engines-configure
junit-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }

# https://docs.gradle.org/8.4/userguide/upgrading_version_8.html#test_framework_implementation_dependencies
junit-launcher = { module = "org.junit.platform:junit-platform-launcher", version.ref = "junit5-platform" }

mockito-kotlin = { module = "org.mockito.kotlin:mockito-kotlin", version.ref = "mockito-kotlin" }
awaitility = { module = "org.awaitility:awaitility", version.ref = "awaility"}


# Only bundle-store
kotest-runner = { module = "io.kotest:kotest-runner-junit5", version.ref = "kotest"}
kotest-assertions = { module = "io.kotest:kotest-assertions-core", version.ref = "kotest"}
kotest-datatest = {group = "io.kotest", name = "kotest-framework-datatest", version.ref = "kotest"}
kotest-spring = { module = "io.kotest.extensions:kotest-extensions-spring", version.ref = "kotest-spring"}

mongo-common-config = { module = "pl.allegro.database.mongodb:mongo-common-config", version.ref = "mongo-common-config" }

hermes-mock = { module = "pl.allegro.tech.hermes:hermes-mock", version.ref = "hermes-mock" }


resilience4j-retry = { module = "io.github.resilience4j:resilience4j-retry", version.ref = "resilience4j" }
resilience4j-spring-boot3 = { module = "io.github.resilience4j:resilience4j-spring-boot3", version.ref = "resilience4j" }

account_permission_verifier_starter = { group = "pl.allegro.settings.selleraccounts", name = "account-permissions-verifier-web-starter", version.ref = "permission_verifier_starter" }
account_permission_verifier_test = { group = "pl.allegro.settings.selleraccounts", name = "account-permissions-verifier-test-starter", version.ref = "permission_verifier_starter" }

mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
springmockk = { module = "com.ninja-squad:springmockk", version.ref = "springmockk" }

kotlin-logging = { module = "io.github.oshai:kotlin-logging-jvm", version.ref = "kotlin-logging" }
logback-classic = { module = "ch.qos.logback:logback-classic", version.ref = "logback" }

shedlock-mongo = { module = "net.javacrumbs.shedlock:shedlock-provider-mongo", version.ref = "shedlock" }
shedlock-spring = { module = "net.javacrumbs.shedlock:shedlock-spring", version.ref = "shedlock" }

jobrunr-spring = { module = "org.jobrunr:jobrunr-spring-boot-3-starter", version.ref = "jobrunr" }
jobrunr-kotlin = { module = "org.jobrunr:jobrunr-kotlin-2.0-support", version.ref = "jobrunr" }

konsists = { module = "com.lemonappdev:konsist", version.ref = "konsists" }
archunit = {module = "com.tngtech.archunit:archunit", version.ref = "archunit"}
javamoney-moneta = { module = "org.javamoney:moneta", version.ref = "javamoney-moneta"}

user-store-client = { group = "pl.allegro.user.usersdb", name = "user-store-client", version.ref = "user-store-client" }

[bundles]

spock = ['spock-junit', 'spock-core']
resilience4j = ['resilience4j-circuitbreaker', 'resilience4j-micrometer']
kotest = ['kotest-runner', 'kotest-assertions', 'kotest-datatest']
shedlock = ['shedlock-mongo', 'shedlock-spring']

junit-tests = ["junit-api", "junit-params"]
junit-runtime = ["junit-engine", "junit-launcher"]

[plugins]

kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-allopen = { id = "org.jetbrains.kotlin.plugin.allopen", version.ref = "kotlin" }
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
integration-test = { id = "com.coditory.integration-test", version.ref = "integration-test"}
                    """.trimIndent(),
            ) { path("gradle/libs.versions.toml") },
        )
    }


}

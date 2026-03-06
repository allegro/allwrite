package pl.allegro.tech.allwrite.recipes

import org.openrewrite.groovy.Assertions.groovy
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.tree.J.CompilationUnit
import org.openrewrite.properties.Assertions.properties
import org.openrewrite.properties.tree.Properties
import org.openrewrite.test.SourceSpec
import org.openrewrite.yaml.Assertions.yaml
import org.openrewrite.yaml.tree.Yaml
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.SourceSpecs.text
import org.openrewrite.text.PlainText
import org.openrewrite.toml.tree.Toml
import org.openrewrite.kotlin.tree.K.CompilationUnit as KotlinCompilationUnit
import org.openrewrite.groovy.tree.G.CompilationUnit as GroovyCompilationUnit

fun properties(beforeAndAfter: String, spec: SourceSpec<Properties.File>.() -> Unit = {}) =
    properties(beforeAndAfter, spec)

fun properties(before: String, after: String?, spec: SourceSpec<Properties.File>.() -> Unit = {}) =
    properties(before, after, spec)

fun yaml(beforeAndAfter: String, spec: SourceSpec<Yaml.Documents>.() -> Unit = {}) =
    yaml(beforeAndAfter, spec)

fun yaml(before: String, after: String?, spec: SourceSpec<Yaml.Documents>.() -> Unit = {}) =
    yaml(before, after, spec)

fun java(before: String, after: String?, spec: SourceSpec<CompilationUnit>.() -> Unit = {}) =
    java(before, after, spec)

fun java(beforeAndAfter: String, spec: SourceSpec<CompilationUnit>.() -> Unit = {}) =
    java(beforeAndAfter, spec)

fun kotlin(before: String, after: String, spec: SourceSpec<KotlinCompilationUnit>.() -> Unit = {}) =
    kotlin(before, after, spec)

fun kotlin(beforeAndAfter: String, spec: SourceSpec<KotlinCompilationUnit>.() -> Unit = {}) =
    kotlin(beforeAndAfter, spec)

fun groovy(before: String, after: String?, spec: SourceSpec<GroovyCompilationUnit>.() -> Unit = {}) =
    groovy(before, after, spec)

fun groovy(beforeAndAfter: String, spec: SourceSpec<GroovyCompilationUnit>.() -> Unit = {}) =
    groovy(beforeAndAfter, spec)

fun text(before: String, after: String?, spec: SourceSpec<PlainText>.() -> Unit = {}) =
    text(before, after, spec)

fun text(beforeAndAfter: String, spec: SourceSpec<PlainText>.() -> Unit = {}) =
    text(beforeAndAfter, spec)

fun buildGradle(before: String, after: String?, spec: SourceSpec<GroovyCompilationUnit>.() -> Unit = {}) =
    org.openrewrite.gradle.Assertions.buildGradle(before, after, spec)

fun buildGradle(beforeAndAfter: String, spec: SourceSpec<GroovyCompilationUnit>.() -> Unit = {}) =
    org.openrewrite.gradle.Assertions.buildGradle(beforeAndAfter, spec)

fun buildGradleKts(before: String, after: String?, spec: SourceSpec<KotlinCompilationUnit>.() -> Unit = {}) =
    org.openrewrite.gradle.Assertions.buildGradleKts(before, after, spec)

fun buildGradleKts(beforeAndAfter: String, spec: SourceSpec<KotlinCompilationUnit>.() -> Unit = {}) =
    org.openrewrite.gradle.Assertions.buildGradleKts(beforeAndAfter, spec)

fun toml(before: String, after: String?, spec: SourceSpec<Toml.Document>.() -> Unit = {}) =
    org.openrewrite.toml.Assertions.toml(before, after, spec)

fun toml(beforeAndAfter: String?, spec: SourceSpec<Toml.Document>.() -> Unit = {}) =
    org.openrewrite.toml.Assertions.toml(beforeAndAfter, spec)
